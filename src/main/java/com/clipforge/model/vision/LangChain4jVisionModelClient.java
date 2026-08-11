package com.clipforge.model.vision;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.model.governance.ModelCallLimiter;
import com.clipforge.model.governance.ModelCallType;
import com.clipforge.model.governance.NoOpModelCallLimiter;
import com.clipforge.model.output.JsonCompletenessChecker;
import com.clipforge.model.output.ModelOutputTruncationDetector;
import com.clipforge.model.output.ModelOutputTruncationResult;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.TokenUsage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * LangChain4jVisionModelClient（LangChain4j 视觉模型客户端）。
 *
 * <p>该实现通过 LangChain4j（LangChain 的 Java 封装）的 OpenAiChatModel（OpenAI 聊天模型）调用 OpenAI-Compatible API（OpenAI 兼容接口）多模态模型。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.vision", name = "mock-enabled", havingValue = "false")
public class LangChain4jVisionModelClient implements VisionModelClient {

    /** properties（配置属性）保存视觉模型供应商、地址、密钥和模型名称等配置。 */
    private final VisionModelProperties properties;

    /** promptRenderer（提示词渲染器）用于从文件生成图片理解 Prompt（提示词）。 */
    private final ImageUnderstandingPromptRenderer promptRenderer;

    /** jsonParser（JSON 解析器）用于把真实模型输出解析为结构化图片理解结果。 */
    private final ImageUnderstandingJsonParser jsonParser;

    /** chatModel（聊天模型）是真正执行 LangChain4j（LangChain 的 Java 封装）调用的多模态模型对象。 */
    private final ChatModel chatModel;

    /** modelCallLimiter（模型调用限流器）用于控制真实 VLM（视觉语言模型）调用并发。 */
    private final ModelCallLimiter modelCallLimiter;

    /** truncationDetector（截断识别器）用于统一判断视觉模型 JSON（对象表示格式）输出是否疑似截断。 */
    private final ModelOutputTruncationDetector truncationDetector;

    /**
     * 构造函数用于根据配置创建 LangChain4j（LangChain 的 Java 封装）视觉模型客户端。
     *
     * @param properties 视觉模型配置属性
     * @param promptRenderer 图片理解提示词渲染器
     * @param jsonParser 图片理解 JSON 解析器
     */
    public LangChain4jVisionModelClient(VisionModelProperties properties,
                                        ImageUnderstandingPromptRenderer promptRenderer,
                                        ImageUnderstandingJsonParser jsonParser) {
        this(properties, promptRenderer, jsonParser, new NoOpModelCallLimiter(), new ModelOutputTruncationDetector(new JsonCompletenessChecker()));
    }

