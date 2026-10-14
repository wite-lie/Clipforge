package com.clipforge.agent.generator;

import com.clipforge.agent.prompt.ArticleCreationPromptRenderer;
import com.clipforge.ai.api.chat.ChatMessage;
import com.clipforge.ai.api.chat.ChatModelClient;
import com.clipforge.ai.api.chat.ChatModelRequest;
import com.clipforge.ai.api.chat.ChatModelResponse;
import com.clipforge.ai.infrastructure.chat.ChatModelProperties;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.contract.creation.ArticleCreationPlan;
import com.clipforge.creation.article.ArticleCreationPlanGenerator;
import com.clipforge.creation.article.ArticleCreationPlanJsonParser;
import com.clipforge.creation.article.ArticleCreationPlanValidator;
import com.clipforge.creation.knowledge.CreativeKnowledge;
import com.clipforge.creation.model.AgentContext;
import com.clipforge.creation.model.AgentPlan;
import com.clipforge.creation.prompt.RenderedCreationPlanPrompt;
import com.clipforge.creation.prompt.RenderedPrompt;
import com.clipforge.observability.trace.TraceRecorder;
import com.clipforge.observability.usage.ModelCallRecord;
import com.clipforge.observability.usage.ModelUsageRecorder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * LLMArticleCreationPlanGenerator（大语言模型图文创作方案生成器）。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.model", name = "mock-enabled", havingValue = "false")
@RequiredArgsConstructor
public class LLMArticleCreationPlanGenerator implements ArticleCreationPlanGenerator {

    /** traceRecorder（执行链路记录器）用于记录图文生成事件。 */
    private final TraceRecorder traceRecorder;

    /** promptRenderer（图文提示词渲染器）用于渲染模型输入。 */
    private final ArticleCreationPromptRenderer promptRenderer;

    /** chatModelClient（聊天模型客户端）用于调用真实 LLM（大语言模型）。 */
    private final ChatModelClient chatModelClient;

    /** chatModelProperties（聊天模型配置）用于读取温度和最大输出 Token。 */
    private final ChatModelProperties chatModelProperties;

    /** jsonParser（JSON 解析器）用于解析模型输出。 */
    private final ArticleCreationPlanJsonParser jsonParser;

    /** validator（校验器）用于校验图文结构完整性。 */
    private final ArticleCreationPlanValidator validator;

    /** modelUsageRecorder（模型用量记录器）用于记录 Token 和耗时。 */
    private final ModelUsageRecorder modelUsageRecorder;

    /** outputRepairEnabled（输出修复开关）用于在图文 JSON 不合法时进行一次修复调用。 */
    @Value("${clipforge.model.output-repair-enabled:true}")
    private boolean outputRepairEnabled = true;

    /** printCreationPromptAndResponse（打印开关）与短视频生成共用，便于真实测试观察。 */
    @Value("${clipforge.model.print-creation-prompt-and-response:false}")
    private boolean printCreationPromptAndResponse = false;

    @Override
    public ArticleCreationPlan generate(AgentContext context, AgentPlan plan) {
        traceRecorder.record(context.getRunId(), "ARTICLE_GENERATOR_STARTED", "图文方案生成器开始生成", "platform：" + context.getPlatform());
        CreativeKnowledge knowledge = (CreativeKnowledge) context.getArtifact("CREATIVE_KNOWLEDGE");
        RenderedCreationPlanPrompt prompt = promptRenderer.render(context, plan, knowledge);
        context.putArtifact("RENDERED_ARTICLE_CREATION_PROMPT", prompt);
        printPromptIfEnabled(prompt);
        ChatModelRequest request = ChatModelRequest.builder()
                .runId(context.getRunId())
                .purpose("ARTICLE_PLAN_GENERATION")
                .temperature(chatModelProperties.getTemperature())
                .maxTokens(chatModelProperties.getMaxTokens())
                .messages(List.of(ChatMessage.system(prompt.getSystemPrompt()), ChatMessage.user(prompt.getUserPrompt())))
                .build();
        ChatModelResponse response = callModelAndRecord(context, request, "ARTICLE_MODEL_CALL_FINISHED", "ARTICLE_MODEL_CALL_FAILED");
        context.putArtifact("ARTICLE_CHAT_MODEL_RESPONSE", response);
        printResponseIfEnabled(response);
        ArticleCreationPlan articlePlan = parseValidateAndRepairIfNeeded(context, request, response);
        context.putArtifact("ARTICLE_CREATION_PLAN", articlePlan);
        traceRecorder.record(context.getRunId(), "ARTICLE_GENERATOR_FINISHED", "图文方案生成完成", "标题数：" + safeSize(articlePlan.getTitleCandidates()) + "，段落数：" + safeSize(articlePlan.getSections()));
        return articlePlan;
    }

