package com.clipforge.application;

import com.clipforge.agent.AgentRunResult;
import com.clipforge.agent.ClipForgeAgentRuntime;
import com.clipforge.agent.intent.ConversationIntentAgent;
import com.clipforge.agent.intent.ConversationIntentDecision;
import com.clipforge.api.dto.MaterialResponse;
import com.clipforge.application.persistence.CreationRunPersistenceService;
import com.clipforge.api.dto.CreatePlanRequest;
import com.clipforge.api.dto.CreatePlanResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.memory.model.MemoryContext;
import com.clipforge.memory.extract.MemoryExtractionTriggerType;
import com.clipforge.memory.read.MemoryReadRequest;
import com.clipforge.memory.write.MemoryWriteRequest;
import com.clipforge.rag.selection.KnowledgeBaseSelectionRequest;
import com.clipforge.rag.selection.KnowledgeBaseSelectionResult;
import com.clipforge.rag.selection.KnowledgeBaseSelector;
import com.clipforge.video.understanding.VideoUnderstandingCreationContext;
import com.clipforge.video.understanding.VideoUnderstandingReadService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * CreationApplicationService（创作应用服务）。
 *
 * <p>该服务位于 Application（应用编排层），负责把接口请求转换为 Agent Runtime（智能体运行时）调用。
 */
@Service
public class CreationApplicationService {

    /** clipForgeAgentRuntime（创作规划智能体运行时）负责真正执行智能体链路。 */
    private final ClipForgeAgentRuntime clipForgeAgentRuntime;

    /** creationRunPersistenceService（创作运行持久化服务）负责保存任务、结果和执行链路。 */
    private final CreationRunPersistenceService creationRunPersistenceService;

    /** projectConversationApplicationService（项目会话应用服务）负责校验会话并保存会话消息。 */
    private final ProjectConversationApplicationService projectConversationApplicationService;

    /** conversationIntentAgent（会话意图智能体）用于预留多轮会话路由入口。 */
    private final ConversationIntentAgent conversationIntentAgent;

    /** materialApplicationServiceProvider（素材应用服务提供器）用于在图片素材阶段按需查询素材列表。 */
    private final ObjectProvider<MaterialApplicationService> materialApplicationServiceProvider;

    /** memoryApplicationServiceProvider（记忆应用服务提供器）用于在 db（数据库）环境下读取 MemoryContext（记忆上下文）。 */
    private final ObjectProvider<MemoryApplicationService> memoryApplicationServiceProvider;

    /** videoUnderstandingReadServiceProvider（视频理解读取服务提供器）用于读取已经保存的视频理解摘要。 */
    private final ObjectProvider<VideoUnderstandingReadService> videoUnderstandingReadServiceProvider;

    /** knowledgeBaseSelectorProvider（知识库选择器提供器）用于自动选择本次 RAG（检索增强生成）检索范围。 */
    private final ObjectProvider<KnowledgeBaseSelector> knowledgeBaseSelectorProvider;

    /** conversationSummaryApplicationServiceProvider（会话摘要应用服务提供器）用于创作前刷新会话滚动摘要。 */
    private final ObjectProvider<ConversationSummaryApplicationService> conversationSummaryApplicationServiceProvider;

    /**
     * 构造函数用于兼容早期单元测试，未传入素材应用服务提供器时会跳过素材自动查询。
     *
     * @param clipForgeAgentRuntime 创作规划智能体运行时
     * @param creationRunPersistenceService 创作运行持久化服务
     * @param projectConversationApplicationService 项目会话应用服务
     * @param conversationIntentAgent 会话意图智能体
     */
    public CreationApplicationService(ClipForgeAgentRuntime clipForgeAgentRuntime, CreationRunPersistenceService creationRunPersistenceService, ProjectConversationApplicationService projectConversationApplicationService, ConversationIntentAgent conversationIntentAgent) {
        // 调用完整构造函数，素材服务提供器为空，表示测试环境不自动查询素材。
        this(clipForgeAgentRuntime, creationRunPersistenceService, projectConversationApplicationService, conversationIntentAgent, null, null, null, null, null);
    }

    /**
     * 构造函数用于兼容 Phase 6（阶段 6）后已有的素材自动查询测试。
     *
     * @param clipForgeAgentRuntime 创作规划智能体运行时
     * @param creationRunPersistenceService 创作运行持久化服务
     * @param projectConversationApplicationService 项目会话应用服务
     * @param conversationIntentAgent 会话意图智能体
     * @param materialApplicationServiceProvider 素材应用服务提供器
     */
    public CreationApplicationService(ClipForgeAgentRuntime clipForgeAgentRuntime, CreationRunPersistenceService creationRunPersistenceService, ProjectConversationApplicationService projectConversationApplicationService, ConversationIntentAgent conversationIntentAgent, ObjectProvider<MaterialApplicationService> materialApplicationServiceProvider) {
        // 调用完整构造函数，记忆服务提供器为空，表示测试环境不自动读取记忆。
        this(clipForgeAgentRuntime, creationRunPersistenceService, projectConversationApplicationService, conversationIntentAgent, materialApplicationServiceProvider, null, null, null, null);
    }

