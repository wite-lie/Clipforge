package com.clipforge.application;

import com.clipforge.contract.knowledge.TaskStatusResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.material.infrastructure.persistence.entity.MaterialEntity;
import com.clipforge.material.infrastructure.persistence.entity.MaterialProcessTaskEntity;
import com.clipforge.material.infrastructure.persistence.repository.MaterialProcessTaskRepository;
import com.clipforge.material.infrastructure.persistence.repository.MaterialRepository;
import com.clipforge.material.model.MaterialType;
import com.clipforge.task.TaskBizType;
import com.clipforge.task.TaskNextAction;
import com.clipforge.task.TaskPhase;
import com.clipforge.task.TaskStatusSource;
import com.clipforge.task.TaskStatusValue;
import com.clipforge.task.TaskType;
import com.clipforge.material.task.cache.MaterialProcessTaskStatus;
import com.clipforge.material.task.cache.MaterialProcessTaskStatusCache;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * TaskStatusApplicationService（任务状态应用服务）。
 *
 * <p>该服务负责把不同业务领域的后台任务状态统一包装为前端可查询的 TaskStatusResponse（任务状态响应）。
 */
@Service
@RequiredArgsConstructor
public class TaskStatusApplicationService {

    /** materialProcessTaskRepositoryProvider（素材处理任务仓储提供器）用于读取真实素材处理任务。 */
    private final ObjectProvider<MaterialProcessTaskRepository> materialProcessTaskRepositoryProvider;

    /** materialRepositoryProvider（素材仓储提供器）用于兼容历史素材派生状态。 */
    private final ObjectProvider<MaterialRepository> materialRepositoryProvider;

    /** materialProcessTaskStatusCache（素材处理任务状态缓存）用于优先读取 Redis（缓存）中的热点任务状态。 */
    private final MaterialProcessTaskStatusCache materialProcessTaskStatusCache;

    /**
     * queryByTaskId（按任务编号查询）用于统一查询后台任务状态。
     *
     * @param taskId 任务编号
     * @return 任务状态响应
     */
    public TaskStatusResponse queryByTaskId(String taskId) {
        // 校验任务编号不能为空。
        if (taskId == null || taskId.isBlank()) {
            // 抛出请求参数异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务编号不能为空");
        }
        // 获取任务仓储，未启用数据库时直接返回明确错误。
        MaterialProcessTaskRepository processTaskRepository = requiredMaterialProcessTaskRepository();
        // 优先从 Redis（缓存）读取素材处理任务状态。
        Optional<MaterialProcessTaskStatus> cachedStatus = safeGetCache(taskId.strip());
        // 如果缓存命中，则直接返回 Redis（缓存）来源。
        if (cachedStatus.isPresent()) {
            // 返回缓存任务状态。
            return fromCachedStatus(cachedStatus.get(), TaskStatusSource.REDIS.value());
        }
        // 优先从真实素材处理任务表查询。
        MaterialProcessTaskEntity processTask = processTaskRepository.findByProcessTaskId(taskId.strip()).orElse(null);
        // 如果查到真实任务，则直接转换为统一响应。
        if (processTask != null) {
            // 转换为数据库任务状态响应。
            TaskStatusResponse response = fromProcessTask(processTask, TaskStatusSource.DATABASE.value());
            // 尝试把数据库状态回填 Redis（缓存）。
            safeSaveCache(toCacheStatus(response));
            // 返回数据库任务状态。
            return response;
        }
        // 尝试兼容优化项 1 派生的历史任务编号。
        TaskStatusResponse response = queryDerivedMaterialStatus(taskId.strip());
        // 尝试把派生状态回填 Redis（缓存）。
        safeSaveCache(toCacheStatus(response));
        // 返回派生任务状态。
        return response;
    }

    /**
     * fromProcessTask（从处理任务转换）用于把素材处理任务实体转换为统一任务状态响应。
     *
     * @param entity 素材处理任务实体
     * @param source 状态来源
     * @return 任务状态响应
     */
    private TaskStatusResponse fromProcessTask(MaterialProcessTaskEntity entity, String source) {
        // 使用 Builder（建造者模式）组装统一响应。
        return TaskStatusResponse.builder()
                // 设置任务编号。
                .taskId(entity.getProcessTaskId())
                // 设置任务类型。
                .taskType(entity.getTaskType())
                // 设置业务编号。
                .bizId(entity.getMaterialId())
                // 设置业务类型。
                .bizType(entity.getBizType())
                // 设置任务状态。
                .status(entity.getStatus())
                // 设置处理阶段。
                .phase(entity.getPhase())
                // 设置进度百分比。
                .progressPercent(entity.getProgressPercent())
                // 设置状态说明。
                .message(entity.getMessage())
                // 设置错误码。
                .errorCode(entity.getErrorCode())
                // 设置错误消息。
                .errorMessage(entity.getErrorMessage())
                // 设置重试次数。
                .retryCount(entity.getRetryCount())
                // 设置是否可重试。
                .canRetry(entity.getCanRetry() != null && entity.getCanRetry() == 1)
                // 设置下一步动作。
                .nextAction(entity.getNextAction())
                // 设置开始时间。
                .startedAt(entity.getStartedAt())
                // 设置更新时间。
                .updatedAt(entity.getUpdatedAt())
                // 设置结束时间。
                .endedAt(entity.getEndedAt())
                // 设置状态来源。
                .source(source)
                // 构建响应。
                .build();
    }

