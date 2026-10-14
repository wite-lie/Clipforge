package com.clipforge.agent.workflow;

import com.clipforge.creation.model.AgentContext;
import com.clipforge.creation.model.AgentPlan;
import com.clipforge.contract.creation.ArticleCreationPlan;
import com.clipforge.contract.creation.CreationOutputType;
import com.clipforge.contract.creation.CreationPlanResult;
import com.clipforge.creation.article.ArticleCreationPlanAdapter;
import com.clipforge.creation.article.ArticleCreationPlanGenerator;
import com.clipforge.creation.model.CreationWorkflowResult;
import com.clipforge.creation.generator.CreationPlanGenerator;
import com.clipforge.agent.material.MaterialUnderstandingAgent;
import com.clipforge.creation.planner.CreationPlanner;
import com.clipforge.contract.material.MaterialResponse;
import com.clipforge.creation.evaluation.CreationPlanEvaluationResult;
import com.clipforge.creation.evaluation.CreationPlanEvaluator;
import com.clipforge.creation.evaluation.CreationPlanRevisionGenerator;
import com.clipforge.creation.evaluation.CreationPlanRevisionPolicy;
import com.clipforge.material.MaterialUnderstandingResult;
import com.clipforge.quality.record.CreationQualityRecorder;
import com.clipforge.creation.knowledge.CreativeKnowledge;
import com.clipforge.tool.CreativeKnowledgeRetrieveTool;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalRequest;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalResult;
import com.clipforge.knowledge.retrieval.KnowledgeRetriever;
import com.clipforge.observability.trace.TraceRecorder;
import com.clipforge.tool.execution.ToolAction;
import com.clipforge.tool.execution.ToolExecutor;
import com.clipforge.tool.execution.ToolInvocationRequest;
import com.clipforge.material.video.understanding.VideoUnderstandingCreationContext;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FixedCreationWorkflow（固定创作工作流）。
 *
 * <p>该工作流固定系统级执行顺序，并把 Planner（规划器）、Tool（工具）、Generator（生成器）和 Evaluator（评估器）串起来。
 */
@Component
@RequiredArgsConstructor
public class FixedCreationWorkflow {

    /** creativeKnowledgeRetrieveTool（创作知识检索工具）负责获取平台规则和脚本知识。 */
    private final CreativeKnowledgeRetrieveTool creativeKnowledgeRetrieveTool;

    /** knowledgeRetriever（知识检索器）负责检索用户知识库中的真实创作知识。 */
    private final KnowledgeRetriever knowledgeRetriever;

    /** materialUnderstandingAgent（素材理解智能体）负责把图片素材理解为创作可用摘要。 */
    private final MaterialUnderstandingAgent materialUnderstandingAgent;

    /** creationPlanner（创作规划器）负责生成任务内部的 AgentPlan（智能体计划）。 */
    private final CreationPlanner creationPlanner;

    /** creationPlanGenerator（创作方案生成器）负责生成结构化创作方案。 */
    private final CreationPlanGenerator creationPlanGenerator;

    /** articleCreationPlanAdapter（图文方案适配器）用于生成旧接口兼容结果。 */
    private final ArticleCreationPlanAdapter articleCreationPlanAdapter = new ArticleCreationPlanAdapter();

    /** creationPlanEvaluator（创作方案评估器）负责给创作方案评分并提出建议。 */
    private final CreationPlanEvaluator creationPlanEvaluator;

    /** creationPlanRevisionPolicy（创作方案修正策略）负责判断是否触发有限 Reflection（反思修正）。 */
    private final CreationPlanRevisionPolicy creationPlanRevisionPolicy;

    /** creationPlanRevisionGenerator（创作方案修正生成器）负责根据评估结果修正低质量方案。 */
    private final CreationPlanRevisionGenerator creationPlanRevisionGenerator;

    /** traceRecorder（执行链路记录器）用于记录工作流级事件。 */
    private final TraceRecorder traceRecorder;

    /** creationQualityRecorder（创作质量记录器）用于记录方案版本、评估结果和修正关系。 */
    private final CreationQualityRecorder creationQualityRecorder;

    /** toolExecutor（工具执行器）用于统一包裹工具调用；手动构造测试未注入时保留直接调用路径。 */
    private ToolExecutor toolExecutor;