    /**
     * 构造函数用于兼容 Phase 8（阶段 8）已有的记忆读取测试。
     *
     * @param clipForgeAgentRuntime 创作规划智能体运行时
     * @param creationRunPersistenceService 创作运行持久化服务
     * @param projectConversationApplicationService 项目会话应用服务
     * @param conversationIntentAgent 会话意图智能体
     * @param materialApplicationServiceProvider 素材应用服务提供器
     * @param memoryApplicationServiceProvider 记忆应用服务提供器
     */
    public CreationApplicationService(ClipForgeAgentRuntime clipForgeAgentRuntime, CreationRunPersistenceService creationRunPersistenceService, ProjectConversationApplicationService projectConversationApplicationService, ConversationIntentAgent conversationIntentAgent, ObjectProvider<MaterialApplicationService> materialApplicationServiceProvider, ObjectProvider<MemoryApplicationService> memoryApplicationServiceProvider) {
        // 调用完整构造函数，视频理解读取服务提供器为空，表示测试环境不自动读取视频摘要。
        this(clipForgeAgentRuntime, creationRunPersistenceService, projectConversationApplicationService, conversationIntentAgent, materialApplicationServiceProvider, memoryApplicationServiceProvider, null, null, null);
    }

    /**
     * 构造函数用于 Spring（Spring 框架）运行时注入全部依赖。
     *
     * @param clipForgeAgentRuntime 创作规划智能体运行时
     * @param creationRunPersistenceService 创作运行持久化服务
     * @param projectConversationApplicationService 项目会话应用服务
     * @param conversationIntentAgent 会话意图智能体
     * @param materialApplicationServiceProvider 素材应用服务提供器
     * @param memoryApplicationServiceProvider 记忆应用服务提供器
     * @param videoUnderstandingReadServiceProvider 视频理解读取服务提供器
     */
    /**
     * 构造函数用于兼容 Phase 9（阶段 9）已有的视频理解读取测试，未传入知识库选择器时只保留显式知识库兼容逻辑。
     *
     * @param clipForgeAgentRuntime 创作规划智能体运行时
     * @param creationRunPersistenceService 创作运行持久化服务
     * @param projectConversationApplicationService 项目会话应用服务
     * @param conversationIntentAgent 会话意图智能体
     * @param materialApplicationServiceProvider 素材应用服务提供器
     * @param memoryApplicationServiceProvider 记忆应用服务提供器
     * @param videoUnderstandingReadServiceProvider 视频理解读取服务提供器
     */
    public CreationApplicationService(ClipForgeAgentRuntime clipForgeAgentRuntime,
                                      CreationRunPersistenceService creationRunPersistenceService,
                                      ProjectConversationApplicationService projectConversationApplicationService,
                                      ConversationIntentAgent conversationIntentAgent,
                                      ObjectProvider<MaterialApplicationService> materialApplicationServiceProvider,
                                      ObjectProvider<MemoryApplicationService> memoryApplicationServiceProvider,
                                      ObjectProvider<VideoUnderstandingReadService> videoUnderstandingReadServiceProvider) {
        // 转发到完整构造函数，知识库选择器为空表示兼容旧测试构造路径。
        this(clipForgeAgentRuntime, creationRunPersistenceService, projectConversationApplicationService, conversationIntentAgent, materialApplicationServiceProvider, memoryApplicationServiceProvider, videoUnderstandingReadServiceProvider, null, null);
    }

    /**
     * 构造函数用于兼容优化项 9（项目默认知识库）之后已有的知识库选择器测试。
     *
     * @param clipForgeAgentRuntime 创作规划智能体运行时
     * @param creationRunPersistenceService 创作运行持久化服务
     * @param projectConversationApplicationService 项目会话应用服务
     * @param conversationIntentAgent 会话意图智能体
     * @param materialApplicationServiceProvider 素材应用服务提供器
     * @param memoryApplicationServiceProvider 记忆应用服务提供器
     * @param videoUnderstandingReadServiceProvider 视频理解读取服务提供器
     * @param knowledgeBaseSelectorProvider 知识库选择器提供器
     */
    public CreationApplicationService(ClipForgeAgentRuntime clipForgeAgentRuntime,
                                      CreationRunPersistenceService creationRunPersistenceService,
                                      ProjectConversationApplicationService projectConversationApplicationService,
                                      ConversationIntentAgent conversationIntentAgent,
                                      ObjectProvider<MaterialApplicationService> materialApplicationServiceProvider,
                                      ObjectProvider<MemoryApplicationService> memoryApplicationServiceProvider,
                                      ObjectProvider<VideoUnderstandingReadService> videoUnderstandingReadServiceProvider,
                                      ObjectProvider<KnowledgeBaseSelector> knowledgeBaseSelectorProvider) {
        // 转发到完整构造函数，会话摘要服务为空表示兼容旧测试构造路径。
        this(clipForgeAgentRuntime, creationRunPersistenceService, projectConversationApplicationService, conversationIntentAgent, materialApplicationServiceProvider, memoryApplicationServiceProvider, videoUnderstandingReadServiceProvider, knowledgeBaseSelectorProvider, null);
    }