    /**
     * fromCachedStatus（从缓存状态转换）用于把 Redis（缓存）中的素材处理状态转换为统一任务状态响应。
     *
     * @param status 缓存状态
     * @param source 状态来源
     * @return 任务状态响应
     */
    private TaskStatusResponse fromCachedStatus(MaterialProcessTaskStatus status, String source) {
        // 使用 Builder（建造者模式）组装统一响应。
        return TaskStatusResponse.builder()
                // 设置任务编号。
                .taskId(status.getTaskId())
                // 设置任务类型。
                .taskType(status.getTaskType())
                // 设置业务编号。
                .bizId(status.getBizId())
                // 设置业务类型。
                .bizType(status.getBizType())
                // 设置任务状态。
                .status(status.getStatus())
                // 设置处理阶段。
                .phase(status.getPhase())
                // 设置进度百分比。
                .progressPercent(status.getProgressPercent())
                // 设置状态说明。
                .message(status.getMessage())
                // 设置错误码。
                .errorCode(status.getErrorCode())
                // 设置错误消息。
                .errorMessage(status.getErrorMessage())
                // 设置重试次数。
                .retryCount(status.getRetryCount())
                // 设置是否可重试。
                .canRetry(Boolean.TRUE.equals(status.getCanRetry()))
                // 设置下一步动作。
                .nextAction(status.getNextAction())
                // 设置开始时间。
                .startedAt(status.getStartedAt())
                // 设置更新时间。
                .updatedAt(status.getUpdatedAt())
                // 设置结束时间。
                .endedAt(status.getEndedAt())
                // 设置状态来源。
                .source(source)
                // 构建响应。
                .build();
    }

    /**
     * toCacheStatus（转缓存状态）用于把统一任务状态响应转换为 Redis（缓存）内部对象。
     *
     * @param response 任务状态响应
     * @return 素材处理任务缓存状态
     */
    private MaterialProcessTaskStatus toCacheStatus(TaskStatusResponse response) {
        // 使用 Builder（建造者模式）组装缓存对象。
        return MaterialProcessTaskStatus.builder()
                // 设置任务编号。
                .taskId(response.getTaskId())
                // 设置任务类型。
                .taskType(response.getTaskType())
                // 设置业务编号。
                .bizId(response.getBizId())
                // 设置业务类型。
                .bizType(response.getBizType())
                // 设置任务状态。
                .status(response.getStatus())
                // 设置处理阶段。
                .phase(response.getPhase())
                // 设置进度百分比。
                .progressPercent(response.getProgressPercent())
                // 设置状态说明。
                .message(response.getMessage())
                // 设置错误码。
                .errorCode(response.getErrorCode())
                // 设置错误消息。
                .errorMessage(response.getErrorMessage())
                // 设置重试次数。
                .retryCount(response.getRetryCount())
                // 设置是否可重试。
                .canRetry(response.getCanRetry())
                // 设置下一步动作。
                .nextAction(response.getNextAction())
                // 设置开始时间。
                .startedAt(response.getStartedAt())
                // 设置更新时间。
                .updatedAt(response.getUpdatedAt())
                // 设置结束时间。
                .endedAt(response.getEndedAt())
                // 构建缓存对象。
                .build();
    }

    /**
     * safeGetCache（安全读取缓存）用于在 Redis（缓存）异常时自动回退数据库。
     *
     * @param taskId 任务编号
     * @return 可能存在的缓存状态
     */
    private Optional<MaterialProcessTaskStatus> safeGetCache(String taskId) {
        try {
            // 读取缓存状态。
            return materialProcessTaskStatusCache.get(taskId);
        } catch (RuntimeException exception) {
            // 缓存读取失败不影响主流程，回退数据库。
            return Optional.empty();
        }
    }

    /**
     * safeSaveCache（安全保存缓存）用于在 Redis（缓存）异常时不影响主流程。
     *
     * @param status 素材处理任务缓存状态
     */
    private void safeSaveCache(MaterialProcessTaskStatus status) {
        try {
            // 写入缓存状态。
            materialProcessTaskStatusCache.save(status);
        } catch (RuntimeException exception) {
            // 缓存写入失败不影响主流程。
        }
    }