    /** articleCreationPlanGenerator（图文方案生成器）用于 ARTICLE_PLAN 分支。 */
    private ArticleCreationPlanGenerator articleCreationPlanGenerator;

    @Autowired
    public void setToolExecutor(ObjectProvider<ToolExecutor> toolExecutorProvider) {
        this.toolExecutor = toolExecutorProvider == null ? null : toolExecutorProvider.getIfAvailable();
    }

    @Autowired
    public void setArticleCreationPlanGenerator(ObjectProvider<ArticleCreationPlanGenerator> articleCreationPlanGeneratorProvider) {
        this.articleCreationPlanGenerator = articleCreationPlanGeneratorProvider == null ? null : articleCreationPlanGeneratorProvider.getIfAvailable();
    }

    /**
     * run（运行工作流）用于执行当前阶段固定系统流程。
     *
     * @param context 智能体上下文
     * @return 创作工作流结果
     */
    public CreationWorkflowResult run(AgentContext context) {
        // 记录工作流开始事件。
        traceRecorder.record(context.getRunId(), "WORKFLOW_STARTED", "固定创作工作流开始执行", "系统流程：素材理解 -> 知识检索 -> 规划 -> 生成 -> 评估 -> 按需修正");
        // 记录记忆上下文读取结果，方便复盘本次创作是否使用了用户偏好、项目记忆、历史摘要或会话摘要。
        recordMemoryContextLoaded(context);
        // 调用素材理解智能体，把图片素材转换为后续 Prompt（提示词）可使用的素材摘要。
        MaterialUnderstandingResult materialUnderstandingResult = understandMaterials(context);
        // 把素材理解结果写入上下文，供生成、评估和修正节点读取。
        context.putArtifact("MATERIAL_UNDERSTANDING_RESULT", materialUnderstandingResult);
        // 把素材摘要单独写入上下文，降低后续 Prompt（提示词）渲染器读取成本。
        context.putArtifact("MATERIAL_SUMMARY", materialUnderstandingResult.getMaterialSummary());
        // 记录素材理解完成后的工作流事件。
        traceRecorder.record(context.getRunId(), "WORKFLOW_MATERIAL_SUMMARY_READY", "工作流已获得素材摘要", "图片素材数量：" + materialUnderstandingResult.getImageMaterialCount());
        // 记录视频理解摘要读取和注入结果。
        recordVideoUnderstandingContext(context);
        // 调用创作知识检索工具，获取外部创作知识。
        CreativeKnowledge knowledge = executeTool(
                context.getRunId(),
                "creative_knowledge_retrieve",
                "platform：" + context.getPlatform(),
                () -> creativeKnowledgeRetrieveTool.retrieve(context));
        // 把知识摘要写入执行链路，方便观察工作流中工具输出。
        traceRecorder.record(context.getRunId(), "WORKFLOW_KNOWLEDGE_READY", "工作流已获得创作知识", "平台规则数：" + knowledge.getPlatformRules().size());
        // 调用知识检索器，把用户知识库中的真实内容也注入到工作流里。
        KnowledgeRetrievalResult retrievalResult = retrieveWorkflowKnowledge(context, materialUnderstandingResult);
        // 把检索结果写入上下文，供 Prompt（提示词）渲染器、评估器和修正器复用。
        context.putArtifact("KNOWLEDGE_RETRIEVAL_RESULT", retrievalResult);
        // 记录知识检索完成事件。
        traceRecorder.record(context.getRunId(), "WORKFLOW_RAG_READY", "工作流已获得知识库检索结果", "来源：" + retrievalResult.getSource() + "，降级：" + retrievalResult.isFallbackUsed() + "，命中切片数：" + safeSize(retrievalResult.getChunks()) + "，注入证据数：" + safeSize(retrievalResult.getPromptEvidences()));
        // 调用 Planner（规划器）生成 AgentPlan（智能体计划）。
        AgentPlan plan = creationPlanner.plan(context);
        // 根据产物类型调用对应 Generator（生成器）。
        GenerationResult generationResult = generateByOutputType(context, plan);
        // 读取旧接口兼容创作方案结果。
        CreationPlanResult generatedResult = generationResult.legacyResult();
        // 调用 Evaluator（评估器）对创作方案评分并补充改进建议。
        CreationPlanResult evaluatedResult = creationPlanEvaluator.evaluate(context, generatedResult);
        // 记录初始版本和对应评估结果，形成质量复盘版本链的起点。
        recordInitialQualitySnapshot(context, evaluatedResult);
        // 按评估结果尝试执行一次有限 Reflection（反思修正）。
        CreationPlanResult finalResult = reviseOnceIfNeeded(context, evaluatedResult);
        // 把最终结果写入上下文，方便后续节点读取。
        context.putArtifact("FINAL_RESULT", finalResult);
        // 解析真实通用产物：图文分支保存 ArticleCreationPlan，短视频分支保存最终 CreationPlanResult。
        Object outputPayload = resolveOutputPayload(context, generationResult, finalResult);
        // 把真实通用产物写入上下文，方便后续持久化或导出前排查。
        context.putArtifact("FINAL_OUTPUT_PAYLOAD", outputPayload);
        // 记录工作流完成事件。
        traceRecorder.record(context.getRunId(), "WORKFLOW_FINISHED", "固定创作工作流执行完成", "综合评分：" + finalResult.getScore());
        // 返回工作流的计划、兼容结果和真实通用产物。
        return new CreationWorkflowResult(plan, finalResult, outputPayload);
    }