    @Autowired
    public CreationApplicationService(ClipForgeAgentRuntime clipForgeAgentRuntime,
                                      CreationRunPersistenceService creationRunPersistenceService,
                                      ProjectConversationApplicationService projectConversationApplicationService,
                                      ConversationIntentAgent conversationIntentAgent,
                                      ObjectProvider<MaterialApplicationService> materialApplicationServiceProvider,
                                      ObjectProvider<MemoryApplicationService> memoryApplicationServiceProvider,
                                      ObjectProvider<VideoUnderstandingReadService> videoUnderstandingReadServiceProvider,
                                      ObjectProvider<KnowledgeBaseSelector> knowledgeBaseSelectorProvider,
                                      ObjectProvider<ConversationSummaryApplicationService> conversationSummaryApplicationServiceProvider) {
        // 保存创作规划智能体运行时。
        this.clipForgeAgentRuntime = clipForgeAgentRuntime;
        // 保存创作运行持久化服务。
        this.creationRunPersistenceService = creationRunPersistenceService;
        // 保存项目会话应用服务。
        this.projectConversationApplicationService = projectConversationApplicationService;
        // 保存会话意图智能体。
        this.conversationIntentAgent = conversationIntentAgent;
        // 保存素材应用服务提供器。
        this.materialApplicationServiceProvider = materialApplicationServiceProvider;
        // 保存记忆应用服务提供器。
        this.memoryApplicationServiceProvider = memoryApplicationServiceProvider;
        // 保存视频理解读取服务提供器。
        this.videoUnderstandingReadServiceProvider = videoUnderstandingReadServiceProvider;
        // 保存知识库选择器提供器。
        this.knowledgeBaseSelectorProvider = knowledgeBaseSelectorProvider;
        // 保存会话摘要应用服务提供器。
        this.conversationSummaryApplicationServiceProvider = conversationSummaryApplicationServiceProvider;
    }

    /**
     * createPlan（创建创作方案）用于执行一次完整的最小创作智能体流程。
     *
     * @param request 创建方案请求
     * @return 创建方案响应
     */
    public CreatePlanResponse createPlan(CreatePlanRequest request) {
        // 在应用层生成运行编号，方便任务表、结果表和执行链路表使用同一个 runId（运行编号）串联。
        String runId = UUID.randomUUID().toString();
        // 创建任务记录并标记为 RUNNING（执行中）；默认无数据库环境下是空实现。
        String taskId = creationRunPersistenceService.createRunningTask(runId, request.getGoal(), request.getPlatform(), request.getStylePreference());
        // 声明智能体运行结果变量，便于后续保存和返回。
        AgentRunResult runResult;
        // 声明会话归属解析结果，便于响应返回自动补齐后的项目和会话编号。
        ConversationResolution conversationResolution;
        try {
            // 识别会话意图，当前阶段只用于写入元数据和预留后续路由入口。
            ConversationIntentDecision intentDecision = conversationIntentAgent.recognize(request);
            // 解析项目和会话归属，必要时自动创建默认项目或会话。
            conversationResolution = projectConversationApplicationService.resolveCreationConversation(request);
            // 如果测试或兜底实现返回空，则使用无会话归属，避免空指针。
            if (conversationResolution == null) {
                // 构造空会话归属。
                conversationResolution = ConversationResolution.builder().build();
            }
            // 如果已经有会话归属，则在会话中追加用户消息。
            appendUserMessageIfNeeded(conversationResolution.getConversationId(), runId, request);
            // 构造 Agent（智能体）运行元数据，供模型用量记录器绑定项目和会话。
            Map<String, Object> metadata = buildRuntimeMetadata(runId, request, conversationResolution, intentDecision);
            // 写入任务编号，供质量版本化记录关联 cf_creation_task（创作任务表）。
            metadata.put("TASK_ID", taskId);
            // 调用 Agent Runtime（智能体运行时）执行创作规划任务，并使用应用层提前生成的运行编号。
            runResult = clipForgeAgentRuntime.runWithRunId(runId, request.getGoal(), request.getPlatform(), request.getStylePreference(), metadata);
            // 如果已经有会话归属，则追加助手消息摘要。
            appendAssistantMessageIfNeeded(conversationResolution.getConversationId(), runId, runResult);
            // 保存成功运行产生的创作结果和 Trace（执行链路追踪）；默认无数据库环境下是空实现。
            creationRunPersistenceService.saveSucceededRun(taskId, runResult);
            // 创作成功后沉淀 Memory（记忆），供同用户或同项目后续创作读取；该副作用失败时不影响本次成功结果返回。
            writeMemoryAfterSuccess(runId, request, conversationResolution, metadata, runResult);
        } catch (RuntimeException exception) {
            // 保存失败任务状态；默认无数据库环境下是空实现。
            creationRunPersistenceService.saveFailedRun(runId, exception);
            // 继续抛出原异常，让全局异常处理器返回统一错误响应。
            throw exception;
        }

        // 使用 Builder（建造者模式）组装接口响应，避免连续 setter（赋值方法）造成样板代码。
        return CreatePlanResponse.builder()
                // 设置本次 Agent（智能体）运行编号。
                .runId(runResult.getRunId())
                // 设置项目编号。
                .projectId(conversationResolution.getProjectId())
                // 设置会话编号。
                .conversationId(conversationResolution.getConversationId())
                // 设置本次 AgentPlan（智能体计划）编号。
                .planId(runResult.getPlan().getPlanId())
                // 设置最终创作方案结果。
                .result(runResult.getResult())
                // 设置执行链路事件列表。
                .traceEvents(runResult.getTraceEvents())
                // build（构建）生成不可再追加字段的响应对象。
                .build();
    }

