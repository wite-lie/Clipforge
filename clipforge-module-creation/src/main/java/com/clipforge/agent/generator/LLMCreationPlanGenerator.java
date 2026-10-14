package com.clipforge.agent.generator;

import com.clipforge.creation.model.AgentContext;
import com.clipforge.creation.model.AgentPlan;
import com.clipforge.creation.generator.CreationPlanGenerator;
import com.clipforge.creation.generator.CreationPlanJsonParser;
import com.clipforge.contract.creation.CreationPlanResult;
import com.clipforge.creation.guardrail.CreationPlanOutputValidator;
import com.clipforge.agent.prompt.CreationPlanPromptRenderer;
import com.clipforge.creation.prompt.RenderedCreationPlanPrompt;
import com.clipforge.creation.prompt.RenderedPrompt;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.ai.api.chat.ChatMessage;
import com.clipforge.ai.api.chat.ChatModelClient;
import com.clipforge.ai.infrastructure.chat.ChatModelProperties;
import com.clipforge.ai.api.chat.ChatModelRequest;
import com.clipforge.ai.api.chat.ChatModelResponse;
import com.clipforge.ai.infrastructure.output.JsonCompletenessChecker;
import com.clipforge.ai.infrastructure.output.ModelOutputRepairResult;
import com.clipforge.ai.infrastructure.output.ModelOutputRepairStatus;
import com.clipforge.ai.infrastructure.output.ModelOutputTruncationDetector;
import com.clipforge.ai.infrastructure.output.ModelOutputTruncationResult;
import com.clipforge.observability.usage.ModelCallRecord;
import com.clipforge.observability.usage.ModelUsageRecorder;
import com.clipforge.prompt.budget.TokenBudgetDecision;
import com.clipforge.prompt.budget.TokenBudgetPolicy;
import com.clipforge.prompt.compress.PromptCompressionPolicy;
import com.clipforge.prompt.compress.PromptCompressionRequest;
import com.clipforge.prompt.compress.PromptCompressionResult;
import com.clipforge.prompt.token.CreationPlanPromptTokenEstimateService;
import com.clipforge.prompt.token.PromptTokenEstimateResult;
import com.clipforge.creation.knowledge.CreativeKnowledge;
import com.clipforge.observability.trace.TraceRecorder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * LLMCreationPlanGenerator（大语言模型创作方案生成器）。
 *
 * <p>该组件负责把已经整理好的 AgentContext（智能体上下文）、AgentPlan（智能体规划）和
 * CreativeKnowledge（创作知识）渲染成 Prompt（提示词），并通过 ChatModelClient（聊天模型客户端）
 * 调用真实 LLM（大语言模型）生成结构化创作方案。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.model", name = "mock-enabled", havingValue = "false")
@RequiredArgsConstructor
public class LLMCreationPlanGenerator implements CreationPlanGenerator {

    /** traceRecorder（执行链路记录器）用于记录生成、模型调用、修复等关键事件。 */
    private final TraceRecorder traceRecorder;

    /** creationPlanPromptRenderer（创作方案提示词渲染器）用于构造 system/user（系统/用户）提示词。 */
    private final CreationPlanPromptRenderer creationPlanPromptRenderer;

    /** chatModelClient（聊天模型客户端）用于屏蔽不同模型供应商的调用差异。 */
    private final ChatModelClient chatModelClient;

    /** chatModelProperties（聊天模型配置）用于读取温度、最大输出 Token（词元）等参数。 */
    private final ChatModelProperties chatModelProperties;

    /** creationPlanJsonParser（创作方案 JSON 解析器）用于把模型文本输出解析成结构化对象。 */
    private final CreationPlanJsonParser creationPlanJsonParser;

    /** creationPlanOutputValidator（创作方案输出校验器）用于校验结构化结果是否满足最低契约。 */
    private final CreationPlanOutputValidator creationPlanOutputValidator;

    /** modelUsageRecorder（模型用量记录器）用于保存每次模型调用的 Token（词元）和耗时。 */
    private final ModelUsageRecorder modelUsageRecorder;

    /** promptTokenEstimateService（提示词词元估算服务）用于在调用模型前估算上下文长度。 */
    private final CreationPlanPromptTokenEstimateService promptTokenEstimateService;