    private GenerationResult generateByOutputType(AgentContext context, AgentPlan plan) {
        CreationOutputType outputType = resolveOutputType(context);
        if (outputType == CreationOutputType.ARTICLE_PLAN) {
            if (articleCreationPlanGenerator == null) {
                throw new IllegalStateException("ARTICLE_PLAN 已请求，但图文创作方案生成器未注入");
            }
            traceRecorder.record(context.getRunId(), "ARTICLE_WORKFLOW_BRANCH_SELECTED", "工作流进入图文创作方案分支", "platform：" + context.getPlatform());
            ArticleCreationPlan articlePlan = articleCreationPlanGenerator.generate(context, plan);
            CreationPlanResult legacyResult = articleCreationPlanAdapter.toLegacyResult(articlePlan);
            return new GenerationResult(legacyResult, articlePlan, outputType);
        }
        traceRecorder.record(context.getRunId(), "VIDEO_PLAN_WORKFLOW_BRANCH_SELECTED", "工作流进入短视频创作方案分支", "platform：" + context.getPlatform());
        CreationPlanResult videoPlan = creationPlanGenerator.generate(context, plan);
        return new GenerationResult(videoPlan, videoPlan, outputType);
    }

    private Object resolveOutputPayload(AgentContext context, GenerationResult generationResult, CreationPlanResult finalResult) {
        if (generationResult.outputType() == CreationOutputType.ARTICLE_PLAN) {
            return generationResult.outputPayload();
        }
        return finalResult;
    }

    private CreationOutputType resolveOutputType(AgentContext context) {
        Object value = context.getArtifact("OUTPUT_TYPE");
        if (value instanceof CreationOutputType outputType) {
            return outputType;
        }
        if (value instanceof String text) {
            return CreationOutputType.resolveOrDefault(text);
        }
        return CreationOutputType.VIDEO_PLAN;
    }