    /**
     * writeMemoryAfterSuccess（成功后写入记忆）用于把本次创作沉淀为未来可复用的用户偏好、项目画像和高分历史经验。
     *
     * @param runId 运行编号
     * @param request 创建方案请求
     * @param conversationResolution 会话归属解析结果
     * @param metadata 运行元数据
     * @param runResult 智能体运行结果
     */
    private void writeMemoryAfterSuccess(String runId, CreatePlanRequest request, ConversationResolution conversationResolution, Map<String, Object> metadata, AgentRunResult runResult) {
        // 如果没有记忆应用服务提供器，说明当前处于早期单元测试构造路径，直接跳过。
        if (memoryApplicationServiceProvider == null) {
            // 直接返回。
            return;
        }
        // 从 Spring（Spring 框架）容器中获取记忆应用服务。
        MemoryApplicationService memoryApplicationService = memoryApplicationServiceProvider.getIfAvailable();
        // 如果未启用 db（数据库）Profile（配置环境）或未注入记忆服务，则跳过。
        if (memoryApplicationService == null) {
            // 直接返回。
            return;
        }
        try {
            // 组装记忆写入请求。
            MemoryWriteRequest memoryWriteRequest = MemoryWriteRequest.builder()
                    // 设置运行编号。
                    .runId(runResult != null && hasText(runResult.getRunId()) ? runResult.getRunId() : runId)
                    // 设置用户编号。
                    .userId(request == null ? null : request.getUserId())
                    // 设置项目编号。
                    .projectId(conversationResolution == null ? null : conversationResolution.getProjectId())
                    // 设置会话编号。
                    .conversationId(conversationResolution == null ? null : conversationResolution.getConversationId())
                    // 设置目标平台。
                    .platform(request == null ? null : request.getPlatform())
                    // 设置用户输入。
                    .userInput(request == null ? null : request.getGoal())
                    // 设置风格偏好。
                    .stylePreference(request == null ? null : request.getStylePreference())
                    // 设置最终创作方案。
                    .finalResult(runResult == null ? null : runResult.getResult())
                    // 设置本次创作前读取到的记忆上下文。
                    .existingMemoryContext(metadata == null ? null : (MemoryContext) metadata.get("MEMORY_CONTEXT"))
                    // 标记为创作成功后的记忆沉淀，便于 LLM 抽取触发策略判断是否调用模型。
                    .triggerType(MemoryExtractionTriggerType.CREATION_SUCCESS)
                    // 记录触发原因，方便后续复盘为什么尝试写入记忆。
                    .triggerReason("CREATION_SUCCESS_AFTER_WORKFLOW")
                    // 构建请求。
                    .build();
            // 调用记忆应用服务执行写入。
            memoryApplicationService.writeMemory(memoryWriteRequest);
        } catch (RuntimeException exception) {
            // 记忆写入属于成功创作后的非关键副作用，失败时不打断用户拿到本次创作结果。
        }
    }

    /**
     * appendUserMessageIfNeeded（按需追加用户消息）用于在已有会话归属时保存用户输入。
     *
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @param request 创建方案请求
     */
    private void appendUserMessageIfNeeded(String conversationId, String runId, CreatePlanRequest request) {
        // 如果没有会话编号，则保持早期无会话模式，不保存用户消息。
        if (!hasText(conversationId)) {
            // 直接返回。
            return;
        }
        // 追加用户消息。
        projectConversationApplicationService.appendMessage(conversationId, runId, "user", request.getGoal());
    }

