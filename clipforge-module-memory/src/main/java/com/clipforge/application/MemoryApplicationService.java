package com.clipforge.application;

import com.clipforge.contract.knowledge.CreationHistoryMemoryResponse;
import com.clipforge.contract.knowledge.DisableMemoryResponse;
import com.clipforge.contract.knowledge.ProjectMemoryResponse;
import com.clipforge.contract.knowledge.UserPreferenceMemoryResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.memory.infrastructure.persistence.entity.CreationHistoryMemoryEntity;
import com.clipforge.memory.infrastructure.persistence.entity.ProjectMemoryEntity;
import com.clipforge.memory.infrastructure.persistence.entity.UserPreferenceMemoryEntity;
import com.clipforge.memory.infrastructure.persistence.enums.MemoryStatus;
import com.clipforge.memory.infrastructure.persistence.repository.CreationHistoryMemoryRepository;
import com.clipforge.memory.infrastructure.persistence.repository.ProjectMemoryRepository;
import com.clipforge.memory.infrastructure.persistence.repository.UserPreferenceMemoryRepository;
import com.clipforge.memory.application.api.MemoryManagementUseCase;
import com.clipforge.memory.model.MemoryContext;
import com.clipforge.memory.read.MemoryReadRequest;
import com.clipforge.memory.read.MemoryReader;
import com.clipforge.memory.write.MemoryWriteRequest;
import com.clipforge.memory.write.MemoryWriteResult;
import com.clipforge.memory.write.MemoryWriter;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * MemoryApplicationService（记忆应用服务）。
 *
 * <p>该服务是应用层读取 MemoryContext（记忆上下文）的入口，后续 Controller（接口层）和 Workflow（工作流）都优先依赖该服务。
 */
@Service
@Profile("db")
public class MemoryApplicationService implements MemoryManagementUseCase {

    /** memoryReader（记忆读取器）负责从多类记忆仓储中组装记忆上下文。 */
    private final MemoryReader memoryReader;

    /** memoryWriterProvider（记忆写入器提供器）负责按需获取记忆写入能力。 */
    private final ObjectProvider<MemoryWriter> memoryWriterProvider;

    /** userPreferenceMemoryRepository（用户偏好记忆仓储）负责管理用户偏好记忆。 */
    private final UserPreferenceMemoryRepository userPreferenceMemoryRepository;

    /** projectMemoryRepository（项目记忆仓储）负责管理项目记忆。 */
    private final ProjectMemoryRepository projectMemoryRepository;

    /** creationHistoryMemoryRepository（创作历史记忆仓储）负责管理历史记忆。 */
    private final CreationHistoryMemoryRepository creationHistoryMemoryRepository;

    /**
     * 构造函数用于兼容只验证读取能力的单元测试。
     *
     * @param memoryReader 记忆读取器
     */
    public MemoryApplicationService(MemoryReader memoryReader) {
        // 调用完整构造函数，记忆写入器提供器为空表示当前测试不验证写入能力。
        this(memoryReader, null, null, null, null);
    }

    /**
     * 构造函数用于 Spring（Spring 框架）运行时注入读取器和写入器。
     *
     * @param memoryReader 记忆读取器
     * @param memoryWriterProvider 记忆写入器提供器
     */
    public MemoryApplicationService(MemoryReader memoryReader, ObjectProvider<MemoryWriter> memoryWriterProvider) {
        // 调用完整构造函数，仓储为空表示当前测试不验证管理接口。
        this(memoryReader, memoryWriterProvider, null, null, null);
    }

    /**
     * 构造函数用于 Spring（Spring 框架）运行时注入读取器、写入器和记忆仓储。
     *
     * @param memoryReader 记忆读取器
     * @param memoryWriterProvider 记忆写入器提供器
     * @param userPreferenceMemoryRepository 用户偏好记忆仓储
     * @param projectMemoryRepository 项目记忆仓储
     * @param creationHistoryMemoryRepository 创作历史记忆仓储
     */
    @Autowired
    public MemoryApplicationService(MemoryReader memoryReader,
                                    ObjectProvider<MemoryWriter> memoryWriterProvider,
                                    UserPreferenceMemoryRepository userPreferenceMemoryRepository,
                                    ProjectMemoryRepository projectMemoryRepository,
                                    CreationHistoryMemoryRepository creationHistoryMemoryRepository) {
        // 保存记忆读取器。
        this.memoryReader = memoryReader;
        // 保存记忆写入器提供器。
        this.memoryWriterProvider = memoryWriterProvider;
        // 保存用户偏好记忆仓储。
        this.userPreferenceMemoryRepository = userPreferenceMemoryRepository;
        // 保存项目记忆仓储。
        this.projectMemoryRepository = projectMemoryRepository;
        // 保存创作历史记忆仓储。
        this.creationHistoryMemoryRepository = creationHistoryMemoryRepository;
    }