    /**
     * retrieveWorkflowKnowledge（检索工作流知识）用于把用户知识库内容注入当前创作流程。
     *
     * @param context 智能体上下文
     * @param materialUnderstandingResult 素材理解结果
     * @return 知识检索结果
     */
    private KnowledgeRetrievalResult retrieveWorkflowKnowledge(AgentContext context, MaterialUnderstandingResult materialUnderstandingResult) {
        // 先解析知识库编号列表，若未传入则继续检查是否启用全库检索。
        List<String> knowledgeBaseIds = resolveKnowledgeBaseIds(context);
        // 解析是否启用全库检索兜底。
        boolean searchAllKnowledgeBases = resolveSearchAllKnowledgeBases(context);
        // 构造检索 query（查询文本），尽量把用户目标、平台和素材摘要拼进去。
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(context.getGoal());
        queryBuilder.append("；平台：").append(context.getPlatform());
        if (context.getStylePreference() != null && !context.getStylePreference().isBlank()) {
            queryBuilder.append("；风格偏好：").append(context.getStylePreference());
        }
        if (materialUnderstandingResult != null
                && materialUnderstandingResult.getMaterialSummary() != null
                && materialUnderstandingResult.getMaterialSummary().getSummaryText() != null
                && !materialUnderstandingResult.getMaterialSummary().getSummaryText().isBlank()) {
            queryBuilder.append("；素材摘要：").append(materialUnderstandingResult.getMaterialSummary().getSummaryText());
        }
        Object videoArtifact = context.getArtifact("VIDEO_UNDERSTANDING_CREATION_CONTEXT");
        if (videoArtifact instanceof VideoUnderstandingCreationContext videoContext && videoContext.hasAvailableSummary()) {
            queryBuilder.append("；视频摘要：").append(videoContext.getSummaryText());
        }
        // 构造知识检索请求。
        KnowledgeRetrievalRequest request = KnowledgeRetrievalRequest.builder()
                .query(queryBuilder.toString())
                .platform(context.getPlatform())
                .knowledgeBaseIds(knowledgeBaseIds)
                .searchAllKnowledgeBases(searchAllKnowledgeBases)
                .topK(15)
                .staticFallbackEnabled(true)
                .build();
        // 记录知识检索开始事件。
        traceRecorder.record(context.getRunId(), "WORKFLOW_RAG_STARTED", "工作流开始检索知识库内容", "知识库编号：" + String.join(",", knowledgeBaseIds) + "，是否全库检索：" + searchAllKnowledgeBases);
        // 执行检索。
        KnowledgeRetrievalResult result = executeTool(
                context.getRunId(),
                "rag_retrieve",
                "topK：" + request.getTopK() + "，searchAll：" + request.getSearchAllKnowledgeBases(),
                () -> knowledgeRetriever.retrieve(request));
        // 记录检索结果概要。
        traceRecorder.record(context.getRunId(), "WORKFLOW_RAG_FINISHED", "工作流知识库检索完成", "来源：" + result.getSource() + "，是否降级：" + result.isFallbackUsed() + "，提示词上下文长度：" + result.getPromptContext().length() + "，注入证据数：" + safeSize(result.getPromptEvidences()) + "，是否触发证据预算保护：" + (result.getEvidenceAssemblyResult() != null && result.getEvidenceAssemblyResult().isBudgetProtectionTriggered()));
        // 返回结果。
        return result;
    }

    private <T> T executeTool(String runId, String toolName, String inputSummary, ToolAction<T> action) {
        if (toolExecutor == null) {
            return action.execute();
        }
        return toolExecutor.execute(ToolInvocationRequest.<T>builder()
                .runId(runId)
                .toolName(toolName)
                .inputSummary(inputSummary)
                .action(action)
                .build()).getOutput();
    }

    /**
     * recordMemoryContextLoaded（记录记忆上下文加载）用于把 Memory（记忆）命中情况写入 Trace（执行链路追踪）。
     *
     * @param context 智能体上下文
     */
    private void recordMemoryContextLoaded(AgentContext context) {
        // 如果没有记忆上下文，则记录未命中事件。
        if (context.getMemoryContext() == null || context.getMemoryContext().getStats() == null) {
            // 记录记忆未命中。
            traceRecorder.record(context.getRunId(), "MEMORY_CONTEXT_LOADED", "工作流未读取到可用记忆上下文", "用户偏好数量：0，项目记忆命中：false，历史记忆数量：0，会话摘要命中：false");
            // 直接返回。
            return;
        }
        // 读取记忆统计。
        var stats = context.getMemoryContext().getStats();
        // 记录记忆命中详情。
        traceRecorder.record(context.getRunId(), "MEMORY_CONTEXT_LOADED", "工作流已加载记忆上下文", "用户偏好数量：" + stats.getUserPreferenceCount()
                + "，项目记忆命中：" + stats.isProjectMemoryHit()
                + "，历史记忆数量：" + stats.getHistoryMemoryCount()
                + "，会话摘要命中：" + stats.isConversationSummaryHit());
    }