    /**
     * buildRuntimeMetadata（构建运行元数据）用于把项目、会话和用户信息传入 AgentContext（智能体上下文）。
     *
     * @param runId 运行编号
     * @param request 创建方案请求
     * @param conversationResolution 会话归属解析结果
     * @param intentDecision 会话意图决策
     * @return 运行元数据
     */
    private Map<String, Object> buildRuntimeMetadata(String runId, CreatePlanRequest request, ConversationResolution conversationResolution, ConversationIntentDecision intentDecision) {
        // 创建有序 Map（映射），方便后续调试观察。
        Map<String, Object> metadata = new LinkedHashMap<>();
        // 如果项目编号不为空，则写入元数据。
        if (conversationResolution != null && hasText(conversationResolution.getProjectId())) {
            // 写入项目编号。
            metadata.put("PROJECT_ID", conversationResolution.getProjectId());
        }
        // 如果会话编号不为空，则写入元数据。
        if (conversationResolution != null && hasText(conversationResolution.getConversationId())) {
            // 写入会话编号。
            metadata.put("CONVERSATION_ID", conversationResolution.getConversationId());
        }
        // 如果用户编号不为空，则写入元数据。
        if (hasText(request.getUserId())) {
            // 写入用户编号。
            metadata.put("USER_ID", request.getUserId());
        }
        // 如果意图决策不为空，则写入会话意图类型。
        if (intentDecision != null && intentDecision.getIntentType() != null) {
            // 写入会话意图类型。
            metadata.put("CONVERSATION_INTENT", intentDecision.getIntentType().name());
            // 写入会话意图置信度。
            metadata.put("CONVERSATION_INTENT_CONFIDENCE", intentDecision.getConfidence());
        }
        // 写入本次创作显式选择的知识库编号，供 FixedCreationWorkflow（固定创作工作流）执行真实 RAG（检索增强生成）。
        attachKnowledgeBaseIds(metadata, request);
        // 查询并写入当前项目或会话下的图片素材列表，供 FixedCreationWorkflow（固定创作工作流）执行素材理解。
        attachImageMaterials(metadata, request, conversationResolution);
        // 查询并写入用户明确选择的视频理解摘要，供 FixedCreationWorkflow（固定创作工作流）注入 Prompt（提示词）。
        attachVideoUnderstandingContext(metadata, request);
        // 在读取 MemoryContext（记忆上下文）前刷新会话滚动摘要，保证本轮 Prompt（提示词）使用最新短期记忆。
        refreshConversationSummaryBeforeMemory(metadata, runId, request, conversationResolution);
        // 查询并写入 MemoryContext（记忆上下文），供后续 Planner（规划器）、Generator（生成器）和 Prompt（提示词）渲染器使用。
        attachMemoryContext(metadata, runId, request, conversationResolution);
        // 返回元数据。
        return metadata;
    }

    /**
     * refreshConversationSummaryBeforeMemory（读取记忆前刷新会话摘要）用于把长会话旧消息压缩为滚动摘要。
     *
     * @param metadata 运行元数据
     * @param runId 运行编号
     * @param request 创建方案请求
     * @param conversationResolution 会话归属解析结果
     */
    private void refreshConversationSummaryBeforeMemory(Map<String, Object> metadata, String runId, CreatePlanRequest request, ConversationResolution conversationResolution) {
        // 如果没有会话摘要服务提供器，说明处于旧测试构造路径，直接跳过。
        if (conversationSummaryApplicationServiceProvider == null) {
            return;
        }
        // 如果没有会话编号，则无需处理短期记忆摘要。
        if (conversationResolution == null || !hasText(conversationResolution.getConversationId())) {
            return;
        }
        // 从 Spring（Spring 框架）容器中获取会话摘要应用服务。
        ConversationSummaryApplicationService summaryApplicationService = conversationSummaryApplicationServiceProvider.getIfAvailable();
        // 如果服务不可用，说明当前环境未启用相关 Bean（组件），直接跳过。
        if (summaryApplicationService == null) {
            return;
        }
        try {
            // 触发会话滚动摘要刷新；内部会根据阈值判断是否真的调用 LLM（大语言模型）。
            Object decision = summaryApplicationService.refreshBeforeCreation(
                    runId,
                    request == null ? null : request.getUserId(),
                    conversationResolution.getProjectId(),
                    conversationResolution.getConversationId());
            // 写入触发决策，方便测试和后续接口排查。
            metadata.put("CONVERSATION_SUMMARY_TRIGGER_DECISION", decision);
        } catch (BusinessException exception) {
            // 未启用数据库时保持早期纯文本链路可用。
            if (exception.getErrorCode() == ErrorCode.PERSISTENCE_DISABLED) {
                return;
            }
            // 其他业务错误继续抛出，避免摘要链路的数据问题被吞掉。
            throw exception;
        }
    }