    /** tokenBudgetPolicy（词元预算策略）用于判断本次 Prompt（提示词）是否超过预算。 */
    private final TokenBudgetPolicy tokenBudgetPolicy;

    /** promptCompressionPolicy（提示词压缩策略）用于在模型调用前裁剪动态上下文。 */
    private final PromptCompressionPolicy promptCompressionPolicy;

    /** outputTruncationDetector（输出截断识别器）用于在解析前识别模型是否疑似没说完。 */
    private final ModelOutputTruncationDetector outputTruncationDetector = new ModelOutputTruncationDetector(new JsonCompletenessChecker());

    /** outputRepairEnabled（输出修复开关）用于控制模型输出非法时是否进行一次修复调用。 */
    @Value("${clipforge.model.output-repair-enabled:true}")
    private boolean outputRepairEnabled = true;

    /** printCreationPromptAndResponse（打印创作提示词和响应开关）用于真实集成测试观察最终模型输入输出。 */
    @Value("${clipforge.model.print-creation-prompt-and-response:false}")
    private boolean printCreationPromptAndResponse = false;

    /**
     * generate（生成）用于通过真实 LLM（大语言模型）生成创作方案。
     *
     * @param context AgentContext（智能体上下文）
     * @param plan AgentPlan（智能体规划）
     * @return CreationPlanResult（创作方案结果）
     */
    @Override
    public CreationPlanResult generate(AgentContext context, AgentPlan plan) {
        // 记录真实生成器启动事件，方便 Trace（执行链路追踪）复盘。
        traceRecorder.record(context.getRunId(), "LLM_GENERATOR_STARTED", "真实模型生成器开始生成创作方案", "规划步骤数：" + plan.getSteps().size());

        // 从上下文中读取已经检索得到的创作知识。
        CreativeKnowledge knowledge = (CreativeKnowledge) context.getArtifact("CREATIVE_KNOWLEDGE");

        // 首次渲染原始创作方案 Prompt（提示词）。
        RenderedCreationPlanPrompt renderedPrompt = creationPlanPromptRenderer.render(context, plan, knowledge);

        // 把原始 Prompt（提示词）写入上下文，方便测试和链路排查。
        context.putArtifact("RENDERED_CREATION_PLAN_PROMPT", renderedPrompt);

        // 估算原始 Prompt（提示词）长度。
        PromptTokenEstimateResult tokenEstimateResult = promptTokenEstimateService.estimateCreationPlanPrompt(context, renderedPrompt);

        // 把估算结果写入上下文。
        context.putArtifact("CREATION_PLAN_PROMPT_TOKEN_ESTIMATE", tokenEstimateResult);

        // 根据估算结果生成 Token Budget（词元预算）决策。
        TokenBudgetDecision tokenBudgetDecision = tokenBudgetPolicy.decide("CREATION_PLAN_GENERATION", tokenEstimateResult);

        // 把预算决策写入上下文。
        context.putArtifact("TOKEN_BUDGET_DECISION", tokenBudgetDecision);

        // 按预算结果执行本地 Prompt Compression（提示词压缩）。
        PromptCompressionResult compressionResult = applyPromptCompressionIfNeeded(context, plan, knowledge, tokenEstimateResult, tokenBudgetDecision);

        // 如果发生压缩，需要重新渲染并重新估算，保证最终发给模型的是压缩后的 Prompt（提示词）。
        if (Boolean.TRUE.equals(compressionResult.getCompressed())) {
            renderedPrompt = creationPlanPromptRenderer.render(context, plan, knowledge);
            context.putArtifact("RENDERED_CREATION_PLAN_PROMPT", renderedPrompt);
            tokenEstimateResult = promptTokenEstimateService.estimateCreationPlanPrompt(context, renderedPrompt);
            context.putArtifact("CREATION_PLAN_PROMPT_TOKEN_ESTIMATE", tokenEstimateResult);
            tokenBudgetDecision = tokenBudgetPolicy.decide("CREATION_PLAN_GENERATION", tokenEstimateResult);
            context.putArtifact("TOKEN_BUDGET_DECISION", tokenBudgetDecision);
            compressionResult.getReport().setAfterEstimatedTokens(tokenEstimateResult.getTotalEstimatedTokens());
            context.putArtifact("PROMPT_COMPRESSION_REPORT", compressionResult.getReport());
        }

        // 真实模型测试时只打印最终生成方案 Prompt（提示词）。
        printCreationPromptIfEnabled(renderedPrompt);

        // 记录最终 Prompt（提示词）渲染完成事件。
        traceRecorder.record(context.getRunId(), "PROMPT_RENDERED", "创作方案提示词已渲染", "system长度：" + renderedPrompt.getSystemPrompt().length()
                + "，user长度：" + renderedPrompt.getUserPrompt().length()
                + "，估算Token：" + tokenEstimateResult.getTotalEstimatedTokens()
                + "，预算：" + tokenBudgetDecision.getInputBudgetTokens()
                + "，是否超预算：" + tokenBudgetDecision.getOverBudget()
                + "，风险等级：" + tokenBudgetDecision.getRiskLevel());

        // 构造真实聊天模型请求。
        ChatModelRequest chatModelRequest = ChatModelRequest.builder()
                .runId(context.getRunId())
                .purpose("CREATION_PLAN_GENERATION")
                .temperature(chatModelProperties.getTemperature())
                .maxTokens(chatModelProperties.getMaxTokens())
                .messages(List.of(
                        ChatMessage.system(renderedPrompt.getSystemPrompt()),
                        ChatMessage.user(renderedPrompt.getUserPrompt())
                ))
                .build();

        // 记录模型调用开始事件。
        traceRecorder.record(context.getRunId(), "MODEL_CALL_STARTED", "开始调用真实聊天模型", "用途：" + chatModelRequest.getPurpose());

        // 调用模型并记录 Token（词元）用量。
        ChatModelResponse chatModelResponse = callModelAndRecord(context, chatModelRequest, "MODEL_CALL_FINISHED", "MODEL_CALL_FAILED");

        // 真实模型测试时只打印模型返回的原始方案。
        printCreationResponseIfEnabled(chatModelResponse);

        // 把模型响应写入上下文。
        context.putArtifact("CHAT_MODEL_RESPONSE", chatModelResponse);

        // 解析并校验模型输出，必要时发起一次结构化修复调用。
        CreationPlanResult result = parseAndValidateWithOptionalRepair(context, chatModelRequest, chatModelResponse);

        // 把生成结果写入上下文。
        context.putArtifact("GENERATED_PLAN_RESULT", result);

        // 记录真实生成器完成事件。
        traceRecorder.record(context.getRunId(), "LLM_GENERATOR_FINISHED", "真实模型生成器完成创作方案", "标题候选数：" + result.getTitleCandidates().size() + "，模型原始评分：" + result.getScore());

        // 返回结构化创作方案。
        return result;
    }