    /**
     * recordVideoUnderstandingContext（记录视频理解上下文）用于把视频摘要注入情况写入 Trace（执行链路追踪）。
     *
     * @param context 智能体上下文
     */
    private void recordVideoUnderstandingContext(AgentContext context) {
        // 读取视频理解创作上下文。
        Object artifact = context.getArtifact("VIDEO_UNDERSTANDING_CREATION_CONTEXT");
        // 如果没有视频理解上下文，则说明本次没有显式选择视频素材。
        if (!(artifact instanceof VideoUnderstandingCreationContext videoContext)) {
            // 记录跳过事件。
            traceRecorder.record(context.getRunId(), "VIDEO_UNDERSTANDING_SUMMARY_READ_SKIPPED", "本次创作没有读取视频理解摘要", "原因：未显式选择 VIDEO（视频）素材或视频摘要读取服务不可用");
            // 直接返回。
            return;
        }
        // 记录读取完成事件。
        traceRecorder.record(context.getRunId(), "VIDEO_UNDERSTANDING_SUMMARY_READ_FINISHED", "工作流已读取视频理解摘要", "视频素材数量：" + safeSize(videoContext.getVideoMaterialIds()) + "，可用摘要数量：" + safeSize(videoContext.getSummaries()));
        // 如果有警告，则逐条记录缺失或不可用原因。
        if (videoContext.getWarnings() != null && !videoContext.getWarnings().isEmpty()) {
            // 遍历警告。
            for (String warning : videoContext.getWarnings()) {
                // 记录视频摘要缺失事件。
                traceRecorder.record(context.getRunId(), "VIDEO_UNDERSTANDING_SUMMARY_MISSING", "视频理解摘要存在缺失或不可用", warning);
            }
        }
        // 如果存在可用摘要，则记录注入事件。
        if (videoContext.hasAvailableSummary()) {
            // 记录摘要注入事件。
            traceRecorder.record(context.getRunId(), "VIDEO_UNDERSTANDING_SUMMARY_INJECTED", "视频理解摘要已注入创作上下文", "摘要长度：" + videoContext.getSummaryText().length());
            // 直接返回。
            return;
        }
        // 没有可用摘要时记录缺失事件。
        traceRecorder.record(context.getRunId(), "VIDEO_UNDERSTANDING_SUMMARY_MISSING", "没有可注入的视频理解摘要", "创作 Workflow（工作流）将继续使用文本、图片、RAG（检索增强生成）和 Memory（记忆）上下文。");
    }

    /**
     * resolveKnowledgeBaseIds（解析知识库编号列表）用于从上下文中读取本次创作任务要使用的知识库。
     *
     * @param context 智能体上下文
     * @return 知识库编号列表
     */
    private List<String> resolveKnowledgeBaseIds(AgentContext context) {
        // 读取上下文中的知识库编号。
        Object artifact = context.getArtifact("KNOWLEDGE_BASE_IDS");
        // 如果是字符串集合，则直接转换。
        if (artifact instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(value -> value != null && !value.isBlank())
                    .toList();
        }
        // 如果只传入单个字符串，则包装为单元素列表。
        if (artifact instanceof String value && !value.isBlank()) {
            return List.of(value);
        }
        // 默认返回空列表，后续可能走全库检索或静态降级。
        return List.of();
    }

    /**
     * resolveSearchAllKnowledgeBases（解析全库检索标志）用于从上下文读取是否放开知识库过滤。
     *
     * @param context 智能体上下文
     * @return true 表示全库检索
     */
    private boolean resolveSearchAllKnowledgeBases(AgentContext context) {
        // 读取上下文中的全库检索标志。
        Object artifact = context.getArtifact("SEARCH_ALL_KNOWLEDGE_BASES");
        // 布尔值直接返回。
        if (artifact instanceof Boolean value) {
            return value;
        }
        // 字符串形式兼容配置或接口调试场景。
        if (artifact instanceof String value) {
            return Boolean.parseBoolean(value);
        }
        // 默认不启用全库检索。
        return false;
    }

