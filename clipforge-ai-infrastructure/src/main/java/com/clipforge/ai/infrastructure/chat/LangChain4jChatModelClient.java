package com.clipforge.ai.infrastructure.chat;
import com.clipforge.ai.api.chat.*;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.ai.infrastructure.governance.ModelCallLimiter;
import com.clipforge.ai.infrastructure.governance.ModelCallType;
import com.clipforge.ai.infrastructure.governance.NoOpModelCallLimiter;
import com.clipforge.ai.infrastructure.output.JsonCompletenessChecker;
import com.clipforge.ai.infrastructure.output.ModelOutputTruncationDetector;
import com.clipforge.ai.infrastructure.output.ModelOutputTruncationResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.TokenUsage;
import java.util.List;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * LangChain4jChatModelClient（LangChain4j 聊天模型客户端）。
 *
 * <p>该实现通过 LangChain4j（LangChain 的 Java 封装）的 OpenAiChatModel（OpenAI 聊天模型）调用 OpenAI-Compatible API（OpenAI 兼容接口）模型。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.model", name = "mock-enabled", havingValue = "false")
public class LangChain4jChatModelClient implements ChatModelClient {

    /** properties（配置属性）保存模型供应商、地址、密钥、模型名称等配置。 */
    private final ChatModelProperties properties;

    /** chatModel（聊天模型）是真正执行 LangChain4j（LangChain 的 Java 封装）调用的模型对象。 */
    private final ChatModel chatModel;

    /** modelCallLimiter（模型调用限流器）用于控制真实 LLM（大语言模型）调用并发。 */
    private final ModelCallLimiter modelCallLimiter;

    /** truncationDetector（截断识别器）用于给模型响应补充本地输出诊断元信息。 */
    private final ModelOutputTruncationDetector truncationDetector;

    /**
     * 构造函数用于根据配置创建 LangChain4j（LangChain 的 Java 封装）聊天模型。
     *
     * @param properties 聊天模型配置属性
     */
    public LangChain4jChatModelClient(ChatModelProperties properties) {
        this(properties, new NoOpModelCallLimiter(), new ModelOutputTruncationDetector(new JsonCompletenessChecker()));
    }

    /**
     * 构造函数用于注入模型调用限流器和输出截断识别器。
     *
     * @param properties 聊天模型配置属性
     * @param modelCallLimiter 模型调用限流器
     * @param truncationDetector 截断识别器
     */
    @Autowired
    public LangChain4jChatModelClient(ChatModelProperties properties, ModelCallLimiter modelCallLimiter, ModelOutputTruncationDetector truncationDetector) {
        // 保存配置属性，后续组装响应时需要读取 provider（供应商）和 modelName（模型名称）。
        this.properties = properties;
        // 保存模型调用限流器，真实出网调用前会先获取许可。
        this.modelCallLimiter = modelCallLimiter;
        // 保存截断识别器，用于在供应商未提供 finishReason（结束原因）时做本地启发式判断。
        this.truncationDetector = truncationDetector;
        // 校验真实模型配置，避免启动后才发现 API Key（接口密钥）缺失。
        validateProperties(properties);
        // 创建 OpenAI-Compatible API（OpenAI 兼容接口）聊天模型。
        this.chatModel = OpenAiChatModel.builder()
                // 设置模型接口基础地址。
                .baseUrl(properties.getBaseUrl())
                // 设置接口密钥。
                .apiKey(properties.getApiKey())
                // 设置模型名称。
                .modelName(properties.getModelName())
                // 设置是否记录请求。
                .logRequests(properties.isLogRequests())
                // 设置是否记录响应。
                .logResponses(properties.isLogResponses())
                // 构建 LangChain4j（LangChain 的 Java 封装）聊天模型。
                .build();
    }