    /**
     * applyPromptCompressionIfNeeded（按需压缩提示词）用于在模型调用前裁剪动态上下文。
     *
     * @param context AgentContext（智能体上下文）
     * @param plan AgentPlan（智能体规划）
     * @param knowledge CreativeKnowledge（创作知识）
     * @param estimateResult Prompt Token（提示词词元）估算结果
     * @param budgetDecision Token Budget（词元预算）决策
     * @return PromptCompressionResult（提示词压缩结果）
     */
    private PromptCompressionResult applyPromptCompressionIfNeeded(AgentContext context, AgentPlan plan, CreativeKnowledge knowledge, PromptTokenEstimateResult estimateResult, TokenBudgetDecision budgetDecision) {
        // 调用压缩策略，压缩策略内部会把覆盖文本写入 AgentContext（智能体上下文）。
        PromptCompressionResult result = promptCompressionPolicy.compress(PromptCompressionRequest.builder()
                .context(context)
                .estimateResult(estimateResult)
                .budgetDecision(budgetDecision)
                .build());

        // 保存压缩结果对象，便于测试和链路复盘。
        context.putArtifact("PROMPT_COMPRESSION_RESULT", result);

        // 保存压缩报告对象，便于后续接口或测试读取。
        context.putArtifact("PROMPT_COMPRESSION_REPORT", result.getReport());

        // 只有真正发生裁剪时才记录压缩事件，避免 Trace（执行链路追踪）噪声过大。
        if (Boolean.TRUE.equals(result.getCompressed())) {
            traceRecorder.record(context.getRunId(), "PROMPT_COMPRESSION_APPLIED", "Prompt（提示词）动态上下文已裁剪", "beforeEstimatedTokens：" + estimateResult.getTotalEstimatedTokens()
                    + "，planId：" + plan.getPlanId()
                    + "，knowledgePlatform：" + (knowledge == null ? "" : knowledge.getPlatform()));
        }

        // 返回压缩结果。
        return result;
    }

