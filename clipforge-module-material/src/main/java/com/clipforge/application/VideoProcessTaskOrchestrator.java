package com.clipforge.application;

import com.clipforge.contract.material.VideoMetadataResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.material.infrastructure.persistence.entity.MaterialProcessTaskEntity;
import com.clipforge.material.infrastructure.persistence.repository.MaterialProcessTaskRepository;
import com.clipforge.messaging.rocketmq.material.video.VideoSubTaskProcessingMessage;
import com.clipforge.messaging.rocketmq.material.video.VideoSubTaskProcessingProducer;
import com.clipforge.task.TaskBizType;
import com.clipforge.task.TaskNextAction;
import com.clipforge.task.TaskPhase;
import com.clipforge.task.TaskStatusValue;
import com.clipforge.material.task.cache.MaterialProcessTaskStatus;
import com.clipforge.material.task.cache.MaterialProcessTaskStatusCache;
import com.clipforge.video.metadata.VideoMetadataProbeStatus;
import com.clipforge.video.process.VideoSubTaskType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * VideoProcessTaskOrchestrator（视频处理任务编排器）。
 *
 * <p>该编排器负责把视频父任务拆成可并行执行的子任务：FRAME_BRANCH（抽帧图像分支）、
 * AUDIO_BRANCH（音频分支）和 SUMMARY（汇总分支）。父任务只维护整体进度与最终可创作状态，
 * 具体耗时处理交给子任务消费者执行。
 */
@Service
@Profile("db")
@RequiredArgsConstructor
public class VideoProcessTaskOrchestrator {

    /** processTaskRepository（处理任务仓储）用于读写父任务和子任务。 */
    private final MaterialProcessTaskRepository processTaskRepository;

    /** videoMetadataApplicationService（视频元数据应用服务）用于父任务阶段先校验视频是否可解析。 */
    private final VideoMetadataApplicationService videoMetadataApplicationService;

    /** videoMaterialProcessApplicationService（视频素材处理应用服务）用于执行具体子任务。 */
    private final VideoMaterialProcessApplicationService videoMaterialProcessApplicationService;

    /** videoSubTaskProcessingProducer（视频子任务消息生产者）用于投递 FRAME、AUDIO 和 SUMMARY 子任务。 */
    private final VideoSubTaskProcessingProducer videoSubTaskProcessingProducer;

    /** statusCache（状态缓存）用于刷新 Redis（缓存）中的父子任务状态。 */
    private final MaterialProcessTaskStatusCache statusCache;

    /** transactionTemplate（事务模板）用于把任务创建和状态更新控制在短事务内。 */
    private final TransactionTemplate transactionTemplate;

    /**
     * processParentTask（处理父任务）用于读取视频元数据并派发可并行的子任务。
     *
     * @param processTaskId 父处理任务编号
     * @param instruction 视频理解指令
     */
    public void processParentTask(String processTaskId, String instruction) {
        MaterialProcessTaskEntity parentTask = findTask(processTaskId, "视频父处理任务不存在");
        try {
            updateTask(parentTask, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_METADATA, 10, "正在读取视频元数据", null, null, TaskNextAction.WAIT_PROCESSING, null);
            VideoMetadataResponse metadata = videoMetadataApplicationService.readMetadata(parentTask.getMaterialId());
            assertMetadataSucceeded(metadata);
            updateTask(parentTask, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_PROCESSING_ORCHESTRATING, 20, "视频元数据读取完成，正在派发抽帧图像分支和音频分支", null, null, TaskNextAction.WAIT_PROCESSING, null);
            MaterialProcessTaskEntity frameTask = createSubTaskIfAbsent(parentTask, VideoSubTaskType.FRAME_BRANCH, TaskPhase.VIDEO_FRAME_BRANCH, null);
            MaterialProcessTaskEntity audioTask = createSubTaskIfAbsent(parentTask, VideoSubTaskType.AUDIO_BRANCH, TaskPhase.VIDEO_AUDIO_BRANCH, null);
            sendSubTaskIfDispatchable(frameTask, VideoSubTaskType.FRAME_BRANCH, instruction);
            sendSubTaskIfDispatchable(audioTask, VideoSubTaskType.AUDIO_BRANCH, instruction);
        } catch (RuntimeException exception) {
            markParentFailed(parentTask, exception);
            throw exception;
        }
    }