    /**
     * queryDerivedMaterialStatus（查询派生素材状态）用于兼容没有真实任务记录的历史素材。
     *
     * @param taskId 任务编号
     * @return 任务状态响应
     */
    private TaskStatusResponse queryDerivedMaterialStatus(String taskId) {
        // 只有 process_task_ 前缀的任务编号才能尝试从素材编号反推。
        if (!taskId.startsWith("process_task_")) {
            // 抛出数据不存在异常。
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到任务状态，taskId：" + taskId);
        }
        // 从任务编号还原素材编号。
        String materialId = "material_" + taskId.substring("process_task_".length());
        // 查询素材实体。
        MaterialEntity material = requiredMaterialRepository().findByMaterialId(materialId)
                // 找不到素材时抛出数据不存在异常。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到任务状态，taskId：" + taskId));
        // 从素材状态派生任务状态。
        return TaskStatusResponse.builder()
                // 设置任务编号。
                .taskId(taskId)
                // 设置任务类型。
                .taskType(taskType(material.getMaterialType()))
                // 设置业务编号。
                .bizId(material.getMaterialId())
                // 设置业务类型。
                .bizType(TaskBizType.MATERIAL.value())
                // 设置任务状态。
                .status(processStatus(material.getStatus()))
                // 设置任务阶段。
                .phase(processPhase(material.getMaterialType(), material.getStatus()))
                // 设置进度百分比。
                .progressPercent(progressPercent(processStatus(material.getStatus())))
                // 设置状态说明。
                .message(statusMessage(material.getMaterialType(), material.getStatus()))
                // 历史派生状态没有错误码。
                .errorCode(null)
                // 历史派生状态没有错误消息。
                .errorMessage(null)
                // 历史派生状态没有重试次数。
                .retryCount(0)
                // 失败状态可以重试，其他状态暂不允许。
                .canRetry(TaskStatusValue.FAILED.value().equals(processStatus(material.getStatus())))
                // 设置下一步动作。
                .nextAction(nextAction(processStatus(material.getStatus())))
                // 历史派生状态没有任务开始时间。
                .startedAt(null)
                // 使用素材更新时间作为状态更新时间。
                .updatedAt(material.getUpdateTime() == null ? LocalDateTime.now() : material.getUpdateTime())
                // 成功或失败时使用素材更新时间作为结束时间。
                .endedAt(endedAt(material))
                // 标记状态来源为素材派生。
                .source(TaskStatusSource.MATERIAL_DERIVED.value())
                // 构建响应。
                .build();
    }

    /**
     * taskType（任务类型）用于根据素材类型推断统一任务类型。
     *
     * @param materialType 素材类型
     * @return 任务类型
     */
    private String taskType(String materialType) {
        // 视频素材对应视频处理任务。
        if (MaterialType.VIDEO.value().equals(materialType)) {
            // 返回视频处理任务类型。
            return TaskType.MATERIAL_VIDEO_PROCESSING.value();
        }
        // 默认返回图片理解任务类型。
        return TaskType.MATERIAL_IMAGE_UNDERSTANDING.value();
    }

    /**
     * processStatus（处理状态）用于把素材状态映射为统一任务状态。
     *
     * @param materialStatus 素材状态
     * @return 任务状态
     */
    private String processStatus(String materialStatus) {
        // 空状态按等待中兜底。
        if (materialStatus == null || materialStatus.isBlank()) {
            // 返回等待中。
            return TaskStatusValue.PENDING.value();
        }
        // 标准化素材状态。
        String normalizedStatus = materialStatus.strip().toUpperCase();
        // 已理解表示成功。
        if ("UNDERSTOOD".equals(normalizedStatus) || "READY".equals(normalizedStatus) || "READY_FOR_CREATION".equals(normalizedStatus)) {
            // 返回成功。
            return TaskStatusValue.SUCCEEDED.value();
        }
        // 失败状态映射为失败。
        if ("FAILED".equals(normalizedStatus)) {
            // 返回失败。
            return TaskStatusValue.FAILED.value();
        }
        // 处理中状态映射为处理中。
        if ("UNDERSTANDING".equals(normalizedStatus) || "PROCESSING".equals(normalizedStatus)) {
            // 返回处理中。
            return TaskStatusValue.PROCESSING.value();
        }
        // 默认返回等待中。
        return TaskStatusValue.PENDING.value();
    }