    /**
     * printCreationPromptIfEnabled（按需打印创作提示词）用于真实模型测试观察最终 Prompt（提示词）。
     *
     * @param renderedPrompt 已渲染创作方案提示词
     */
    private void printCreationPromptIfEnabled(RenderedCreationPlanPrompt renderedPrompt) {
        // 未开启打印时直接返回，避免接口日志过长。
        if (!printCreationPromptAndResponse) {
            return;
        }

        // 打印最终 system（系统）提示词和 user（用户）提示词。
        System.out.println("===== 真实 LLM（大语言模型）生成创作方案前 Prompt（提示词）====");
        System.out.println("systemPrompt:");
        System.out.println(renderedPrompt.getSystemPrompt());
        System.out.println("userPrompt:");
        System.out.println(renderedPrompt.getUserPrompt());
    }

    /**
     * printCreationResponseIfEnabled（按需打印创作响应）用于真实模型测试观察模型原始输出。
     *
     * @param response 聊天模型响应
     */
    private void printCreationResponseIfEnabled(ChatModelResponse response) {
        // 未开启打印时直接返回，避免接口日志过长。
        if (!printCreationPromptAndResponse) {
            return;
        }

        // 打印模型返回的原始文本。
        System.out.println("===== 真实 LLM（大语言模型）生成创作方案响应 =====");
        System.out.println(response == null ? "" : response.getContent());
    }