    /**
     * attachKnowledgeBaseIds（附加知识库编号列表）用于自动选择本次创作要使用的知识库范围。
     *
     * @param metadata 运行元数据
     * @param request 创建方案请求
     */
    private void attachKnowledgeBaseIds(Map<String, Object> metadata, CreatePlanRequest request) {
        // 如果知识库选择器不存在，说明处于早期单元测试构造路径，此时只保留显式知识库兼容逻辑。
        if (knowledgeBaseSelectorProvider == null) {
            // 附加显式知识库。
            attachExplicitKnowledgeBaseIds(metadata, request);
            // 直接返回。
            return;
        }
        // 从 Spring（Spring 框架）容器中获取知识库选择器。
        KnowledgeBaseSelector knowledgeBaseSelector = knowledgeBaseSelectorProvider.getIfAvailable();
        // 如果选择器不可用，保留显式知识库兼容逻辑。
        if (knowledgeBaseSelector == null) {
            // 附加显式知识库。
            attachExplicitKnowledgeBaseIds(metadata, request);
            // 直接返回。
            return;
        }
        // 构造知识库选择请求。
        KnowledgeBaseSelectionRequest selectionRequest = KnowledgeBaseSelectionRequest.builder()
                // 传入用户显式选择的知识库编号。
                .explicitKnowledgeBaseIds(request == null ? null : request.getKnowledgeBaseIds())
                // 传入项目编号。
                .projectId(request == null ? null : request.getProjectId())
                // 传入目标平台。
                .platform(request == null ? null : request.getPlatform())
                // 传入创作目标，后续可用于主题匹配。
                .goal(request == null ? null : request.getGoal())
                // 构建请求。
                .build();
        // 执行知识库选择。
        KnowledgeBaseSelectionResult selectionResult = knowledgeBaseSelector.select(selectionRequest);
        // 写入选择结果，方便 Trace（执行链路追踪）和后续前端展示。
        metadata.put("KNOWLEDGE_BASE_SELECTION_RESULT", selectionResult);
        // 如果选择到了具体知识库，则写入知识库编号列表。
        if (selectionResult != null && selectionResult.getKnowledgeBaseIds() != null && !selectionResult.getKnowledgeBaseIds().isEmpty()) {
            // 写入知识库编号列表，FixedCreationWorkflow（固定创作工作流）会读取该键并传给 KnowledgeRetriever（知识检索器）。
            metadata.put("KNOWLEDGE_BASE_IDS", selectionResult.getKnowledgeBaseIds());
        }
        // 如果选择结果要求全库检索，则写入全库检索标志。
        if (selectionResult != null && selectionResult.isSearchAllKnowledgeBases()) {
            // 写入全库检索标志，后续 RAG（检索增强生成）会放开知识库编号过滤。
            metadata.put("SEARCH_ALL_KNOWLEDGE_BASES", true);
        }
    }

    /**
     * attachExplicitKnowledgeBaseIds（附加显式知识库编号）用于兼容没有知识库选择器的测试场景。
     *
     * @param metadata 运行元数据
     * @param request 创建方案请求
     */
    private void attachExplicitKnowledgeBaseIds(Map<String, Object> metadata, CreatePlanRequest request) {
        // 如果请求为空或没有传入知识库编号，则保持为空。
        if (request == null || request.getKnowledgeBaseIds() == null || request.getKnowledgeBaseIds().isEmpty()) {
            // 直接返回。
            return;
        }
        // 清洗空白编号，避免空字符串进入 SQL（结构化查询语言）查询条件。
        List<String> knowledgeBaseIds = request.getKnowledgeBaseIds()
                .stream()
                .filter(this::hasText)
                .distinct()
                .toList();
        // 如果清洗后没有可用编号，则保持为空。
        if (knowledgeBaseIds.isEmpty()) {
            // 直接返回。
            return;
        }
        // 写入知识库编号列表。
        metadata.put("KNOWLEDGE_BASE_IDS", knowledgeBaseIds);
    }

    /**
     * attachMemoryContext（附加记忆上下文）用于把用户偏好、项目记忆、历史摘要和会话摘要写入运行元数据。
     *
     * @param metadata 运行元数据
     * @param runId 运行编号
     * @param request 创建方案请求
     * @param conversationResolution 会话归属解析结果
     */
    private void attachMemoryContext(Map<String, Object> metadata, String runId, CreatePlanRequest request, ConversationResolution conversationResolution) {
        // 如果没有记忆应用服务提供器，说明处于早期单元测试构造路径，直接跳过。
        if (memoryApplicationServiceProvider == null) {
            // 直接返回。
            return;
        }
        // 从 Spring（Spring 框架）容器中获取记忆应用服务。
        MemoryApplicationService memoryApplicationService = memoryApplicationServiceProvider.getIfAvailable();
        // 如果记忆应用服务不可用，说明没有启用 db（数据库）Profile（配置环境），直接跳过。
        if (memoryApplicationService == null) {
            // 直接返回。
            return;
        }
        // 构造记忆读取请求。
        MemoryReadRequest memoryReadRequest = MemoryReadRequest.builder()
                // 设置运行编号。
                .runId(runId)
                // 设置用户编号。
                .userId(request == null ? null : request.getUserId())
                // 设置项目编号。
                .projectId(conversationResolution == null ? null : conversationResolution.getProjectId())
                // 设置会话编号。
                .conversationId(conversationResolution == null ? null : conversationResolution.getConversationId())
                // 设置目标平台。
                .platform(request == null ? null : request.getPlatform())
                // 构建请求。
                .build();
        // 读取记忆上下文。
        MemoryContext memoryContext = memoryApplicationService.readMemoryContext(memoryReadRequest);
        // 如果读取结果为空或没有任何可用记忆，则不写入，避免后续 Prompt（提示词）出现空记忆段落。
        if (memoryContext == null || !memoryContext.hasAnyMemory()) {
            // 直接返回。
            return;
        }
        // 写入 MemoryContext（记忆上下文）供 Agent Runtime（智能体运行时）传入 AgentContext（智能体上下文）。
        metadata.put("MEMORY_CONTEXT", memoryContext);
    }

