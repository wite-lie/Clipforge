package com.clipforge.evaluation;

import com.clipforge.agent.AgentContext;
import com.clipforge.agent.CreationPlanResult;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * LLMCreationPlanEvaluator（大语言模型创作方案评估器）。
 *
 * <p>该评估器使用 LLM-as-Judge（大模型裁判）对创作方案进行多维评分，并记录 Token（词元）消耗。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "clipforge.evaluation", name = "mode", havingValue = "llm")
public class LLMCreationPlanEvaluator implements CreationPlanEvaluator {

    /** traceRecorder（执行链路记录器）用于记录评估过程。 */
    private final TraceRecorder traceRecorder;

    /** promptRenderer（提示词渲染器）用于渲染评估 Prompt（提示词）。 */
    private final CreationPlanPromptRenderer promptRenderer;

    /** chatModelClient（聊天模型客户端）用于调用真实 LLM（大语言模型）。 */
    private final ChatModelClient chatModelClient;

    /** chatModelProperties（聊天模型配置属性）用于读取温度、最大 Token（词元）等模型参数。 */
    private final ChatModelProperties chatModelProperties;

    /** evaluationJsonParser（评估 JSON 解析器）用于解析模型返回的评估结果。 */
    private final CreationPlanEvaluationJsonParser evaluationJsonParser;

    /** modelUsageRecorder（模型用量记录器）用于保存模型调用成本。 */
    private final ModelUsageRecorder modelUsageRecorder;

    /** revisionThreshold（修正阈值）用于在模型未明确要求修正时兜底判断。 */
    @Value("${clipforge.evaluation.score-threshold:80}")
    private int revisionThreshold = 80;

    /**
     * evaluateStructured（结构化评估）用于调用真实模型生成多维评估结果。
     *
     * @param context 智能体上下文
     * @param result 创作方案结果
     * @return 创作方案评估结果
     */
    @Override
    public CreationPlanEvaluationResult evaluateStructured(AgentContext context, CreationPlanResult result) {
        // 记录评估开始事件。
        traceRecorder.record(context.getRunId(), "EVALUATION_STARTED", "LLM-as-Judge 评估器开始多维评分", "平台：" + context.getPlatform());
        // 从上下文中读取创作知识。
        CreativeKnowledge knowledge = (CreativeKnowledge) context.getArtifact("CREATIVE_KNOWLEDGE");
        // 构建评估请求。
        CreationPlanEvaluationRequest evaluationRequest = CreationPlanEvaluationRequest.builder()
                // 设置智能体上下文。
                .context(context)
                // 设置待评估方案。
                .planResult(result)
                // 设置创作知识。
                .creativeKnowledge(knowledge)
                // 构建请求。
                .build();
        // 把评估请求写入上下文，便于排查。
        context.putArtifact("CREATION_PLAN_EVALUATION_REQUEST", evaluationRequest);
        // 渲染评估提示词。
        RenderedPrompt prompt = promptRenderer.renderEvaluation(evaluationRequest);
        // 把渲染后的评估 Prompt（提示词）写入上下文，方便真实模型测试观察。
        context.putArtifact("RENDERED_EVALUATION_PROMPT", prompt);
        // 构造聊天模型请求。
        ChatModelRequest chatModelRequest = ChatModelRequest.builder()
                // 设置运行编号。
                .runId(context.getRunId())
                // 设置模型调用用途。
                .purpose("CREATION_PLAN_EVALUATION")
                // 评估任务要求稳定，使用较低温度。
                .temperature(0.0)
                // 设置最大输出 Token（词元）数量。
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
        // 记录模型调用开始事件。
        traceRecorder.record(context.getRunId(), "EVALUATION_MODEL_CALL_STARTED", "评估模型调用开始", "用途：" + chatModelRequest.getPurpose());
        // 调用模型并记录 Token（词元）成本。
        ChatModelResponse response = callModelAndRecord(context, chatModelRequest);
        // 把原始响应写入上下文，方便调试。
        context.putArtifact("EVALUATION_CHAT_MODEL_RESPONSE", response);
        // 解析模型返回的评估 JSON（对象表示格式）。
        CreationPlanEvaluationResult evaluationResult = evaluationJsonParser.parse(response.getContent());
        // 如果模型没有明确给 evaluatorType（评估器类型），则补齐为 llm。
        if (evaluationResult.getEvaluatorType() == null || evaluationResult.getEvaluatorType().isBlank()) {
            // 设置评估器类型。
            evaluationResult.setEvaluatorType("llm");
        }
        // 如果综合分低于阈值，则兜底标记需要修正。
        if (evaluationResult.getOverallScore() < revisionThreshold) {
            // 设置需要修正。
            evaluationResult.setRevisionRequired(true);
        }
        // 把结构化评估结果写入上下文。
        context.putArtifact("CREATION_PLAN_EVALUATION_RESULT", evaluationResult);
        // 记录评估完成事件。
        traceRecorder.record(context.getRunId(), "EVALUATION_FINISHED", "LLM-as-Judge 评估器完成多维评分", "综合评分：" + evaluationResult.getOverallScore() + "，是否需要修正：" + evaluationResult.isRevisionRequired());
        // 返回评估结果。
        return evaluationResult;
    }

    /**
     * evaluate（评估）用于回填综合评分和修正建议，保持创作方案接口兼容。
     *
     * @param context 智能体上下文
     * @param result 创作方案结果
     * @return 评估后的创作方案结果
     */
    @Override
    public CreationPlanResult evaluate(AgentContext context, CreationPlanResult result) {
        // 执行结构化评估。
        CreationPlanEvaluationResult evaluationResult = evaluateStructured(context, result);
        // 回填综合评分。
        result.setScore(evaluationResult.getOverallScore());
        // 回填修正建议。
        result.setImprovementSuggestions(evaluationResult.getRevisionAdvice());
        // 把评估后的方案写入上下文。
        context.putArtifact("EVALUATED_PLAN_RESULT", result);
        // 返回评估后的方案。
        return result;
    }

    /**
     * callModelAndRecord（调用模型并记录）用于统一处理评估模型调用和 Token（词元）成本记录。
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
            // 计算耗时。
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
                    // 设置成功状态。
                    .success(true)
                    // 构建记录。
                    .build());
            // 记录模型调用完成事件。
            traceRecorder.record(context.getRunId(), "EVALUATION_MODEL_CALL_FINISHED", "评估模型调用完成", "provider：" + response.getProvider() + "，model：" + response.getModelName() + "，promptTokens：" + response.getPromptTokens() + "，completionTokens：" + response.getCompletionTokens() + "，totalTokens：" + response.getTotalTokens() + "，latencyMs：" + latencyMs);
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
                    // 设置失败状态。
                    .success(false)
                    // 设置错误码。
                    .errorCode(errorCode)
                    // 设置错误消息。
                    .errorMessage(exception.getMessage())
                    // 构建记录。
                    .build());
            // 记录模型调用失败事件。
            traceRecorder.record(context.getRunId(), "EVALUATION_MODEL_CALL_FAILED", "评估模型调用失败", "purpose：" + request.getPurpose() + "，errorCode：" + errorCode + "，latencyMs：" + latencyMs);
            // 继续抛出原始异常。
            throw exception;
        }
    }
}