    /**
     * understandMaterials（理解素材）用于从上下文读取素材列表并调用素材理解智能体。
     *
     * @param context 智能体上下文
     * @return 素材理解结果
     */
    @SuppressWarnings("unchecked")
    private MaterialUnderstandingResult understandMaterials(AgentContext context) {
        // 如果上游已经完成素材理解，则直接复用结果，避免真实 VLM（视觉语言模型）重复消耗 Token（词元）。
        Object existingResult = context.getArtifact("MATERIAL_UNDERSTANDING_RESULT");
        // 如果已有素材理解结果，则直接返回。
        if (existingResult instanceof MaterialUnderstandingResult materialUnderstandingResult) {
            // 记录素材理解复用事件。
            traceRecorder.record(context.getRunId(), "WORKFLOW_MATERIAL_SUMMARY_REUSED", "工作流复用已有素材摘要", "图片素材数量：" + materialUnderstandingResult.getImageMaterialCount());
            // 返回已有素材理解结果。
            return materialUnderstandingResult;
        }
        // 从上下文读取素材列表，当前约定键为 MATERIALS（素材列表）。
        Object materialsArtifact = context.getArtifact("MATERIALS");
        // 如果调用方没有提前放入素材列表，则使用空列表，保证纯文本创作链路仍然可运行。
        if (!(materialsArtifact instanceof List<?> materials)) {
            // 调用素材理解智能体生成空素材摘要。
            return materialUnderstandingAgent.understand(context.getRunId(), List.of(), buildMaterialUnderstandingInstruction(context));
        }
        // 过滤并转换为素材响应列表。
        List<MaterialResponse> materialResponses = materials.stream()
                // 只保留 MaterialResponse（素材响应）对象。
                .filter(MaterialResponse.class::isInstance)
                // 转换为 MaterialResponse（素材响应）。
                .map(MaterialResponse.class::cast)
                // 收集为列表。
                .toList();
        // 调用素材理解智能体。
        return materialUnderstandingAgent.understand(context.getRunId(), materialResponses, buildMaterialUnderstandingInstruction(context));
    }

    /**
     * buildMaterialUnderstandingInstruction（构建素材理解指令）用于让图片理解围绕当前创作目标和平台展开。
     *
     * @param context 智能体上下文
     * @return 素材理解指令
     */
    private String buildMaterialUnderstandingInstruction(AgentContext context) {
        // 拼接当前创作任务需要的图片理解重点。
        return "请从短视频创作规划角度理解这些图片素材。创作目标：" + context.getGoal()
                + "；目标平台：" + context.getPlatform()
                + "；风格偏好：" + (context.getStylePreference() == null || context.getStylePreference().isBlank() ? "用户未提供明确风格偏好" : context.getStylePreference())
                + "。请重点识别图片主体、场景、视觉风格、关键画面元素、可用于分镜的位置和风险提示。";
    }

    /**
     * reviseOnceIfNeeded（按需修正一次）用于在评估后执行有限 Reflection（反思修正）。
     *
     * @param context 智能体上下文
     * @param evaluatedResult 已评估创作方案
     * @return 最终创作方案
     */
    private CreationPlanResult reviseOnceIfNeeded(AgentContext context, CreationPlanResult evaluatedResult) {
        // 从上下文读取结构化评估结果。
        CreationPlanEvaluationResult evaluationResult = (CreationPlanEvaluationResult) context.getArtifact("CREATION_PLAN_EVALUATION_RESULT");
        // 读取当前方案版本编号。
        String sourceVersionId = artifactString(context, "CURRENT_CREATION_PLAN_VERSION_ID");
        // 读取当前评估结果编号。
        String evaluationId = artifactString(context, "CURRENT_CREATION_EVALUATION_ID");
        // 读取当前修正次数。
        int currentRevisionTimes = currentRevisionTimes(context);
        // 如果策略判断不需要修正，则记录跳过事件并返回原方案。
        if (!creationPlanRevisionPolicy.shouldRevise(evaluationResult, currentRevisionTimes)) {
            // 记录跳过修正事件。
            traceRecorder.record(context.getRunId(), "REVISION_SKIPPED", "创作方案无需修正", "评分：" + evaluatedResult.getScore() + "，已修正次数：" + currentRevisionTimes);
            // 标记当前版本为最终版本。
            creationQualityRecorder.markFinalVersion(sourceVersionId);
            // 返回原评估结果。
            return evaluatedResult;
        }
        // 写入新的修正次数，确保工作流最多修正一次。
        context.putArtifact("CREATION_PLAN_REVISION_COUNT", currentRevisionTimes + 1);
        // 调用修正生成器生成修正方案。
        CreationPlanResult revisedResult = creationPlanRevisionGenerator.revise(context, evaluatedResult, evaluationResult);
        // 对修正后的方案再次评估，但不再次进入修正循环。
        CreationPlanResult evaluatedRevisedResult = creationPlanEvaluator.evaluate(context, revisedResult);
        // 保存修正版方案版本。
        String targetVersionId = creationQualityRecorder.recordRevisedVersion(context, evaluatedRevisedResult, sourceVersionId);
        // 保存触发修正的前后版本关系。
        String revisionId = creationQualityRecorder.recordRevision(context, sourceVersionId, targetVersionId, evaluationId, evaluationResult);
        // 保存修正版对应的复评结果。
        CreationPlanEvaluationResult revisedEvaluationResult = (CreationPlanEvaluationResult) context.getArtifact("CREATION_PLAN_EVALUATION_RESULT");
        String revisedEvaluationId = creationQualityRecorder.recordEvaluation(context, targetVersionId, revisedEvaluationResult);
        // 更新上下文中的当前版本、评估和修正编号。
        context.putArtifact("CURRENT_CREATION_PLAN_VERSION_ID", targetVersionId);
        context.putArtifact("CURRENT_CREATION_EVALUATION_ID", revisedEvaluationId);
        context.putArtifact("CURRENT_CREATION_REVISION_ID", revisionId);
        // 标记修正版为最终版本。
        creationQualityRecorder.markFinalVersion(targetVersionId);
        // 把修正后再次评估的结果写入上下文。
        context.putArtifact("REVISED_EVALUATED_PLAN_RESULT", evaluatedRevisedResult);
        // 返回修正后评估结果。
        return evaluatedRevisedResult;
    }