    /**
     * attachImageMaterials（附加图片素材）用于把当前项目或会话下的图片素材写入运行元数据。
     *
     * @param metadata 运行元数据
     * @param request 创建方案请求
     * @param conversationResolution 会话归属解析结果
     */
    private void attachImageMaterials(Map<String, Object> metadata, CreatePlanRequest request, ConversationResolution conversationResolution) {
        // 如果没有素材应用服务提供器，说明处于早期单元测试构造路径，直接跳过。
        if (materialApplicationServiceProvider == null) {
            // 直接返回。
            return;
        }
        // 如果没有任何用户、项目或会话归属，避免执行无边界素材查询。
        if (!shouldQueryMaterials(request, conversationResolution)) {
            // 直接返回。
            return;
        }
        // 从 Spring（Spring 框架）容器中获取素材应用服务。
        MaterialApplicationService materialApplicationService = materialApplicationServiceProvider.getIfAvailable();
        // 如果素材应用服务不可用，则跳过素材注入。
        if (materialApplicationService == null) {
            // 直接返回。
            return;
        }
        // 如果用户明确选择了素材编号，则优先使用精确素材列表。
        if (hasMaterialIds(request)) {
            // 查询用户明确选择的 IMAGE（图片）素材。
            List<MaterialResponse> selectedMaterials = materialApplicationService.listMaterialsByIds(request.getMaterialIds(), "IMAGE");
            // 如果查询到选中图片素材，则写入运行元数据。
            if (selectedMaterials != null && !selectedMaterials.isEmpty()) {
                // 写入 MATERIALS（素材列表）供工作流读取。
                metadata.put("MATERIALS", selectedMaterials);
            }
            // 精确选择优先级最高，无论是否查到结果，都不再走自动素材查询，避免把未选中的图片混入本次创作。
            return;
        }
        try {
            // 解析素材查询边界，优先使用会话，其次项目，最后用户，避免多个条件 AND（并且）后过度收窄。
            MaterialQueryBoundary boundary = resolveMaterialQueryBoundary(request, conversationResolution);
            // 查询当前归属下的 IMAGE（图片）素材。
            List<MaterialResponse> materials = materialApplicationService.listMaterials(
                    // 使用查询边界中的用户编号。
                    boundary.userId(),
                    // 使用查询边界中的项目编号。
                    boundary.projectId(),
                    // 使用查询边界中的会话编号。
                    boundary.conversationId(),
                    // 新的 runId（运行编号）在素材上传前通常不存在，当前不按运行编号查询。
                    null,
                    // 当前阶段只查询图片素材。
                    "IMAGE");
            // 如果查询到图片素材，则写入元数据。
            if (materials != null && !materials.isEmpty()) {
                // 写入 MATERIALS（素材列表）供工作流读取。
                metadata.put("MATERIALS", materials);
            }
        } catch (BusinessException exception) {
            // 如果未启用 db（数据库）Profile（配置环境），纯文本创作链路继续运行。
            if (exception.getErrorCode() == ErrorCode.PERSISTENCE_DISABLED) {
                // 直接返回。
                return;
            }
            // 其他业务异常继续抛出，避免真实数据问题被吞掉。
            throw exception;
        }
    }

    /**
     * attachVideoUnderstandingContext（附加视频理解上下文）用于读取用户明确选择的视频素材摘要。
     *
     * @param metadata 运行元数据
     * @param request 创建方案请求
     */
    private void attachVideoUnderstandingContext(Map<String, Object> metadata, CreatePlanRequest request) {
        // Phase 9.7（阶段 9.7）只处理用户明确选择的视频素材编号。
        if (!hasMaterialIds(request)) {
            // 没有显式素材编号时不自动扫描视频，避免历史视频污染当前创作。
            return;
        }
        // 如果没有素材应用服务提供器，则跳过。
        if (materialApplicationServiceProvider == null) {
            // 直接返回。
            return;
        }
        // 如果没有视频理解读取服务提供器，则跳过。
        if (videoUnderstandingReadServiceProvider == null) {
            // 直接返回。
            return;
        }
        // 从 Spring（Spring 框架）容器中获取素材应用服务。
        MaterialApplicationService materialApplicationService = materialApplicationServiceProvider.getIfAvailable();
        // 从 Spring（Spring 框架）容器中获取视频理解读取服务。
        VideoUnderstandingReadService videoUnderstandingReadService = videoUnderstandingReadServiceProvider.getIfAvailable();
        // 如果任一服务不可用，则跳过。
        if (materialApplicationService == null || videoUnderstandingReadService == null) {
            // 直接返回。
            return;
        }
        try {
            // 查询用户明确选择的 VIDEO（视频）素材。
            List<MaterialResponse> selectedVideos = materialApplicationService.listMaterialsByIds(request.getMaterialIds(), "VIDEO");
            // 如果没有选中视频素材，则直接返回。
            if (selectedVideos == null || selectedVideos.isEmpty()) {
                // 直接返回。
                return;
            }
            // 写入视频素材列表，便于 Trace（执行链路追踪）和后续复盘。
            metadata.put("VIDEO_MATERIALS", selectedVideos);
            // 提取视频素材编号。
            List<String> videoMaterialIds = selectedVideos.stream()
                    // 读取素材编号。
                    .map(MaterialResponse::getMaterialId)
                    // 过滤空编号。
                    .filter(this::hasText)
                    // 收集为列表。
                    .toList();
            // 读取创作用视频理解上下文。
            VideoUnderstandingCreationContext videoContext = videoUnderstandingReadService.readForCreation(videoMaterialIds);
            // 写入视频理解创作上下文。
            metadata.put("VIDEO_UNDERSTANDING_CREATION_CONTEXT", videoContext);
        } catch (BusinessException exception) {
            // 如果未启用 db（数据库）Profile（配置环境），创作链路继续运行。
            if (exception.getErrorCode() == ErrorCode.PERSISTENCE_DISABLED) {
                // 直接返回。
                return;
            }
            // 其他业务异常继续抛出。
            throw exception;
        }
    }

