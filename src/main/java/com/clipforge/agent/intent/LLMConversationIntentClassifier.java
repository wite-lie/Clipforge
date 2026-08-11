package com.clipforge.agent.intent;

import com.clipforge.model.chat.ChatMessage;
import com.clipforge.model.chat.ChatModelClient;
import com.clipforge.model.chat.ChatModelRequest;
import com.clipforge.model.chat.ChatModelResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LLMConversationIntentClassifier（大语言模型会话意图分类器）。
 *
 * <p>该组件只负责把模糊用户输入交给 LLM（大语言模型）做结构化分类，
 * 不直接决定最终路由；最终是否采信由 HybridConversationIntentAgent（混合会话意图智能体）判断。
 */
@Component
@RequiredArgsConstructor
public class LLMConversationIntentClassifier {

    /** chatModelClient（聊天模型客户端）用于执行真实或模拟 LLM（大语言模型）调用。 */
    private final ChatModelClient chatModelClient;

    /** promptRenderer（提示词渲染器）用于生成文件化 Prompt（提示词）。 */
    private final ConversationIntentPromptRenderer promptRenderer;

    /** properties（配置）用于控制温度、最大输出和启用开关。 */
    private final ConversationIntentProperties properties;

    /** objectMapper（对象映射器）用于解析模型输出 JSON（对象表示）。 */
    private final ObjectMapper objectMapper;

    /**
     * classify（分类）用于调用模型并解析结构化意图结果。
     */
    public ConversationIntentDecision classify(ConversationIntentRequest request) {
        RenderedIntentPrompt prompt = promptRenderer.render(request);
        ChatModelResponse response = chatModelClient.chat(ChatModelRequest.builder()
                .runId(request == null ? null : request.getRunId())
                .messages(List.of(
                        ChatMessage.system(prompt.getSystemPrompt()),
                        ChatMessage.user(prompt.getUserPrompt())
                ))
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .purpose("CONVERSATION_INTENT_CLASSIFICATION")
                .build());
        String rawOutput = response == null ? null : response.getContent();
        try {
            IntentModelOutput output = objectMapper.readValue(extractJson(rawOutput), IntentModelOutput.class);
            return ConversationIntentDecision.builder()
                    .intentType(parseIntentType(output.getIntentType()))
                    .confidence(output.getConfidence() == null ? 0.0 : output.getConfidence())
                    .recognitionMethod(ConversationIntentRecognitionMethod.LLM)
                    .routeTarget(output.getRouteTarget())
                    .requiredContext(output.getRequiredContext())
                    .missingContext(output.getMissingContext())
                    .reason(output.getReason())
                    .rawModelOutput(rawOutput)
                    .build();
        } catch (RuntimeException exception) {
            return ConversationIntentDecision.builder()
                    .intentType(ConversationIntentType.UNKNOWN)
                    .confidence(0.0)
                    .recognitionMethod(ConversationIntentRecognitionMethod.FALLBACK)
                    .routeTarget("FALLBACK_HANDLER")
                    .requiredContext(List.of())
                    .missingContext(List.of())
                    .reason("LLM 意图分类输出解析失败：" + exception.getMessage())
                    .rawModelOutput(rawOutput)
                    .build();
        } catch (Exception exception) {
            return ConversationIntentDecision.builder()
                    .intentType(ConversationIntentType.UNKNOWN)
                    .confidence(0.0)
                    .recognitionMethod(ConversationIntentRecognitionMethod.FALLBACK)
                    .routeTarget("FALLBACK_HANDLER")
                    .requiredContext(List.of())
                    .missingContext(List.of())
                    .reason("LLM 意图分类调用失败：" + exception.getMessage())
                    .rawModelOutput(rawOutput)
                    .build();
        }
    }

    /**
     * extractJson（提取 JSON）用于兼容模型在 JSON 外包裹说明文字或代码块的情况。
     */
    private String extractJson(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return "{}";
        }
        String trimmed = rawOutput.strip();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    /**
     * parseIntentType（解析意图类型）用于把模型返回的字符串安全转换为枚举。
     */
    private ConversationIntentType parseIntentType(String value) {
        if (value == null || value.isBlank()) {
            return ConversationIntentType.UNKNOWN;
        }
        try {
            return ConversationIntentType.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return ConversationIntentType.UNKNOWN;
        }
    }

    /**
     * IntentModelOutput（意图模型输出）用于承接 LLM（大语言模型）的 JSON（对象表示）响应。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    private static class IntentModelOutput {

        /** intentType（意图类型）表示模型判断的意图枚举名。 */
        private String intentType;

        /** confidence（置信度）表示模型自评置信度。 */
        private Double confidence;

        /** routeTarget（路由目标）表示建议进入的处理器。 */
        private String routeTarget;

        /** requiredContext（必需上下文）表示执行该路由需要哪些字段。 */
        private List<String> requiredContext;

        /** missingContext（缺失上下文）表示当前仍缺少哪些字段。 */
        private List<String> missingContext;

        /** reason（原因）表示模型判断理由。 */
        private String reason;
    }
}
