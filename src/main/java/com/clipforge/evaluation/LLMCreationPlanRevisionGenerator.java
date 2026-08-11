package com.clipforge.evaluation;

import com.clipforge.agent.AgentContext;
import com.clipforge.agent.CreationPlanResult;
import com.clipforge.agent.generator.CreationPlanJsonParser;
import com.clipforge.agent.guardrail.CreationPlanOutputValidator;
import com.clipforge.agent.prompt.CreationPlanPromptRenderer;
import com.clipforge.agent.prompt.RenderedPrompt;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.model.chat.ChatMessage;
import com.clipforge.model.chat.ChatModelClient;
import com.clipforge.model.chat.ChatModelProperties;
import com.clipforge.model.chat.ChatModelRequest;
import com.clipforge.model.chat.ChatModelResponse;
import com.clipforge.model.usage.ModelCallRecord;
import com.clipforge.model.usage.ModelUsageRecorder;
import com.clipforge.tool.CreativeKnowledge;
import com.clipforge.trace.TraceRecorder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LLMCreationPlanRevisionGenerator（大语言模型创作方案修正生成器）。
 *
 * <p>该组件使用 LLM（大语言模型）根据 Evaluation（评估）结果修正创作方案，并记录 Token（词元）成本。
 */
@Component
@RequiredArgsConstructor
public class LLMCreationPlanRevisionGenerator implements CreationPlanRevisionGenerator {

    /** traceRecorder（执行链路记录器）用于记录修正过程。 */
    private final TraceRecorder traceRecorder;

    /** promptRenderer（提示词渲染器）用于渲染修正 Prompt（提示词）。 */
    private final CreationPlanPromptRenderer promptRenderer;

    /** chatModelClient（聊天模型客户端）用于调用 LLM（大语言模型）。 */
    private final ChatModelClient chatModelClient;

    /** chatModelProperties（聊天模型配置属性）用于读取模型参数。 */
    private final ChatModelProperties chatModelProperties;

    /** creationPlanJsonParser（创作方案 JSON 解析器）用于解析修正后的创作方案。 */
    private final CreationPlanJsonParser creationPlanJsonParser;

    /** creationPlanOutputValidator（创作方案输出校验器）用于校验修正结果。 */
    private final CreationPlanOutputValidator creationPlanOutputValidator;

    /** modelUsageRecorder（模型用量记录器）用于保存模型调用成本。 */
    private final ModelUsageRecorder modelUsageRecorder;

    /**
     * revise（修正）用于调用模型生成修正后的创作方案。
     *
     * @param context 智能体上下文
     * @param originalResult 原始创作方案结果
     * @param evaluationResult 创作方案评估结果
     * @return 修正后的创作方案结果
     */
    @Override
    public CreationPlanResult revise(AgentContext context, CreationPlanResult originalResult, CreationPlanEvaluationResult evaluationResult) {
        // 记录修正开始事件。
        traceRecorder.record(context.getRunId(), "REVISION_STARTED", "创作方案修正开始", "原始评分：" + evaluationResult.getOverallScore());
        // 从上下文读取创作知识。
        CreativeKnowledge knowledge = (CreativeKnowledge) context.getArtifact("CREATIVE_KNOWLEDGE");
        // 渲染修正提示词。
        RenderedPrompt prompt = promptRenderer.renderRevision(context, originalResult, evaluationResult, knowledge);
        // 把渲染后的修正 Prompt（提示词）写入上下文，方便真实模型测试观察。
        context.putArtifact("RENDERED_REVISION_PROMPT", prompt);
        // 构造聊天模型请求。
        ChatModelRequest request = ChatModelRequest.builder()
                // 设置运行编号。
                .runId(context.getRunId())
                // 设置调用用途。
                .purpose("CREATION_PLAN_REVISION")
                // 修正任务需要稳定，温度设置为 0.1。
                .temperature(0.1)
                // 设置最大输出 Token（词元）。
                .maxTokens(chatModelProperties.getMaxTokens())
                // 设置 system（系统角色）和 user（用户角色）消息。
                .messages(List.of(
                        // 添加系统提示词。
                        ChatMessage.system(prompt.getSystemPrompt()),
                        // 添加用户提示词。
                        ChatMessage.user(prompt.getUserPrompt())
                ))
                // 构建请求。
                .build();
        // 记录修正模型调用开始。
        traceRecorder.record(context.getRunId(), "REVISION_MODEL_CALL_STARTED", "修正模型调用开始", "用途：" + request.getPurpose());
        // 调用模型并记录成本。
        ChatModelResponse response = callModelAndRecord(context, request);
        // 把修正模型响应写入上下文。
        context.putArtifact("REVISION_CHAT_MODEL_RESPONSE", response);
        // 解析修正后的创作方案。
        CreationPlanResult revisedResult = creationPlanJsonParser.parse(response.getContent());
        // 校验修正后的创作方案结构。
        creationPlanOutputValidator.validate(revisedResult);
        // 把修正结果写入上下文。
        context.putArtifact("REVISED_PLAN_RESULT", revisedResult);
        // 记录修正完成事件。
        traceRecorder.record(context.getRunId(), "REVISION_FINISHED", "创作方案修正完成", "修正后主题：" + revisedResult.getTheme());
        // 返回修正后的创作方案。
        return revisedResult;
    }

