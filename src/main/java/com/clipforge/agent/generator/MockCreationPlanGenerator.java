package com.clipforge.agent.generator;

import com.clipforge.agent.AgentContext;
import com.clipforge.agent.AgentPlan;
import com.clipforge.agent.CreationPlanResult;
import com.clipforge.agent.prompt.CreationPlanPromptRenderer;
import com.clipforge.agent.prompt.RenderedCreationPlanPrompt;
import com.clipforge.model.chat.ChatMessage;
import com.clipforge.model.chat.ChatModelClient;
import com.clipforge.model.chat.ChatModelRequest;
import com.clipforge.model.chat.ChatModelResponse;
import com.clipforge.prompt.budget.TokenBudgetDecision;
import com.clipforge.prompt.budget.TokenBudgetPolicy;
import com.clipforge.prompt.compress.PromptCompressionPolicy;
import com.clipforge.prompt.compress.PromptCompressionRequest;
import com.clipforge.prompt.compress.PromptCompressionResult;
import com.clipforge.prompt.token.CreationPlanPromptTokenEstimateService;
import com.clipforge.prompt.token.PromptTokenEstimateResult;
import com.clipforge.tool.CreativeKnowledge;
import com.clipforge.trace.TraceRecorder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MockCreationPlanGenerator（模拟创作方案生成器）。
 *
 * <p>该生成器在未接入 LLM（大语言模型）前返回稳定结果，方便验证 Agent（智能体）工程链路。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.model", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MockCreationPlanGenerator implements CreationPlanGenerator {

    /** traceRecorder（执行链路记录器）用于记录生成过程。 */
    private final TraceRecorder traceRecorder;

    /** creationPlanPromptRenderer（创作方案提示词渲染器）用于生成后续真实 LLM（大语言模型）调用所需的 Prompt（提示词）。 */
    private final CreationPlanPromptRenderer creationPlanPromptRenderer;

    /** chatModelClient（聊天模型客户端）用于屏蔽 Mock（模拟）模型和后续真实模型调用差异。 */
    private final ChatModelClient chatModelClient;

    /** promptTokenEstimateService（提示词词元估算服务）用于在调用 Mock（模拟）模型前估算 Prompt（提示词）长度。 */
    private final CreationPlanPromptTokenEstimateService promptTokenEstimateService;

    /** tokenBudgetPolicy（词元预算策略）用于根据估算结果生成预算决策。 */
    private final TokenBudgetPolicy tokenBudgetPolicy;

    /** promptCompressionPolicy（提示词压缩策略）用于让 Mock（模拟）链路也遵守上下文裁剪规则。 */
    private final PromptCompressionPolicy promptCompressionPolicy;

    /**
     * generate（生成）用于生成当前阶段的模拟创作方案。
     *
     * @param context 智能体上下文
     * @param plan 智能体计划
     * @return 创作方案结果
     */
    @Override
    public CreationPlanResult generate(AgentContext context, AgentPlan plan) {
        // 记录生成开始事件。
        traceRecorder.record(context.getRunId(), "GENERATOR_STARTED", "生成器开始生成创作方案", "计划步骤数：" + plan.getSteps().size());
        // 从上下文中读取创作知识。
        CreativeKnowledge knowledge = (CreativeKnowledge) context.getArtifact("CREATIVE_KNOWLEDGE");
        // 渲染创作方案 Prompt（提示词），当前模拟生成器先保存它，后续真实 LLM（大语言模型）生成器会直接使用它。
        RenderedCreationPlanPrompt renderedPrompt = creationPlanPromptRenderer.render(context, plan, knowledge);
        // 把已渲染 Prompt（提示词）写入上下文，方便测试、Trace（执行链路追踪）和后续模型调用复用。
        context.putArtifact("RENDERED_CREATION_PLAN_PROMPT", renderedPrompt);
        // 估算 Prompt（提示词）Token（词元）占用。
        PromptTokenEstimateResult tokenEstimateResult = promptTokenEstimateService.estimateCreationPlanPrompt(context, renderedPrompt);
        // 把估算结果写入上下文。
        context.putArtifact("CREATION_PLAN_PROMPT_TOKEN_ESTIMATE", tokenEstimateResult);
        // 生成 Token Budget（词元预算）决策。
        TokenBudgetDecision tokenBudgetDecision = tokenBudgetPolicy.decide("CREATION_PLAN_GENERATION", tokenEstimateResult);
        // 把预算决策写入上下文。
        context.putArtifact("TOKEN_BUDGET_DECISION", tokenBudgetDecision);
        PromptCompressionResult compressionResult = applyPromptCompressionIfNeeded(context, plan, knowledge, tokenEstimateResult, tokenBudgetDecision);
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
        // 记录提示词渲染完成事件，方便观察 Prompt（提示词）长度。
        traceRecorder.record(context.getRunId(), "PROMPT_RENDERED", "创作方案提示词渲染完成", "system长度：" + renderedPrompt.getSystemPrompt().length() + "，user长度：" + renderedPrompt.getUserPrompt().length() + "，估算Token：" + tokenEstimateResult.getTotalEstimatedTokens() + "，预算输入：" + tokenBudgetDecision.getInputBudgetTokens() + "，是否超预算：" + tokenBudgetDecision.getOverBudget() + "，风险等级：" + tokenBudgetDecision.getRiskLevel());
        // 构造聊天模型请求，当前会调用 MockChatModelClient（模拟聊天模型客户端）。
        ChatModelRequest chatModelRequest = ChatModelRequest.builder()
                // 设置 Agent（智能体）运行编号。
                .runId(context.getRunId())
                // 设置本次模型调用用途。
                .purpose("CREATION_PLAN_GENERATION")
                // 设置温度参数，创作规划需要一定创造性但不能过于发散。
                .temperature(0.2)
                // 设置最大输出 Token（词元）数量。
                .maxTokens(2048)
                // 设置 system（系统角色）和 user（用户角色）消息。
                .messages(List.of(
                        // 添加系统提示词消息。
                        ChatMessage.system(renderedPrompt.getSystemPrompt()),
                        // 添加用户提示词消息。
                        ChatMessage.user(renderedPrompt.getUserPrompt())
                ))
                // 构建聊天模型请求。
                .build();
        // 记录模型调用开始事件。
        traceRecorder.record(context.getRunId(), "MODEL_CALL_STARTED", "聊天模型客户端开始调用", "用途：" + chatModelRequest.getPurpose());
        // 调用聊天模型客户端，当前默认走 Mock（模拟）实现。
        ChatModelResponse chatModelResponse = chatModelClient.chat(chatModelRequest);
        // 把模型响应写入上下文，后续真实生成器会从 content（内容）解析结构化 JSON（对象表示格式）。
        context.putArtifact("CHAT_MODEL_RESPONSE", chatModelResponse);
        // 记录模型调用完成事件。
        traceRecorder.record(context.getRunId(), "MODEL_CALL_FINISHED", "聊天模型客户端调用完成", "provider：" + chatModelResponse.getProvider() + "，model：" + chatModelResponse.getModelName() + "，totalTokens：" + chatModelResponse.getTotalTokens());
        // 读取第一条脚本结构，用于让模拟结果体现知识检索结果。
        String scriptPattern = knowledge.getScriptPatterns().get(0);
        // 创建创作方案结果对象。
        CreationPlanResult result = new CreationPlanResult();
        // 设置创作主题。
        result.setTheme("围绕“" + context.getGoal() + "”的" + context.getPlatform() + "创作方案");
        // 设置目标受众。
        result.setTargetAudience("对该主题感兴趣、希望快速理解核心价值的目标用户");
        // 设置开头钩子。
        result.setHook("你是不是也想把一个想法快速变成能发布的短视频方案？");
        // 设置分镜脚本，并把检索到的脚本结构放入第一条，体现 RAG（检索增强生成）思想。
        result.setStoryboard(List.of(
                "内容结构：" + scriptPattern,
                "镜头 1：用问题开场，快速点出用户痛点。",
                "镜头 2：展示核心思路，说明方案如何解决问题。",
                "镜头 3：给出结构化步骤，让观众看到可执行路径。",
                "镜头 4：总结价值，并引导用户收藏或评论。"));
        // 设置旁白脚本。
        result.setVoiceoverScript("这条内容会先抓住用户痛点，再用清晰步骤展示解决方案，最后用总结和行动引导提高互动。");
        // 设置字幕建议。
        result.setSubtitleSuggestions(List.of("痛点是什么？", "方案怎么做？", "为什么值得看？", "下一步行动"));
        // 设置标题候选。
        result.setTitleCandidates(List.of("一个想法如何变成短视频方案", "别再只会堆素材了，先学会规划内容", "创作前多做这一步，内容更稳"));
        // 设置发布文案。
        result.setPublishCopy("把创作目标拆成主题、钩子、分镜、旁白和标题，内容生产会更清晰，也更容易复盘。");
        // 设置话题标签。
        result.setHashtags(List.of("#短视频创作", "#内容规划", "#AI创作", "#" + context.getPlatform()));
        // 把生成结果写入上下文，供 Evaluator（评估器）继续使用。
        context.putArtifact("GENERATED_PLAN_RESULT", result);
        // 记录生成完成事件。
        traceRecorder.record(context.getRunId(), "GENERATOR_FINISHED", "生成器完成创作方案", "标题候选数：" + result.getTitleCandidates().size());
        // 返回生成结果。
        return result;
    }
    /**
     * applyPromptCompressionIfNeeded（按需压缩提示词）用于在 Mock（模拟）模型调用前裁剪动态上下文。
     *
     * @param context AgentContext（智能体上下文）
     * @param plan AgentPlan（智能体计划）
     * @param knowledge CreativeKnowledge（创作知识）
     * @param estimateResult Prompt Token（提示词词元）估算结果
     * @param budgetDecision Token Budget（词元预算）决策
     * @return Prompt Compression（提示词压缩）结果
     */
    private PromptCompressionResult applyPromptCompressionIfNeeded(AgentContext context, AgentPlan plan, CreativeKnowledge knowledge, PromptTokenEstimateResult estimateResult, TokenBudgetDecision budgetDecision) {
        PromptCompressionResult result = promptCompressionPolicy.compress(PromptCompressionRequest.builder()
                .context(context)
                .estimateResult(estimateResult)
                .budgetDecision(budgetDecision)
                .build());
        context.putArtifact("PROMPT_COMPRESSION_RESULT", result);
        context.putArtifact("PROMPT_COMPRESSION_REPORT", result.getReport());
        if (Boolean.TRUE.equals(result.getCompressed())) {
            traceRecorder.record(context.getRunId(), "PROMPT_COMPRESSION_APPLIED", "Prompt（提示词）动态上下文已裁剪", "beforeEstimatedTokens：" + estimateResult.getTotalEstimatedTokens() + "，planId：" + plan.getPlanId() + "，knowledgePlatform：" + (knowledge == null ? "" : knowledge.getPlatform()));
        }
        return result;
    }
}