    /**
     * parseAndValidateWithOptionalRepair（解析校验并可选修复）用于处理模型输出不符合 JSON（对象表示格式）契约的情况。
     *
     * @param context AgentContext（智能体上下文）
     * @param originalRequest 原始模型请求
     * @param originalResponse 原始模型响应
     * @return CreationPlanResult（创作方案结果）
     */
    private CreationPlanResult parseAndValidateWithOptionalRepair(AgentContext context, ChatModelRequest originalRequest, ChatModelResponse originalResponse) {
        ModelOutputTruncationResult truncationResult = outputTruncationDetector.detect(
                originalResponse == null ? null : originalResponse.getContent(),
                originalResponse == null ? null : originalResponse.getFinishReason(),
                originalRequest == null ? null : originalRequest.getMaxTokens());
        context.putArtifact("MODEL_OUTPUT_TRUNCATION_RESULT", truncationResult);
        traceRecorder.record(context.getRunId(), "MODEL_OUTPUT_TRUNCATION_CHECKED", "模型输出截断检查完成",
                "truncated=" + truncationResult.isTruncated()
                        + "，issueType=" + truncationResult.getIssueType()
                        + "，confidence=" + truncationResult.getConfidence()
                        + "，responseLength=" + truncationResult.getResponseLength()
                        + "，reason=" + truncationResult.getReason());
        if (truncationResult.isTruncated()) {
            traceRecorder.record(context.getRunId(), "MODEL_OUTPUT_TRUNCATED", "模型输出疑似被截断",
                    "issueType=" + truncationResult.getIssueType() + "，reason=" + truncationResult.getReason());
        }
        try {
            return parseAndValidate(originalResponse);
        } catch (BusinessException exception) {
            if (!shouldRepair(exception)) {
                throw exception;
            }
            if (!outputRepairEnabled) {
                traceRecorder.record(context.getRunId(), "MODEL_OUTPUT_REPAIR_SKIPPED", "模型输出修复已关闭", "错误：" + exception.getMessage());
                context.putArtifact("MODEL_OUTPUT_REPAIR_RESULT", ModelOutputRepairResult.builder()
                        .status(truncationResult.isTruncated() ? ModelOutputRepairStatus.DEGRADED : ModelOutputRepairStatus.UNREPAIRABLE)
                        .truncationResult(truncationResult)
                        .originalContent(originalResponse == null ? null : originalResponse.getContent())
                        .message(exception.getMessage())
                        .build());
                throw exception;
            }
            if (truncationResult.isTruncated()) {
                traceRecorder.record(context.getRunId(), "MODEL_OUTPUT_TRUNCATION_REPAIR_STARTED", "模型输出截断修复开始", "reason=" + truncationResult.getReason());
            }
            traceRecorder.record(context.getRunId(), "MODEL_OUTPUT_REPAIR_STARTED", "模型输出不符合结构，开始一次修复", "错误：" + exception.getMessage());
            ChatModelRequest repairRequest = buildRepairRequest(context, originalRequest, originalResponse, exception);
            ChatModelResponse repairResponse = callModelAndRecord(context, repairRequest, "MODEL_OUTPUT_REPAIR_FINISHED", "MODEL_OUTPUT_REPAIR_FAILED");
            context.putArtifact("REPAIRED_CHAT_MODEL_RESPONSE", repairResponse);
            try {
                CreationPlanResult repairedResult = parseAndValidate(repairResponse);
                context.putArtifact("MODEL_OUTPUT_REPAIR_RESULT", ModelOutputRepairResult.builder()
                        .status(ModelOutputRepairStatus.REPAIRED)
                        .truncationResult(truncationResult)
                        .originalContent(originalResponse == null ? null : originalResponse.getContent())
                        .repairedContent(repairResponse == null ? null : repairResponse.getContent())
                        .message("模型输出修复成功")
                        .build());
                if (truncationResult.isTruncated()) {
                    traceRecorder.record(context.getRunId(), "MODEL_OUTPUT_TRUNCATION_REPAIR_SUCCEEDED", "模型输出截断修复成功", "titleCandidates=" + repairedResult.getTitleCandidates().size());
                }
                traceRecorder.record(context.getRunId(), "MODEL_OUTPUT_REPAIR_SUCCEEDED", "模型输出修复成功", "标题候选数：" + repairedResult.getTitleCandidates().size());
                return repairedResult;
            } catch (BusinessException repairException) {
                context.putArtifact("MODEL_OUTPUT_REPAIR_RESULT", ModelOutputRepairResult.builder()
                        .status(truncationResult.isTruncated() ? ModelOutputRepairStatus.DEGRADED : ModelOutputRepairStatus.REPAIR_FAILED)
                        .truncationResult(truncationResult)
                        .originalContent(originalResponse == null ? null : originalResponse.getContent())
                        .repairedContent(repairResponse == null ? null : repairResponse.getContent())
                        .message(repairException.getMessage())
                        .build());
                if (truncationResult.isTruncated()) {
                    traceRecorder.record(context.getRunId(), "MODEL_OUTPUT_TRUNCATION_REPAIR_FAILED", "模型输出截断修复失败", repairException.getMessage());
                    traceRecorder.record(context.getRunId(), "MODEL_OUTPUT_DEGRADED", "模型输出已降级为结构化错误", "请减少素材、缩短输入或提高最大输出 Token 后重试");
                    throw new BusinessException(ErrorCode.MODEL_OUTPUT_TRUNCATED, "模型输出疑似被截断，自动修复仍失败。请减少素材数量、缩短输入或提高最大输出 Token 后重试。原因：" + repairException.getMessage(), repairException);
                }
                throw repairException;
            }
        }
    }

    /**
     * parseAndValidate（解析并校验）用于把模型响应转换为有效创作方案。
     *
     * @param response 聊天模型响应
     * @return CreationPlanResult（创作方案结果）
     */
    private CreationPlanResult parseAndValidate(ChatModelResponse response) {
        // 解析模型输出 JSON（对象表示格式）。
        CreationPlanResult result = creationPlanJsonParser.parse(response.getContent());

        // 校验结构化创作方案是否满足最低输出要求。
        creationPlanOutputValidator.validate(result);

        // 返回校验通过的结果。
        return result;
    }