    /**
     * processSubTask（处理子任务）用于根据消息中的子任务类型分派到具体处理逻辑。
     *
     * @param message 视频子任务处理消息
     */
    public void processSubTask(VideoSubTaskProcessingMessage message) {
        if (message == null || message.getProcessTaskId() == null || message.getSubTaskType() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频子任务消息缺少必要字段");
        }
        VideoSubTaskType subTaskType = VideoSubTaskType.valueOf(message.getSubTaskType());
        try {
            if (subTaskType == VideoSubTaskType.FRAME_BRANCH) {
                videoMaterialProcessApplicationService.processFrameBranchTask(message.getProcessTaskId(), message.getInstruction());
            } else if (subTaskType == VideoSubTaskType.AUDIO_BRANCH) {
                videoMaterialProcessApplicationService.processAudioBranchTask(message.getProcessTaskId(), message.getInstruction());
            } else if (subTaskType == VideoSubTaskType.SUMMARY) {
                videoMaterialProcessApplicationService.processSummaryTask(message.getProcessTaskId(), message.getInstruction());
            } else {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的视频子任务类型：" + message.getSubTaskType());
            }
            onSubTaskFinished(message.getParentProcessTaskId(), message.getInstruction());
        } catch (RuntimeException exception) {
            onSubTaskFinished(message.getParentProcessTaskId(), message.getInstruction());
            throw exception;
        }
    }

    /**
     * onSubTaskFinished（子任务完成回调）用于刷新父任务状态，并在条件满足时触发 SUMMARY（汇总）子任务。
     *
     * @param parentProcessTaskId 父处理任务编号
     * @param instruction 视频理解指令
     */
    public void onSubTaskFinished(String parentProcessTaskId, String instruction) {
        if (parentProcessTaskId == null || parentProcessTaskId.isBlank()) {
            return;
        }
        MaterialProcessTaskEntity parentTask = findTask(parentProcessTaskId, "视频父处理任务不存在");
        List<MaterialProcessTaskEntity> subTasks = processTaskRepository.findByParentProcessTaskId(parentProcessTaskId);
        Optional<MaterialProcessTaskEntity> failedTask = firstByStatus(subTasks, TaskStatusValue.FAILED);
        if (failedTask.isPresent()) {
            markParentFailed(parentTask, new BusinessException(ErrorCode.MATERIAL_NOT_READY, "视频子任务处理失败：" + failedTask.get().getMessage()));
            return;
        }
        Optional<MaterialProcessTaskEntity> summaryTask = findSubTask(subTasks, VideoSubTaskType.SUMMARY);
        if (summaryTask.isPresent() && TaskStatusValue.SUCCEEDED.value().equals(summaryTask.get().getStatus())) {
            updateTask(parentTask, TaskStatusValue.SUCCEEDED, TaskPhase.READY_FOR_CREATION, 100, "视频处理完成，可用于创作", null, null, TaskNextAction.ALLOW_CREATION, LocalDateTime.now());
            return;
        }
        Optional<MaterialProcessTaskEntity> frameTask = findSubTask(subTasks, VideoSubTaskType.FRAME_BRANCH);
        Optional<MaterialProcessTaskEntity> audioTask = findSubTask(subTasks, VideoSubTaskType.AUDIO_BRANCH);
        boolean frameReady = frameTask.isPresent() && TaskStatusValue.SUCCEEDED.value().equals(frameTask.get().getStatus());
        boolean audioReady = audioTask.isPresent() && TaskStatusValue.SUCCEEDED.value().equals(audioTask.get().getStatus());
        if (frameReady && audioReady && summaryTask.isEmpty()) {
            Optional<MaterialProcessTaskEntity> createdSummaryTask = createSummarySubTaskIfAbsent(parentTask, frameTask.get(), audioTask.get());
            createdSummaryTask.ifPresent(task -> sendSubTask(task, VideoSubTaskType.SUMMARY, instruction));
            updateTask(parentTask, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_SUMMARY, 80, "抽帧图像分支和音频分支已完成，正在汇总视频理解结果", null, null, TaskNextAction.WAIT_PROCESSING, null);
            return;
        }
        updateTask(parentTask, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_PROCESSING_ORCHESTRATING, calculateParentProgress(frameReady, audioReady, summaryTask), "视频子任务处理中，等待抽帧图像分支、音频分支或汇总分支完成", null, null, TaskNextAction.WAIT_PROCESSING, null);
    }