    /**
     * processPhase（处理阶段）用于把素材状态映射为统一任务阶段。
     *
     * @param materialType 素材类型
     * @param materialStatus 素材状态
     * @return 任务阶段
     */
    private String processPhase(String materialType, String materialStatus) {
        // 计算统一任务状态。
        String status = processStatus(materialStatus);
        // 成功状态表示可创作。
        if (TaskStatusValue.SUCCEEDED.value().equals(status)) {
            // 返回可创作阶段。
            return TaskPhase.READY_FOR_CREATION.value();
        }
        // 失败状态表示失败阶段。
        if (TaskStatusValue.FAILED.value().equals(status)) {
            // 返回失败阶段。
            return TaskPhase.FAILED.value();
        }
        // 视频素材等待视频处理。
        if (MaterialType.VIDEO.value().equals(materialType)) {
            // 返回等待视频处理。
            return TaskPhase.WAITING_VIDEO_PROCESSING.value();
        }
        // 默认等待图片理解。
        return TaskPhase.WAITING_IMAGE_UNDERSTANDING.value();
    }

    /**
     * progressPercent（进度百分比）用于按统一任务状态生成粗粒度进度。
     *
     * @param status 任务状态
     * @return 进度百分比
     */
    private Integer progressPercent(String status) {
        // 成功为 100%。
        if (TaskStatusValue.SUCCEEDED.value().equals(status)) {
            // 返回完成进度。
            return 100;
        }
        // 处理中为 50%。
        if (TaskStatusValue.PROCESSING.value().equals(status)) {
            // 返回处理中进度。
            return 50;
        }
        // 其他状态为 0%。
        return 0;
    }

    /**
     * nextAction（下一步动作）用于按任务状态生成前端动作建议。
     *
     * @param status 任务状态
     * @return 下一步动作
     */
    private String nextAction(String status) {
        // 成功允许创作。
        if (TaskStatusValue.SUCCEEDED.value().equals(status)) {
            // 返回允许创作。
            return TaskNextAction.ALLOW_CREATION.value();
        }
        // 失败允许重试。
        if (TaskStatusValue.FAILED.value().equals(status)) {
            // 返回重试处理。
            return TaskNextAction.RETRY_PROCESSING.value();
        }
        // 其他状态等待处理。
        return TaskNextAction.WAIT_PROCESSING.value();
    }

    /**
     * statusMessage（状态说明）用于生成派生状态的人类可读说明。
     *
     * @param materialType 素材类型
     * @param materialStatus 素材状态
     * @return 状态说明
     */
    private String statusMessage(String materialType, String materialStatus) {
        // 计算统一状态。
        String status = processStatus(materialStatus);
        // 成功说明。
        if (TaskStatusValue.SUCCEEDED.value().equals(status)) {
            // 返回成功说明。
            return "素材处理完成，可以用于创作";
        }
        // 失败说明。
        if (TaskStatusValue.FAILED.value().equals(status)) {
            // 返回失败说明。
            return "素材处理失败，请检查失败原因后重试";
        }
        // 处理中说明。
        if (TaskStatusValue.PROCESSING.value().equals(status)) {
            // 返回处理中说明。
            return "素材正在处理中，请稍后查看";
        }
        // 图片等待说明。
        if (MaterialType.IMAGE.value().equals(materialType)) {
            // 返回图片等待说明。
            return "图片已上传，等待图片理解处理";
        }
        // 返回视频等待说明。
        return "视频已上传，等待视频处理流水线";
    }

    /**
     * endedAt（结束时间）用于从历史素材状态推断任务结束时间。
     *
     * @param material 素材实体
     * @return 结束时间
     */
    private LocalDateTime endedAt(MaterialEntity material) {
        // 计算统一状态。
        String status = processStatus(material.getStatus());
        // 成功或失败才认为任务已经结束。
        if (TaskStatusValue.SUCCEEDED.value().equals(status) || TaskStatusValue.FAILED.value().equals(status)) {
            // 返回素材更新时间。
            return material.getUpdateTime();
        }
        // 其他状态没有结束时间。
        return null;
    }

    /**
     * requiredMaterialProcessTaskRepository（获取必需素材处理任务仓储）用于处理未启用数据库的场景。
     *
     * @return 素材处理任务仓储
     */
    private MaterialProcessTaskRepository requiredMaterialProcessTaskRepository() {
        // 从 Spring 容器中获取素材处理任务仓储。
        MaterialProcessTaskRepository repository = materialProcessTaskRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，则说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "任务状态查询需要启用 db Profile（数据库配置环境）");
        }
        // 返回素材处理任务仓储。
        return repository;
    }

    /**
     * requiredMaterialRepository（获取必需素材仓储）用于处理历史素材派生状态查询。
     *
     * @return 素材仓储
     */
    private MaterialRepository requiredMaterialRepository() {
        // 从 Spring 容器中获取素材仓储。
        MaterialRepository repository = materialRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，则说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "任务状态查询需要启用 db Profile（数据库配置环境）");
        }
        // 返回素材仓储。
        return repository;
    }
}