    /**
     * readMemoryContext（读取记忆上下文）用于给创作链路准备可注入的记忆数据。
     *
     * @param request 记忆读取请求
     * @return 记忆上下文
     */
    public MemoryContext readMemoryContext(MemoryReadRequest request) {
        // 委托 MemoryReader（记忆读取器）执行具体读取和组装逻辑。
        return memoryReader.read(request);
    }

    /**
     * writeMemory（写入记忆）用于在创作成功后沉淀可供未来复用的 Memory（记忆）。
     *
     * @param request 记忆写入请求
     * @return 记忆写入结果
     */
    public MemoryWriteResult writeMemory(MemoryWriteRequest request) {
        // 如果没有记忆写入器提供器，则说明当前处于早期单元测试路径，直接跳过。
        if (memoryWriterProvider == null) {
            // 返回跳过结果。
            return MemoryWriteResult.skipped("未注入 MemoryWriter（记忆写入器），跳过记忆写入");
        }
        // 从 Spring（Spring 框架）容器中按需获取记忆写入器。
        MemoryWriter memoryWriter = memoryWriterProvider.getIfAvailable();
        // 如果当前 Profile（配置环境）下没有写入器，则跳过。
        if (memoryWriter == null) {
            // 返回跳过结果。
            return MemoryWriteResult.skipped("当前环境没有可用 MemoryWriter（记忆写入器），跳过记忆写入");
        }
        // 委托写入器执行实际沉淀逻辑。
        return memoryWriter.write(request);
    }

    /**
     * listUserPreferenceMemories（查询用户偏好记忆）用于读取某个用户启用中的长期偏好。
     *
     * @param userId 用户编号
     * @return 用户偏好记忆响应列表
     */
    public List<UserPreferenceMemoryResponse> listUserPreferenceMemories(String userId) {
        // 校验用户编号。
        requireText(userId, "userId（用户编号）不能为空");
        // 获取用户偏好记忆仓储。
        UserPreferenceMemoryRepository repository = requiredUserPreferenceMemoryRepository();
        // 查询并转换为响应对象。
        return repository.listActiveByUserId(userId).stream()
                .map(this::toUserPreferenceMemoryResponse)
                .toList();
    }