    private ArticleCreationPlan parseValidateAndRepairIfNeeded(AgentContext context, ChatModelRequest originalRequest, ChatModelResponse response) {
        try {
            ArticleCreationPlan plan = jsonParser.parse(response.getContent());
            validator.validate(plan);
            return plan;
        } catch (BusinessException exception) {
            if (!outputRepairEnabled || !shouldRepair(exception)) {
                throw exception;
            }
            traceRecorder.record(context.getRunId(), "ARTICLE_OUTPUT_REPAIR_STARTED", "图文模型输出不符合结构，开始修复", exception.getMessage());
            RenderedPrompt repairPrompt = promptRenderer.renderRepair(exception.getMessage(), response == null ? null : response.getContent());
            ChatModelRequest repairRequest = ChatModelRequest.builder()
                    .runId(context.getRunId())
                    .purpose("ARTICLE_PLAN_OUTPUT_REPAIR")
                    .temperature(0.0)
                    .maxTokens(originalRequest.getMaxTokens())
                    .messages(List.of(ChatMessage.system(repairPrompt.getSystemPrompt()), ChatMessage.user(repairPrompt.getUserPrompt())))
                    .build();
            ChatModelResponse repairResponse = callModelAndRecord(context, repairRequest, "ARTICLE_OUTPUT_REPAIR_FINISHED", "ARTICLE_OUTPUT_REPAIR_FAILED");
            ArticleCreationPlan repaired = jsonParser.parse(repairResponse.getContent());
            validator.validate(repaired);
            traceRecorder.record(context.getRunId(), "ARTICLE_OUTPUT_REPAIR_SUCCEEDED", "图文模型输出修复成功", "段落数：" + safeSize(repaired.getSections()));
            return repaired;
        }
    }

    private boolean shouldRepair(BusinessException exception) {
        return exception.getErrorCode() == ErrorCode.MODEL_OUTPUT_PARSE_ERROR || exception.getErrorCode() == ErrorCode.AGENT_OUTPUT_INVALID;
    }

    private ChatModelResponse callModelAndRecord(AgentContext context, ChatModelRequest request, String successEventType, String failureEventType) {
        long startedAt = System.currentTimeMillis();
        try {
            ChatModelResponse response = chatModelClient.chat(request);
            long latencyMs = System.currentTimeMillis() - startedAt;
            modelUsageRecorder.record(ModelCallRecord.builder()
                    .runId(context.getRunId())
                    .projectId(artifactText(context, "PROJECT_ID"))
                    .conversationId(artifactText(context, "CONVERSATION_ID"))
                    .provider(response.getProvider())
                    .modelName(response.getModelName())
                    .purpose(request.getPurpose())
                    .promptTokens(response.getPromptTokens())
                    .completionTokens(response.getCompletionTokens())
                    .totalTokens(response.getTotalTokens())
                    .latencyMs(latencyMs)
                    .success(true)
                    .build());
            traceRecorder.record(context.getRunId(), successEventType, "图文模型调用成功", "purpose：" + request.getPurpose() + "，totalTokens：" + response.getTotalTokens() + "，latencyMs：" + latencyMs);
            return response;
        } catch (RuntimeException exception) {
            long latencyMs = System.currentTimeMillis() - startedAt;
            modelUsageRecorder.record(ModelCallRecord.builder()
                    .runId(context.getRunId())
                    .projectId(artifactText(context, "PROJECT_ID"))
                    .conversationId(artifactText(context, "CONVERSATION_ID"))
                    .provider(chatModelProperties.getProvider())
                    .modelName(chatModelProperties.getModelName())
                    .purpose(request.getPurpose())
                    .latencyMs(latencyMs)
                    .success(false)
                    .errorCode(exception.getClass().getSimpleName())
                    .errorMessage(exception.getMessage())
                    .build());
            traceRecorder.record(context.getRunId(), failureEventType, "图文模型调用失败", "purpose：" + request.getPurpose() + "，error：" + exception.getMessage());
            throw exception;
        }
    }

    private void printPromptIfEnabled(RenderedCreationPlanPrompt prompt) {
        if (!printCreationPromptAndResponse) {
            return;
        }
        System.out.println("===== 真实 LLM（大语言模型）生成图文方案前 Prompt（提示词）====");
        System.out.println("systemPrompt:");
        System.out.println(prompt.getSystemPrompt());
        System.out.println("userPrompt:");
        System.out.println(prompt.getUserPrompt());
    }

    private void printResponseIfEnabled(ChatModelResponse response) {
        if (!printCreationPromptAndResponse) {
            return;
        }
        System.out.println("===== 真实 LLM（大语言模型）生成图文方案响应 =====");
        System.out.println(response == null ? "" : response.getContent());
    }

    private String artifactText(AgentContext context, String key) {
        Object value = context == null ? null : context.getArtifact(key);
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }
}