    /**
     * createSummarySubTaskIfAbsent（如果不存在则创建汇总子任务）用于在两个并行分支均完成后幂等创建 SUMMARY（汇总）任务。
     *
     * @param parentTask 父处理任务
     * @param frameTask 抽帧图像分支任务
     * @param audioTask 音频分支任务
     * @return 本次新创建的汇总子任务，如果已经存在则返回空
     */
    private Optional<MaterialProcessTaskEntity> createSummarySubTaskIfAbsent(MaterialProcessTaskEntity parentTask,
                                                                             MaterialProcessTaskEntity frameTask,
                                                                             MaterialProcessTaskEntity audioTask) {
        AtomicReference<MaterialProcessTaskEntity> summaryTaskRef = new AtomicReference<>();
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Optional<MaterialProcessTaskEntity> existing = processTaskRepository.findByParentProcessTaskIdAndSubTaskKey(parentTask.getProcessTaskId(), VideoSubTaskType.SUMMARY.value());
            if (existing.isPresent()) {
                return;
            }
            String dependsOnTaskIds = "[\"" + frameTask.getProcessTaskId() + "\",\"" + audioTask.getProcessTaskId() + "\"]";
            MaterialProcessTaskEntity summaryTask = buildSubTask(parentTask, VideoSubTaskType.SUMMARY, TaskPhase.VIDEO_SUMMARY, dependsOnTaskIds);
            processTaskRepository.save(summaryTask);
            summaryTaskRef.set(summaryTask);
        });
        return Optional.ofNullable(summaryTaskRef.get());
    }

    /**
     * createSubTaskIfAbsent（如果不存在则创建子任务）用于幂等创建 FRAME（抽帧）或 AUDIO（音频）子任务。
     *
     * @param parentTask 父处理任务
     * @param subTaskType 子任务类型
     * @param phase 初始阶段
     * @param dependsOnTaskIds 依赖任务编号 JSON（对象表示格式）字符串
     * @return 已存在或新创建的子任务
     */
    private MaterialProcessTaskEntity createSubTaskIfAbsent(MaterialProcessTaskEntity parentTask, VideoSubTaskType subTaskType,
                                                            TaskPhase phase, String dependsOnTaskIds) {
        AtomicReference<MaterialProcessTaskEntity> subTaskRef = new AtomicReference<>();
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Optional<MaterialProcessTaskEntity> existing = processTaskRepository.findByParentProcessTaskIdAndSubTaskKey(parentTask.getProcessTaskId(), subTaskType.value());
            if (existing.isPresent()) {
                subTaskRef.set(existing.get());
                return;
            }
            MaterialProcessTaskEntity subTask = buildSubTask(parentTask, subTaskType, phase, dependsOnTaskIds);
            processTaskRepository.save(subTask);
            subTaskRef.set(subTask);
        });
        return subTaskRef.get();
    }

    /**
     * buildSubTask（构建子任务）用于复制父任务上下文并生成新的子任务编号。
     *
     * @param parentTask 父处理任务
     * @param subTaskType 子任务类型
     * @param phase 初始阶段
     * @param dependsOnTaskIds 依赖任务编号 JSON（对象表示格式）字符串
     * @return 子任务实体
     */
    private MaterialProcessTaskEntity buildSubTask(MaterialProcessTaskEntity parentTask, VideoSubTaskType subTaskType,
                                                   TaskPhase phase, String dependsOnTaskIds) {
        LocalDateTime now = LocalDateTime.now();
        return MaterialProcessTaskEntity.builder()
                .id("material_process_task_pk_" + shortUuid())
                .processTaskId("process_task_video_" + subTaskType.name().toLowerCase() + "_" + shortUuid())
                .parentProcessTaskId(parentTask.getProcessTaskId())
                .subTaskKey(subTaskType.value())
                .dependsOnTaskIds(dependsOnTaskIds)
                .materialId(parentTask.getMaterialId())
                .uploadBatchId(parentTask.getUploadBatchId())
                .userId(parentTask.getUserId())
                .projectId(parentTask.getProjectId())
                .conversationId(parentTask.getConversationId())
                .runId(parentTask.getRunId())
                .taskType(parentTask.getTaskType())
                .bizType(TaskBizType.MATERIAL.value())
                .status(TaskStatusValue.PENDING.value())
                .phase(phase.value())
                .progressPercent(0)
                .message(subTaskType.value() + " 子任务已创建，等待处理")
                .retryCount(0)
                .canRetry(0)
                .nextAction(TaskNextAction.WAIT_PROCESSING.value())
                .updatedAt(now)
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
    }

    /**
     * sendSubTask（发送子任务）用于把子任务投递到 RocketMQ（消息队列）。
     *
     * @param subTask 子任务实体
     * @param subTaskType 子任务类型
     * @param instruction 视频理解指令
     */
    private void sendSubTask(MaterialProcessTaskEntity subTask, VideoSubTaskType subTaskType, String instruction) {
        videoSubTaskProcessingProducer.send(VideoSubTaskProcessingMessage.builder()
                .messageId("video_sub_task_msg_" + shortUuid())
                .parentProcessTaskId(subTask.getParentProcessTaskId())
                .processTaskId(subTask.getProcessTaskId())
                .materialId(subTask.getMaterialId())
                .subTaskType(subTaskType.value())
                .runId(subTask.getRunId())
                .instruction(instruction)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * sendSubTaskIfDispatchable（如果可投递则发送子任务）用于避免父任务消息重复消费时反复投递已完成或正在执行的子任务。
     *
     * @param subTask 子任务实体
     * @param subTaskType 子任务类型
     * @param instruction 视频理解指令
     */
    private void sendSubTaskIfDispatchable(MaterialProcessTaskEntity subTask, VideoSubTaskType subTaskType, String instruction) {
        if (TaskStatusValue.PENDING.value().equals(subTask.getStatus()) || TaskStatusValue.FAILED.value().equals(subTask.getStatus())) {
            sendSubTask(subTask, subTaskType, instruction);
        }
    }

    /**
     * assertMetadataSucceeded（断言元数据成功）用于在父任务阶段拦截损坏视频或不可解析视频。
     *
     * @param metadata 视频元数据响应
     */
    private void assertMetadataSucceeded(VideoMetadataResponse metadata) {
        if (metadata == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "视频元数据读取结果为空");
        }
        if (VideoMetadataProbeStatus.SUCCEEDED.value().equals(metadata.getProbeStatus())) {
            return;
        }
        throw new BusinessException(ErrorCode.MATERIAL_NOT_READY, "视频元数据读取失败，probeStatus=" + metadata.getProbeStatus() + "，原因：" + metadata.getProbeErrorMessage());
    }

    /**
     * findTask（查询任务）用于按处理任务编号读取任务。
     *
     * @param processTaskId 处理任务编号
     * @param message 未找到时的错误消息
     * @return 处理任务实体
     */
    private MaterialProcessTaskEntity findTask(String processTaskId, String message) {
        return processTaskRepository.findByProcessTaskId(processTaskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, message + "，processTaskId=" + processTaskId));
    }

    /**
     * findSubTask（查找子任务）用于在子任务列表中按子任务类型定位记录。
     *
     * @param subTasks 子任务列表
     * @param subTaskType 子任务类型
     * @return 子任务记录
     */
    private Optional<MaterialProcessTaskEntity> findSubTask(List<MaterialProcessTaskEntity> subTasks, VideoSubTaskType subTaskType) {
        return subTasks.stream()
                .filter(task -> subTaskType.value().equals(task.getSubTaskKey()))
                .findFirst();
    }

    /**
     * firstByStatus（按状态查找第一个任务）用于判断是否存在失败子任务。
     *
     * @param tasks 任务列表
     * @param status 状态
     * @return 第一个匹配任务
     */
    private Optional<MaterialProcessTaskEntity> firstByStatus(List<MaterialProcessTaskEntity> tasks, TaskStatusValue status) {
        return tasks.stream()
                .filter(task -> status.value().equals(task.getStatus()))
                .findFirst();
    }

    /**
     * calculateParentProgress（计算父任务进度）用于按子任务完成情况给前端返回粗粒度进度。
     *
     * @param frameReady 抽帧图像分支是否完成
     * @param audioReady 音频分支是否完成
     * @param summaryTask 汇总任务
     * @return 父任务进度百分比
     */
    private int calculateParentProgress(boolean frameReady, boolean audioReady, Optional<MaterialProcessTaskEntity> summaryTask) {
        int progress = 20;
        if (frameReady) {
            progress += 35;
        }
        if (audioReady) {
            progress += 20;
        }
        if (summaryTask.isPresent()) {
            progress += 5;
        }
        return Math.min(progress, 95);
    }

    /**
     * markParentFailed（标记父任务失败）用于把子任务异常汇总到父任务。
     *
     * @param parentTask 父处理任务
     * @param exception 异常
     */
    private void markParentFailed(MaterialProcessTaskEntity parentTask, RuntimeException exception) {
        String errorCode = exception instanceof BusinessException businessException ? businessException.getErrorCode().getCode() : ErrorCode.INTERNAL_ERROR.getCode();
        updateTask(parentTask, TaskStatusValue.FAILED, TaskPhase.FAILED, 100, "视频处理失败", errorCode, exception.getMessage(), TaskNextAction.RETRY_PROCESSING, LocalDateTime.now());
    }

    /**
     * updateTask（更新任务）用于同步数据库和 Redis（缓存）的处理状态。
     *
     * @param task 处理任务
     * @param status 状态
     * @param phase 阶段
     * @param progressPercent 进度百分比
     * @param message 状态说明
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param nextAction 下一步动作
     * @param endedAt 结束时间
     */
    private void updateTask(MaterialProcessTaskEntity task, TaskStatusValue status, TaskPhase phase, Integer progressPercent, String message,
                            String errorCode, String errorMessage, TaskNextAction nextAction, LocalDateTime endedAt) {
        LocalDateTime now = LocalDateTime.now();
        if (task.getStartedAt() == null && status == TaskStatusValue.PROCESSING) {
            task.setStartedAt(now);
        }
        task.setStatus(status.value());
        task.setPhase(phase.value());
        task.setProgressPercent(progressPercent);
        task.setMessage(message);
        task.setErrorCode(errorCode);
        task.setErrorMessage(errorMessage);
        task.setNextAction(nextAction.value());
        task.setCanRetry(status == TaskStatusValue.FAILED ? 1 : 0);
        task.setUpdatedAt(now);
        task.setEndedAt(endedAt);
        task.setUpdateTime(now);
        transactionTemplate.executeWithoutResult(transactionStatus -> processTaskRepository.update(task));
        statusCache.save(toCacheStatus(task));
    }

    /**
     * toCacheStatus（转缓存状态）用于把任务实体转换成 Redis（缓存）状态对象。
     *
     * @param task 处理任务
     * @return 缓存状态
     */
    private MaterialProcessTaskStatus toCacheStatus(MaterialProcessTaskEntity task) {
        return MaterialProcessTaskStatus.builder()
                .taskId(task.getProcessTaskId())
                .taskType(task.getTaskType())
                .bizId(task.getMaterialId())
                .bizType(TaskBizType.MATERIAL.value())
                .status(task.getStatus())
                .phase(task.getPhase())
                .progressPercent(task.getProgressPercent())
                .message(task.getMessage())
                .errorCode(task.getErrorCode())
                .errorMessage(task.getErrorMessage())
                .retryCount(task.getRetryCount())
                .canRetry(task.getCanRetry() != null && task.getCanRetry() == 1)
                .nextAction(task.getNextAction())
                .startedAt(task.getStartedAt())
                .updatedAt(task.getUpdatedAt())
                .endedAt(task.getEndedAt())
                .build();
    }

    /**
     * shortUuid（短 UUID）用于生成没有连字符的业务编号片段。
     *
     * @return UUID（通用唯一标识）字符串
     */
    private String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