    /**
     * disableUserPreferenceMemory（停用用户偏好记忆）用于人工修正错误偏好。
     *
     * @param memoryId 记忆编号
     * @return 停用结果
     */
    public DisableMemoryResponse disableUserPreferenceMemory(String memoryId) {
        // 校验记忆编号。
        requireText(memoryId, "memoryId（记忆编号）不能为空");
        // 获取用户偏好记忆仓储。
        UserPreferenceMemoryRepository repository = requiredUserPreferenceMemoryRepository();
        // 查询记忆，找不到时抛出异常。
        UserPreferenceMemoryEntity entity = repository.findByMemoryId(memoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到用户偏好记忆，memoryId：" + memoryId));
        // 如果已经停用，则保持幂等，直接返回。
        if (MemoryStatus.DISABLED.value().equals(entity.getStatus())) {
            // 返回未更新结果。
            return DisableMemoryResponse.builder().memoryId(memoryId).memoryType("USER_PREFERENCE").status(MemoryStatus.DISABLED.value()).updated(false).build();
        }
        // 执行停用。
        boolean updated = repository.disableByMemoryId(memoryId);
        // 返回停用结果。
        return DisableMemoryResponse.builder().memoryId(memoryId).memoryType("USER_PREFERENCE").status(MemoryStatus.DISABLED.value()).updated(updated).build();
    }

    /**
     * queryProjectMemory（查询项目记忆）用于读取某个项目启用中的项目画像。
     *
     * @param projectId 项目编号
     * @return 项目记忆响应
     */
    public ProjectMemoryResponse queryProjectMemory(String projectId) {
        // 校验项目编号。
        requireText(projectId, "projectId（项目编号）不能为空");
        // 查询启用项目记忆。
        ProjectMemoryEntity entity = requiredProjectMemoryRepository().findActiveByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到启用项目记忆，projectId：" + projectId));
        // 转换为响应对象。
        return toProjectMemoryResponse(entity);
    }

    /**
     * disableProjectMemory（停用项目记忆）用于人工修正错误项目画像。
     *
     * @param memoryId 记忆编号
     * @return 停用结果
     */
    public DisableMemoryResponse disableProjectMemory(String memoryId) {
        // 校验记忆编号。
        requireText(memoryId, "memoryId（记忆编号）不能为空");
        // 获取项目记忆仓储。
        ProjectMemoryRepository repository = requiredProjectMemoryRepository();
        // 查询记忆，找不到时抛出异常。
        ProjectMemoryEntity entity = repository.findByMemoryId(memoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到项目记忆，memoryId：" + memoryId));
        // 如果已经停用，则保持幂等。
        if (MemoryStatus.DISABLED.value().equals(entity.getStatus())) {
            // 返回未更新结果。
            return DisableMemoryResponse.builder().memoryId(memoryId).memoryType("PROJECT").status(MemoryStatus.DISABLED.value()).updated(false).build();
        }
        // 执行停用。
        boolean updated = repository.disableByMemoryId(memoryId);
        // 返回停用结果。
        return DisableMemoryResponse.builder().memoryId(memoryId).memoryType("PROJECT").status(MemoryStatus.DISABLED.value()).updated(updated).build();
    }

    /**
     * listCreationHistoryMemories（查询创作历史记忆）用于按用户或项目读取历史高分方案摘要。
     *
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param platform 平台
     * @param limit 最大返回数量
     * @return 创作历史记忆响应列表
     */
    public List<CreationHistoryMemoryResponse> listCreationHistoryMemories(String userId, String projectId, String platform, Integer limit) {
        // 规范化返回数量。
        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        // 获取创作历史记忆仓储。
        CreationHistoryMemoryRepository repository = requiredCreationHistoryMemoryRepository();
        // 如果传入项目编号，则优先按项目查询。
        if (hasText(projectId)) {
            // 查询项目历史记忆。
            return repository.listActiveByProjectId(projectId, blankToNull(platform), safeLimit).stream()
                    .map(this::toCreationHistoryMemoryResponse)
                    .toList();
        }
        // 如果没有项目编号，则必须按用户查询。
        requireText(userId, "userId（用户编号）和 projectId（项目编号）不能同时为空");
        // 查询用户近期历史记忆。
        return repository.listRecentActiveByUserId(userId, safeLimit).stream()
                .map(this::toCreationHistoryMemoryResponse)
                .toList();
    }

    /**
     * disableCreationHistoryMemory（停用创作历史记忆）用于人工移除不再适合作为经验的历史摘要。
     *
     * @param memoryId 记忆编号
     * @return 停用结果
     */
    public DisableMemoryResponse disableCreationHistoryMemory(String memoryId) {
        // 校验记忆编号。
        requireText(memoryId, "memoryId（记忆编号）不能为空");
        // 获取历史记忆仓储。
        CreationHistoryMemoryRepository repository = requiredCreationHistoryMemoryRepository();
        // 查询记忆，找不到时抛出异常。
        CreationHistoryMemoryEntity entity = repository.findByMemoryId(memoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到创作历史记忆，memoryId：" + memoryId));
        // 如果已经停用，则保持幂等。
        if (MemoryStatus.DISABLED.value().equals(entity.getStatus())) {
            // 返回未更新结果。
            return DisableMemoryResponse.builder().memoryId(memoryId).memoryType("CREATION_HISTORY").status(MemoryStatus.DISABLED.value()).updated(false).build();
        }
        // 执行停用。
        boolean updated = repository.disableByMemoryId(memoryId);
        // 返回停用结果。
        return DisableMemoryResponse.builder().memoryId(memoryId).memoryType("CREATION_HISTORY").status(MemoryStatus.DISABLED.value()).updated(updated).build();
    }

    /**
     * requiredUserPreferenceMemoryRepository（获取用户偏好记忆仓储）用于处理仓储缺失场景。
     *
     * @return 用户偏好记忆仓储
     */
    private UserPreferenceMemoryRepository requiredUserPreferenceMemoryRepository() {
        // 如果仓储为空，则说明当前构造路径不支持持久化管理。
        if (userPreferenceMemoryRepository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "记忆管理接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回仓储。
        return userPreferenceMemoryRepository;
    }

    /**
     * requiredProjectMemoryRepository（获取项目记忆仓储）用于处理仓储缺失场景。
     *
     * @return 项目记忆仓储
     */
    private ProjectMemoryRepository requiredProjectMemoryRepository() {
        // 如果仓储为空，则说明当前构造路径不支持持久化管理。
        if (projectMemoryRepository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "记忆管理接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回仓储。
        return projectMemoryRepository;
    }

    /**
     * requiredCreationHistoryMemoryRepository（获取创作历史记忆仓储）用于处理仓储缺失场景。
     *
     * @return 创作历史记忆仓储
     */
    private CreationHistoryMemoryRepository requiredCreationHistoryMemoryRepository() {
        // 如果仓储为空，则说明当前构造路径不支持持久化管理。
        if (creationHistoryMemoryRepository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "记忆管理接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回仓储。
        return creationHistoryMemoryRepository;
    }

    /**
     * toUserPreferenceMemoryResponse（转用户偏好记忆响应）用于实体到 DTO（数据传输对象）的转换。
     *
     * @param entity 用户偏好记忆实体
     * @return 用户偏好记忆响应
     */
    private UserPreferenceMemoryResponse toUserPreferenceMemoryResponse(UserPreferenceMemoryEntity entity) {
        // 使用 Builder（建造者模式）组装响应对象。
        return UserPreferenceMemoryResponse.builder()
                .memoryId(entity.getMemoryId())
                .userId(entity.getUserId())
                .memoryType(entity.getMemoryType())
                .memoryKey(entity.getMemoryKey())
                .memoryValue(entity.getMemoryValue())
                .confidence(entity.getConfidence())
                .sourceType(entity.getSourceType())
                .sourceRunId(entity.getSourceRunId())
                .status(entity.getStatus())
                .build();
    }

    /**
     * toProjectMemoryResponse（转项目记忆响应）用于实体到 DTO（数据传输对象）的转换。
     *
     * @param entity 项目记忆实体
     * @return 项目记忆响应
     */
    private ProjectMemoryResponse toProjectMemoryResponse(ProjectMemoryEntity entity) {
        // 使用 Builder（建造者模式）组装响应对象。
        return ProjectMemoryResponse.builder()
                .memoryId(entity.getMemoryId())
                .projectId(entity.getProjectId())
                .userId(entity.getUserId())
                .summary(entity.getSummary())
                .targetPlatform(entity.getTargetPlatform())
                .targetAudience(entity.getTargetAudience())
                .contentStyle(entity.getContentStyle())
                .constraintsJson(entity.getConstraintsJson())
                .status(entity.getStatus())
                .build();
    }

    /**
     * toCreationHistoryMemoryResponse（转创作历史记忆响应）用于实体到 DTO（数据传输对象）的转换。
     *
     * @param entity 创作历史记忆实体
     * @return 创作历史记忆响应
     */
    private CreationHistoryMemoryResponse toCreationHistoryMemoryResponse(CreationHistoryMemoryEntity entity) {
        // 使用 Builder（建造者模式）组装响应对象。
        return CreationHistoryMemoryResponse.builder()
                .memoryId(entity.getMemoryId())
                .userId(entity.getUserId())
                .projectId(entity.getProjectId())
                .conversationId(entity.getConversationId())
                .runId(entity.getRunId())
                .theme(entity.getTheme())
                .platform(entity.getPlatform())
                .summary(entity.getSummary())
                .score(entity.getScore())
                .tagsJson(entity.getTagsJson())
                .status(entity.getStatus())
                .build();
    }

    /**
     * requireText（要求文本存在）用于统一校验必填字符串参数。
     *
     * @param value 原始文本
     * @param message 错误消息
     */
    private void requireText(String value, String message) {
        // 如果文本为空，则抛出参数错误。
        if (!hasText(value)) {
            // 抛出业务异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    /**
     * blankToNull（空白转空引用）用于查询条件清洗。
     *
     * @param value 原始文本
     * @return 清洗后的文本
     */
    private String blankToNull(String value) {
        // 空白文本返回空引用。
        return hasText(value) ? value.trim() : null;
    }

    /**
     * hasText（是否有文本）用于判断字符串去除空白后是否仍有内容。
     *
     * @param value 原始文本
     * @return 是否有文本
     */
    private boolean hasText(String value) {
        // 判断字符串不为空且去除空白后仍有内容。
        return value != null && !value.isBlank();
    }
}