    /**
     * 构造函数用于注入模型调用限流器和输出截断识别器。
     *
     * @param properties 视觉模型配置
     * @param promptRenderer 图片理解提示词渲染器
     * @param jsonParser 图片理解 JSON（对象表示格式）解析器
     * @param modelCallLimiter 模型调用限流器
     * @param truncationDetector 截断识别器
     */
    @Autowired
    public LangChain4jVisionModelClient(VisionModelProperties properties,
                                        ImageUnderstandingPromptRenderer promptRenderer,
                                        ImageUnderstandingJsonParser jsonParser,
                                        ModelCallLimiter modelCallLimiter,
                                        ModelOutputTruncationDetector truncationDetector) {
        // 保存配置属性。
        this.properties = properties;
        // 保存提示词渲染器。
        this.promptRenderer = promptRenderer;
        // 保存 JSON 解析器。
        this.jsonParser = jsonParser;
        // 保存模型调用限流器，真实视觉模型调用前会先获取许可。
        this.modelCallLimiter = modelCallLimiter;
        // 保存截断识别器，视觉输出修复前后共用该组件做诊断。
        this.truncationDetector = truncationDetector;
        // 校验真实视觉模型配置。
        validateProperties(properties);
        // 创建 OpenAI-Compatible API（OpenAI 兼容接口）聊天模型，当前复用 Chat Model（聊天模型）承载多模态输入。
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
     * understand（理解图片）用于发起一次真实 VLM（视觉语言模型）图片理解调用。
     *
     * @param request 视觉模型请求
     * @return 视觉模型响应
     */
    @Override
    public VisionModelResponse understand(VisionModelRequest request) {
        try {
            // 校验请求和图片素材。
            validateRequest(request);
            // 渲染图片理解 Prompt（提示词）。
            RenderedImageUnderstandingPrompt prompt = promptRenderer.render(request);
            // 构造 LangChain4j（LangChain 的 Java 封装）多模态聊天请求。
            ChatRequest chatRequest = ChatRequest.builder()
                    // 设置 system（系统）和 user（用户）消息。
                    .messages(List.of(SystemMessage.from(prompt.getSystemPrompt()), buildUserMessage(prompt, request.getMaterials())))
                    // 设置模型名称。
                    .modelName(properties.getModelName())
                    // 设置温度参数，优先使用请求参数。
                    .temperature(defaultDouble(request.getTemperature(), properties.getTemperature()))
                    // 设置最大输出 Token（词元）数量，优先使用请求参数。
                    .maxOutputTokens(defaultInteger(request.getMaxTokens(), properties.getMaxTokens()))
                    // 构建请求。
                    .build();
            // 调用真实多模态模型。
            ChatResponse chatResponse = modelCallLimiter.execute(
                    ModelCallType.VISION_UNDERSTANDING,
                    request.getRunId(),
                    () -> chatModel.chat(chatRequest));
            // 读取模型原始输出。
            String rawContent = chatResponse.aiMessage().text();
            // 解析结构化图片理解结果，必要时执行一次输出修复。
            ParsedVisionOutput parsedOutput = parseWithOptionalRepair(rawContent, chatResponse.tokenUsage(), request.getMaxTokens());
            // 返回项目内部视觉模型响应。
            return VisionModelResponse.builder()
                    // 设置供应商。
                    .provider(properties.getProvider())
                    // 设置模型名称，优先使用响应中的模型名称。
                    .modelName(defaultText(chatResponse.modelName(), properties.getModelName()))
                    // 设置原始输出。
                    .rawContent(parsedOutput.rawContent())
                    // 设置结构化结果。
                    .results(parsedOutput.results())
                    // 设置输入 Token（词元）数量。
                    .promptTokens(parsedOutput.promptTokens())
                    // 设置输出 Token（词元）数量。
                    .completionTokens(parsedOutput.completionTokens())
                    // 设置总 Token（词元）数量。
                    .totalTokens(parsedOutput.totalTokens())
                    .finishReason(parsedOutput.finishReason())
                    .responseLength(parsedOutput.rawContent() == null ? 0 : parsedOutput.rawContent().length())
                    .truncatedSuspected(parsedOutput.truncatedSuspected())
                    .rawErrorMessage(parsedOutput.rawErrorMessage())
                    // 构建响应。
                    .build();
        } catch (BusinessException exception) {
            // 业务异常直接抛出，避免重复包装。
            throw exception;
        } catch (Exception exception) {
            // 其他异常包装为模型调用错误。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "LangChain4j 调用真实视觉模型失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * parseWithOptionalRepair（解析并按需修复）用于处理真实 VLM（视觉语言模型）偶尔输出半结构化 JSON（对象表示格式）的情况。
     *
     * @param rawContent 原始模型输出
     * @param originalTokenUsage 原始调用 Token（词元）用量
     * @return 已解析图片理解输出
     */
    private ParsedVisionOutput parseWithOptionalRepair(String rawContent, TokenUsage originalTokenUsage, Integer maxOutputTokens) {
        ModelOutputTruncationResult originalTruncationResult = truncationDetector.detect(rawContent, "unknown", maxOutputTokens);
        try {
            // 优先直接解析原始输出。
            List<ImageUnderstandingResult> results = jsonParser.parseResults(rawContent);
            // 返回原始解析结果。
            return new ParsedVisionOutput(rawContent, results, inputTokens(originalTokenUsage), outputTokens(originalTokenUsage), totalTokens(originalTokenUsage), "unknown", originalTruncationResult.isTruncated(), null);
        } catch (BusinessException exception) {
            // 只修复模型输出解析错误，其他业务错误继续抛出。
            if (exception.getErrorCode() != ErrorCode.MODEL_OUTPUT_PARSE_ERROR) {
                // 继续抛出原异常。
                throw exception;
            }
            // 渲染图片理解输出修复 Prompt（提示词）。
            RenderedImageUnderstandingPrompt repairPrompt = promptRenderer.renderRepair(exception.getMessage(), rawContent);
            // 构造文本修复请求，不再携带图片，避免重复视觉输入成本。
            ChatRequest repairRequest = ChatRequest.builder()
                    // 设置修复消息。
                    .messages(List.of(SystemMessage.from(repairPrompt.getSystemPrompt()), UserMessage.from(repairPrompt.getUserPrompt())))
                    // 设置模型名称。
                    .modelName(properties.getModelName())
                    // 修复任务要求稳定，温度固定为 0。
                    .temperature(0.0)
                    // 修复输出通常比原始理解短，仍使用配置最大输出兜底。
                    .maxOutputTokens(properties.getMaxTokens())
                    // 构建请求。
                    .build();
            // 调用模型执行一次 JSON（对象表示格式）修复。
            ChatResponse repairResponse = modelCallLimiter.execute(
                    ModelCallType.CHAT_REPAIR,
                    "vision-output-repair",
                    () -> chatModel.chat(repairRequest));
            // 读取修复后输出。
            String repairedContent = repairResponse.aiMessage().text();
            ModelOutputTruncationResult repairedTruncationResult = truncationDetector.detect(repairedContent, "unknown", properties.getMaxTokens());
            // 解析修复后输出；如果仍失败，则让异常继续暴露，便于调整提示词。
            List<ImageUnderstandingResult> repairedResults = jsonParser.parseResults(repairedContent);
            // 读取修复调用 Token（词元）用量。
            TokenUsage repairTokenUsage = repairResponse.tokenUsage();
            // 合并原始输出和修复输出，便于测试排查真实模型行为。
            String mergedRawContent = "===== 原始图片理解输出 =====\n" + rawContent + "\n\n===== 修复后图片理解输出 =====\n" + repairedContent;
            // 返回修复后的解析结果和合并 Token（词元）用量。
            return new ParsedVisionOutput(
                    mergedRawContent,
                    repairedResults,
                    sumNullable(inputTokens(originalTokenUsage), inputTokens(repairTokenUsage)),
                    sumNullable(outputTokens(originalTokenUsage), outputTokens(repairTokenUsage)),
                    sumNullable(totalTokens(originalTokenUsage), totalTokens(repairTokenUsage)),
                    "unknown",
                    originalTruncationResult.isTruncated() || repairedTruncationResult.isTruncated(),
                    originalTruncationResult.getReason()
            );
        }
    }

    /**
     * inputTokens（输入词元）用于安全读取 TokenUsage（词元用量）中的输入数量。
     *
     * @param tokenUsage 词元用量
     * @return 输入词元数量
     */
    private Integer inputTokens(TokenUsage tokenUsage) {
        // TokenUsage（词元用量）为空时返回空。
        return tokenUsage == null ? null : tokenUsage.inputTokenCount();
    }

    /**
     * outputTokens（输出词元）用于安全读取 TokenUsage（词元用量）中的输出数量。
     *
     * @param tokenUsage 词元用量
     * @return 输出词元数量
     */
    private Integer outputTokens(TokenUsage tokenUsage) {
        // TokenUsage（词元用量）为空时返回空。
        return tokenUsage == null ? null : tokenUsage.outputTokenCount();
    }

    /**
     * totalTokens（总词元）用于安全读取 TokenUsage（词元用量）中的总数量。
     *
     * @param tokenUsage 词元用量
     * @return 总词元数量
     */
    private Integer totalTokens(TokenUsage tokenUsage) {
        // TokenUsage（词元用量）为空时返回空。
        return tokenUsage == null ? null : tokenUsage.totalTokenCount();
    }

    /**
     * sumNullable（可空求和）用于合并原始调用和修复调用的 Token（词元）消耗。
     *
     * @param first 第一个数值
     * @param second 第二个数值
     * @return 合并结果
     */
    private Integer sumNullable(Integer first, Integer second) {
        // 如果两个值都为空，则返回空，表示供应商没有提供 Token（词元）用量。
        if (first == null && second == null) {
            // 返回空。
            return null;
        }
        // 空值按 0 参与求和。
        return (first == null ? 0 : first) + (second == null ? 0 : second);
    }

    /**
     * buildUserMessage（构造用户消息）用于把用户提示词和图片内容合并为多模态消息。
     *
     * @param prompt 已渲染提示词
     * @param materials 图片素材列表
     * @return LangChain4j（LangChain 的 Java 封装）用户消息
     */
    private UserMessage buildUserMessage(RenderedImageUnderstandingPrompt prompt, List<VisionModelRequestMaterial> materials) {
        // 创建多模态内容列表。
        List<Content> contents = new ArrayList<>();
        // 添加文本提示词内容。
        contents.add(TextContent.from(prompt.getUserPrompt()));
        // 遍历图片素材。
        for (VisionModelRequestMaterial material : materials) {
            // 添加图片内容，当前使用本地文件路径和内容类型，LangChain4j 会转换为模型可接收的图片输入。
            contents.add(ImageContent.from(Path.of(material.getFilePath()), material.getContentType()));
        }
        // 返回多模态用户消息。
        return UserMessage.from(contents);
    }

    /**
     * validateRequest（校验请求）用于检查图片理解请求是否可被真实 VLM（视觉语言模型）处理。
     *
     * @param request 视觉模型请求
     */
    private void validateRequest(VisionModelRequest request) {
        // 请求不能为空。
        if (request == null) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "视觉模型请求不能为空");
        }
        // 图片素材不能为空。
        if (request.getMaterials() == null || request.getMaterials().isEmpty()) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "视觉模型请求 materials（图片素材列表）不能为空");
        }
        // 遍历校验每张图片。
        for (VisionModelRequestMaterial material : request.getMaterials()) {
            // 校验文件路径。
            if (isBlank(material.getFilePath())) {
                // 抛出模型配置错误。
                throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "图片素材 filePath（文件路径）不能为空，materialId：" + material.getMaterialId());
            }
            // 校验内容类型。
            if (isBlank(material.getContentType())) {
                // 抛出模型配置错误。
                throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "图片素材 contentType（内容类型）不能为空，materialId：" + material.getMaterialId());
            }
        }
    }

    /**
     * validateProperties（校验配置）用于在创建真实视觉模型客户端前检查必要配置。
     *
     * @param properties 视觉模型配置属性
     */
    private void validateProperties(VisionModelProperties properties) {
        // 校验基础地址。
        if (isBlank(properties.getBaseUrl())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.vision.base-url 不能为空");
        }
        // 校验接口密钥。
        if (isBlank(properties.getApiKey())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.vision.api-key 不能为空");
        }
        // 校验模型名称。
        if (isBlank(properties.getModelName())) {
            // 抛出模型配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.vision.model-name 不能为空");
        }
        // 校验模型是否疑似图像生成或图像编辑模型。
        if (isImageGenerationModel(properties.getModelName())) {
            // 抛出模型配置错误，避免把图像生成模型误用于图片理解。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "clipforge.vision.model-name 当前配置为 " + properties.getModelName() + "，该名称疑似图像生成/编辑模型，不适合图片理解。请改用支持图像输入理解的 VLM（视觉语言模型），例如 qwen-vl-plus、qwen-vl-max 或供应商控制台中标注为视觉理解/多模态理解的模型。");
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

    /**
     * isImageGenerationModel（是否图像生成模型）用于拦截常见图像生成/编辑模型误配置。
     *
     * @param modelName 模型名称
     * @return 疑似图像生成模型时返回 true（真）
     */
    private boolean isImageGenerationModel(String modelName) {
        // 如果模型名为空，则不是本方法处理的场景。
        if (isBlank(modelName)) {
            // 返回 false（假）。
            return false;
        }
        // 转为小写，便于匹配不同大小写写法。
        String lowerModelName = modelName.toLowerCase();
        // qwen-image 系列主要用于图像生成或图像编辑，不是图片理解 VLM。
        return lowerModelName.startsWith("qwen-image");
    }

    /**
     * ParsedVisionOutput（已解析视觉输出）用于承载原始或修复后的图片理解解析结果。
     *
     * @param rawContent 原始内容
     * @param results 图片理解结果
     * @param promptTokens 输入 Token（词元）
     * @param completionTokens 输出 Token（词元）
     * @param totalTokens 总 Token（词元）
     */
    private record ParsedVisionOutput(String rawContent, List<ImageUnderstandingResult> results, Integer promptTokens, Integer completionTokens, Integer totalTokens,
                                      String finishReason, Boolean truncatedSuspected, String rawErrorMessage) {
    }
}