    /**
     * chat（对话）用于发起一次真实非流式 LLM（大语言模型）调用。
     *
     * @param request 聊天模型请求
     * @return 聊天模型响应
     */
    @Override
    public ChatModelResponse chat(ChatModelRequest request) {
        try {
            // 把项目内部消息对象转换为 LangChain4j（LangChain 的 Java 封装）消息对象。
            List<dev.langchain4j.data.message.ChatMessage> messages = toLangChainMessages(request.getMessages());
            // 构造 LangChain4j（LangChain 的 Java 封装）聊天请求。
            ChatRequest langChainRequest = ChatRequest.builder()
                    // 设置消息列表。
                    .messages(messages)
                    // 设置模型名称，请求级配置优先使用当前模型配置。
                    .modelName(properties.getModelName())
                    // 设置温度参数，优先使用请求参数，缺失时使用配置默认值。
                    .temperature(defaultDouble(request.getTemperature(), properties.getTemperature()))
                    // 设置最大输出 Token（词元）数量，优先使用请求参数，缺失时使用配置默认值。
                    .maxOutputTokens(defaultInteger(request.getMaxTokens(), properties.getMaxTokens()))
                    // 构建聊天请求。
                    .build();
            // 调用真实聊天模型。
            ChatResponse response = modelCallLimiter.execute(
                    ModelCallType.fromChatPurpose(request.getPurpose()),
                    request.getRunId(),
                    () -> chatModel.chat(langChainRequest));
            // 从响应中读取 Token（词元）用量。
            TokenUsage tokenUsage = response.tokenUsage();
            // 读取模型文本内容，后续响应元信息和截断识别都基于该文本。
            String content = response.aiMessage().text();
            // LangChain4j 当前 OpenAI-Compatible 路径不稳定暴露 finishReason（结束原因），第一版先填 unknown（未知）。
            String finishReason = "unknown";
            // 使用本地规则识别输出是否疑似截断。
            ModelOutputTruncationResult truncationResult = truncationDetector.detect(content, finishReason, request.getMaxTokens());
            // 使用 Builder（建造者模式）转换为项目内部响应对象。
            return ChatModelResponse.builder()
                    // 设置供应商。
                    .provider(properties.getProvider())
                    // 设置模型名称，优先使用响应中的模型名，缺失时使用配置模型名。
                    .modelName(defaultText(response.modelName(), properties.getModelName()))
                    // 设置模型生成内容。
                    .content(content)
                    // 设置输入 Token（词元）数量。
                    .promptTokens(tokenUsage == null ? null : tokenUsage.inputTokenCount())
                    // 设置输出 Token（词元）数量。
                    .completionTokens(tokenUsage == null ? null : tokenUsage.outputTokenCount())
                    // 设置总 Token（词元）数量。
                    .totalTokens(tokenUsage == null ? null : tokenUsage.totalTokenCount())
                    .finishReason(finishReason)
                    .responseLength(content == null ? 0 : content.length())
                    .truncatedSuspected(truncationResult.isTruncated())
                    // 构建聊天模型响应。
                    .build();
        } catch (BusinessException exception) {
            // 业务异常直接抛出，避免重复包装。
            throw exception;
        } catch (Exception exception) {
            // 其他异常包装为模型调用错误，交给统一异常处理器。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "LangChain4j 调用真实模型失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * toLangChainMessages（转换消息列表）用于把项目内部 ChatMessage（聊天消息）转换为 LangChain4j（LangChain 的 Java 封装）消息。
     *
     * @param messages 项目内部聊天消息列表
     * @return LangChain4j（LangChain 的 Java 封装）聊天消息列表
     */
    private List<dev.langchain4j.data.message.ChatMessage> toLangChainMessages(List<ChatMessage> messages) {
        // 如果消息列表为空，抛出配置错误，避免向模型发送空请求。
        if (messages == null || messages.isEmpty()) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "聊天模型请求 messages（消息列表）不能为空");
        }
        // 转换每一条消息。
        return messages.stream()
                // 调用单条消息转换方法。
                .map(this::toLangChainMessage)
                // 收集为列表。
                .toList();
    }

    /**
     * toLangChainMessage（转换单条消息）用于处理 system/user/assistant（系统/用户/助手）角色。
     *
     * @param message 项目内部聊天消息
     * @return LangChain4j（LangChain 的 Java 封装）聊天消息
     */
    private dev.langchain4j.data.message.ChatMessage toLangChainMessage(ChatMessage message) {
        // 读取角色并转为小写，便于兼容大小写输入。
        String role = defaultText(message.getRole(), "").toLowerCase();
        // 读取消息内容。
        String content = defaultText(message.getContent(), "");
        // 根据角色创建 LangChain4j（LangChain 的 Java 封装）消息对象。
        return switch (role) {
            // system（系统角色）消息用于模型行为约束。
            case "system" -> SystemMessage.from(content);
            // user（用户角色）消息用于承载用户输入和参考资料。
            case "user" -> UserMessage.from(content);
            // assistant（助手角色）消息用于后续多轮对话历史。
            case "assistant" -> AiMessage.from(content);
            // 未知角色按模型配置错误处理。
            default -> throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "不支持的聊天消息角色：" + message.getRole());
        };
    }

    /**
     * validateProperties（校验配置）用于在创建真实模型客户端前检查必要配置。
     *
     * @param properties 聊天模型配置属性
     */
    private void validateProperties(ChatModelProperties properties) {
        // 校验基础地址。
        if (isBlank(properties.getBaseUrl())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.model.base-url 不能为空");
        }
        // 校验接口密钥。
        if (isBlank(properties.getApiKey())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.model.api-key 不能为空");
        }
        // 校验模型名称。
        if (isBlank(properties.getModelName())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.model.model-name 不能为空");
        }
    }

    /**
     * defaultDouble（默认小数）用于处理请求级参数缺失。
     *
     * @param value 请求值
     * @param defaultValue 默认值
     * @return 最终值
     */
    private Double defaultDouble(Double value, Double defaultValue) {
        // 请求值不为空时优先使用请求值。
        return value == null ? defaultValue : value;
    }

    /**
     * defaultInteger（默认整数）用于处理请求级参数缺失。
     *
     * @param value 请求值
     * @param defaultValue 默认值
     * @return 最终值
     */
    private Integer defaultInteger(Integer value, Integer defaultValue) {
        // 请求值不为空时优先使用请求值。
        return value == null ? defaultValue : value;
    }

    /**
     * defaultText（默认文本）用于处理空字符串。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 最终文本
     */
    private String defaultText(String value, String defaultValue) {
        // 如果原始值为空，返回默认值。
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * isBlank（是否空白）用于判断字符串是否为空引用或空白。
     *
     * @param value 原始值
     * @return 是否空白
     */
    private boolean isBlank(String value) {
        // 判断字符串是否为空引用或空白字符串。
        return value == null || value.isBlank();
    }
}