    /**
     * callModelAndRecord（调用模型并记录）用于统一处理修正模型调用和 Token（词元）成本记录。
     *
     * @param context 智能体上下文
     * @param request 聊天模型请求
     * @return 聊天模型响应
     */
    private ChatModelResponse callModelAndRecord(AgentContext context, ChatModelRequest request) {
        // 记录调用开始时间。
        long startedAt = System.currentTimeMillis();
        try {
            // 调用聊天模型客户端。
            ChatModelResponse response = chatModelClient.chat(request);
            // 计算调用耗时。
            long latencyMs = System.currentTimeMillis() - startedAt;
            // 记录成功模型调用。
            modelUsageRecorder.record(ModelCallRecord.builder()
                    // 设置运行编号。
                    .runId(context.getRunId())
                    // 设置项目编号。
                    .projectId((String) context.getArtifact("PROJECT_ID"))
                    // 设置会话编号。
                    .conversationId((String) context.getArtifact("CONVERSATION_ID"))
                    // 设置供应商。
                    .provider(response.getProvider())
                    // 设置模型名称。
                    .modelName(response.getModelName())
                    // 设置调用用途。
                    .purpose(request.getPurpose())
                    // 设置输入 Token（词元）。
                    .promptTokens(response.getPromptTokens())
                    // 设置输出 Token（词元）。
                    .completionTokens(response.getCompletionTokens())
                    // 设置总 Token（词元）。
                    .totalTokens(response.getTotalTokens())
                    // 设置调用耗时。
                    .latencyMs(latencyMs)
                    // 设置调用成功。
                    .success(true)
                    // 构建记录。
                    .build());
            // 记录模型调用完成事件。
            traceRecorder.record(context.getRunId(), "REVISION_MODEL_CALL_FINISHED", "修正模型调用完成", "provider：" + response.getProvider() + "，model：" + response.getModelName() + "，totalTokens：" + response.getTotalTokens() + "，latencyMs：" + latencyMs);
            // 返回模型响应。
            return response;
        } catch (RuntimeException exception) {
            // 计算失败耗时。
            long latencyMs = System.currentTimeMillis() - startedAt;
            // 解析错误码。
            String errorCode = exception instanceof BusinessException businessException ? businessException.getErrorCode().getCode() : ErrorCode.INTERNAL_ERROR.getCode();
            // 记录失败模型调用。
            modelUsageRecorder.record(ModelCallRecord.builder()
                    // 设置运行编号。
                    .runId(context.getRunId())
                    // 设置项目编号。
                    .projectId((String) context.getArtifact("PROJECT_ID"))
                    // 设置会话编号。
                    .conversationId((String) context.getArtifact("CONVERSATION_ID"))
                    // 设置配置中的供应商。
                    .provider(chatModelProperties.getProvider())
                    // 设置配置中的模型名称。
                    .modelName(chatModelProperties.getModelName())
                    // 设置调用用途。
                    .purpose(request.getPurpose())
                    // 设置调用耗时。
                    .latencyMs(latencyMs)
                    // 设置调用失败。
                    .success(false)
                    // 设置错误码。
                    .errorCode(errorCode)
                    // 设置错误消息。
                    .errorMessage(exception.getMessage())
                    // 构建记录。
                    .build());
            // 记录失败 Trace（执行链路追踪）。
            traceRecorder.record(context.getRunId(), "REVISION_MODEL_CALL_FAILED", "修正模型调用失败", "purpose：" + request.getPurpose() + "，errorCode：" + errorCode + "，latencyMs：" + latencyMs);
            // 继续抛出原始异常。
            throw exception;
        }
    }
}