    /**
     * recordInitialQualitySnapshot（记录初始质量快照）用于保存初始方案版本和对应评估结果。
     *
     * @param context 智能体上下文
     * @param evaluatedResult 已评估创作方案
     */
    private void recordInitialQualitySnapshot(AgentContext context, CreationPlanResult evaluatedResult) {
        // 保存初始创作方案版本。
        String versionId = creationQualityRecorder.recordInitialVersion(context, evaluatedResult);
        // 从上下文读取结构化评估结果。
        CreationPlanEvaluationResult evaluationResult = (CreationPlanEvaluationResult) context.getArtifact("CREATION_PLAN_EVALUATION_RESULT");
        // 保存评估结果。
        String evaluationId = creationQualityRecorder.recordEvaluation(context, versionId, evaluationResult);
        // 写入当前版本编号，后续修正节点需要读取。
        context.putArtifact("CURRENT_CREATION_PLAN_VERSION_ID", versionId);
        // 写入当前评估编号，后续修正关系需要关联。
        context.putArtifact("CURRENT_CREATION_EVALUATION_ID", evaluationId);
    }

    /**
     * artifactString（中间产物转字符串）用于安全读取上下文中的版本和评估编号。
     *
     * @param context 智能体上下文
     * @param key 中间产物键
     * @return 字符串值
     */
    private String artifactString(AgentContext context, String key) {
        // 读取上下文中间产物。
        Object value = context.getArtifact(key);
        // 空值返回 null。
        if (value == null) {
            return null;
        }
        // 转为字符串。
        return String.valueOf(value);
    }

    /**
     * currentRevisionTimes（当前修正次数）用于从上下文读取已经执行过的修正次数。
     *
     * @param context 智能体上下文
     * @return 当前修正次数
     */
    private int currentRevisionTimes(AgentContext context) {
        // 读取上下文中的修正次数。
        Object value = context.getArtifact("CREATION_PLAN_REVISION_COUNT");
        // 如果是数字，则返回数字值。
        if (value instanceof Number number) {
            // 返回整数值。
            return number.intValue();
        }
        // 默认尚未修正。
        return 0;
    }

    /**
     * safeSize（安全数量）用于避免列表为空时出现空指针。
     *
     * @param values 列表
     * @return 列表数量
     */
    private int safeSize(List<?> values) {
        // 空列表按 0 处理。
        return values == null ? 0 : values.size();
    }

    /**
     * GenerationResult（生成结果）用于同时承载旧接口兼容结果和真实通用产物。
     *
     * @param legacyResult 旧接口兼容结果
     * @param outputPayload 真实通用产物
     * @param outputType 产物类型
     */
    private record GenerationResult(CreationPlanResult legacyResult, Object outputPayload, CreationOutputType outputType) {
    }
}