    /**
     * shouldRepair（是否修复）用于判断当前异常是否属于可由模型再次修复的输出问题。
     *
     * @param exception 业务异常
     * @return true 表示可以尝试修复
     */
    private boolean shouldRepair(BusinessException exception) {
        // 只有模型输出解析失败或输出校验失败时才尝试修复。
        return exception.getErrorCode() == ErrorCode.MODEL_OUTPUT_PARSE_ERROR || exception.getErrorCode() == ErrorCode.AGENT_OUTPUT_INVALID;
    }

    /**
     * buildRepairRequest（构造修复请求）用于要求模型把错误输出修复成合法 JSON（对象表示格式）。
     *
     * @param context AgentContext（智能体上下文）
     * @param originalRequest 原始模型请求
     * @param originalResponse 原始模型响应
     * @param exception 原始业务异常
     * @return ChatModelRequest（聊天模型请求）
     */
    private ChatModelRequest buildRepairRequest(AgentContext context, ChatModelRequest originalRequest, ChatModelResponse originalResponse, BusinessException exception) {
        // 从 Prompt（提示词）文件加载输出修复提示词，避免修复规则写死在 Java（爪哇语言）代码中。
        RenderedPrompt repairPrompt = creationPlanPromptRenderer.renderRepair(exception.getMessage(), originalResponse.getContent());

        // 构造低温度修复请求，降低修复输出随机性。
        return ChatModelRequest.builder()
                .runId(context.getRunId())
                .purpose("CREATION_PLAN_OUTPUT_REPAIR")
                .temperature(0.0)
                .maxTokens(originalRequest.getMaxTokens())
                .messages(List.of(
                        ChatMessage.system(repairPrompt.getSystemPrompt()),
                        ChatMessage.user(repairPrompt.getUserPrompt())
                ))
                .build();
    }

    /**
     * callModelAndRecord（调用模型并记录）用于统一处理模型调用、Trace（执行链路追踪）和 Token（词元）用量记录。
     *
     * @param context AgentContext（智能体上下文）
     * @param request 聊天模型请求
     * @param successEventType 成功事件类型
     * @param failureEventType 失败事件类型
     * @return ChatModelResponse（聊天模型响应）
     */
    private ChatModelResponse callModelAndRecord(AgentContext context, ChatModelRequest request, String successEventType, String failureEventType) {
        // 记录调用开始时间，用于计算耗时。
        long startedAt = System.currentTimeMillis();
        try {
            // 调用底层模型客户端。
            ChatModelResponse response = chatModelClient.chat(request);

            // 计算本次调用耗时。
            long latencyMs = System.currentTimeMillis() - startedAt;

            // 记录成功调用的模型用量。
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

            // 记录模型调用成功事件。
            traceRecorder.record(context.getRunId(), successEventType, "模型调用成功", "purpose：" + request.getPurpose()
                    + "，provider：" + response.getProvider()
                    + "，model：" + response.getModelName()
                    + "，totalTokens：" + response.getTotalTokens()
                    + "，latencyMs：" + latencyMs);

            // 返回模型响应。
            return response;
        } catch (RuntimeException exception) {
            // 计算失败调用耗时。
            long latencyMs = System.currentTimeMillis() - startedAt;

            // 记录失败调用的模型用量。
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

            // 记录模型调用失败事件。
            traceRecorder.record(context.getRunId(), failureEventType, "模型调用失败", "purpose：" + request.getPurpose()
                    + "，error：" + exception.getMessage()
                    + "，latencyMs：" + latencyMs);

            // 抛出原始异常，交给上层处理。
            throw exception;
        }
    }

    /**
     * artifactText（读取文本中间产物）用于从 AgentContext（智能体上下文）里安全读取项目、会话等运行元数据。
     *
     * @param context AgentContext（智能体上下文）
     * @param key 中间产物键
     * @return 文本值；不存在时返回 null
     */
    private String artifactText(AgentContext context, String key) {
        // 如果上下文为空，直接返回 null，避免异常覆盖原始模型调用错误。
        if (context == null) {
            return null;
        }

        // 读取中间产物。
        Object value = context.getArtifact(key);

        // 只接收非空字符串。
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }

        // 其他类型或空字符串都视为不存在。
        return null;
    }
}