    /**
     * shouldQueryMaterials（是否查询素材）用于避免没有边界条件时查询全部素材。
     *
     * @param request 创建方案请求
     * @param conversationResolution 会话归属解析结果
     * @return 是否应该查询素材
     */
    private boolean shouldQueryMaterials(CreatePlanRequest request, ConversationResolution conversationResolution) {
        // 如果用户明确选择了素材编号，则需要查询素材。
        if (hasMaterialIds(request)) {
            // 返回需要查询。
            return true;
        }
        // 如果用户编号存在，可以限定在用户范围内查询。
        if (request != null && hasText(request.getUserId())) {
            // 返回需要查询。
            return true;
        }
        // 如果项目编号存在，可以限定在项目范围内查询。
        if (conversationResolution != null && hasText(conversationResolution.getProjectId())) {
            // 返回需要查询。
            return true;
        }
        // 如果会话编号存在，可以限定在会话范围内查询。
        return conversationResolution != null && hasText(conversationResolution.getConversationId());
    }

    /**
     * hasMaterialIds（是否有素材编号列表）用于判断用户是否明确选择了本次创作素材。
     *
     * @param request 创建方案请求
     * @return 是否存在有效素材编号
     */
    private boolean hasMaterialIds(CreatePlanRequest request) {
        // 如果请求或素材编号列表为空，则返回 false（假）。
        if (request == null || request.getMaterialIds() == null || request.getMaterialIds().isEmpty()) {
            // 返回 false（假）。
            return false;
        }
        // 只要存在一个非空白素材编号，就认为用户明确选择了素材。
        return request.getMaterialIds().stream().anyMatch(this::hasText);
    }

    /**
     * resolveMaterialQueryBoundary（解析素材查询边界）用于确定本次创作应该按哪个归属范围查询图片素材。
     *
     * @param request 创建方案请求
     * @param conversationResolution 会话归属解析结果
     * @return 素材查询边界
     */
    private MaterialQueryBoundary resolveMaterialQueryBoundary(CreatePlanRequest request, ConversationResolution conversationResolution) {
        // 如果用户请求中明确携带会话编号，优先查询该会话下的素材。
        if (request != null && hasText(request.getConversationId()) && conversationResolution != null && hasText(conversationResolution.getConversationId())) {
            // 返回会话级查询边界。
            return new MaterialQueryBoundary(null, null, conversationResolution.getConversationId());
        }
        // 如果项目编号存在，其次查询该项目下的素材。
        if (conversationResolution != null && hasText(conversationResolution.getProjectId())) {
            // 返回项目级查询边界。
            return new MaterialQueryBoundary(null, conversationResolution.getProjectId(), null);
        }
        // 最后如果用户编号存在，则查询该用户下的素材。
        if (request != null && hasText(request.getUserId())) {
            // 返回用户级查询边界。
            return new MaterialQueryBoundary(request.getUserId(), null, null);
        }
        // 没有任何边界时返回空边界，调用方会在 shouldQueryMaterials（是否查询素材）中提前跳过。
        return new MaterialQueryBoundary(null, null, null);
    }

    /**
     * MaterialQueryBoundary（素材查询边界）用于承载本次自动素材查询的范围。
     *
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param conversationId 会话编号
     */
    private record MaterialQueryBoundary(String userId, String projectId, String conversationId) {
    }

    /**
     * appendAssistantMessageIfNeeded（按需追加助手消息）用于保存智能体输出摘要。
     *
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @param runResult 智能体运行结果
     */
    private void appendAssistantMessageIfNeeded(String conversationId, String runId, AgentRunResult runResult) {
        // 如果没有会话编号，则不保存助手消息。
        if (!hasText(conversationId)) {
            // 直接返回。
            return;
        }
        // 组装助手消息摘要，避免把完整长结果塞入会话消息表。
        String assistantSummary = "已生成创作方案：主题=" + runResult.getResult().getTheme() + "，评分=" + runResult.getResult().getScore();
        // 追加助手消息。
        projectConversationApplicationService.appendMessage(conversationId, runId, "assistant", assistantSummary);
    }

    /**
     * hasText（是否有文本）用于判断字符串是否不为空。
     *
     * @param value 原始值
     * @return 是否有文本
     */
    private boolean hasText(String value) {
        // 判断字符串不为空引用且去掉空白后仍有内容。
        return value != null && !value.isBlank();
    }
}
