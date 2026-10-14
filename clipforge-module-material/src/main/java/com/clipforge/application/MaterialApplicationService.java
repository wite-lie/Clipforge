package com.clipforge.application;

import com.clipforge.contract.material.MaterialResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.material.application.MaterialQueryUseCase;
import com.clipforge.material.application.MaterialUploadCommand;
import com.clipforge.material.application.MaterialUploadUseCase;
import com.clipforge.material.infrastructure.persistence.entity.MaterialEntity;
import com.clipforge.material.infrastructure.persistence.entity.MaterialProcessTaskEntity;
import com.clipforge.material.infrastructure.persistence.repository.MaterialProcessTaskRepository;
import com.clipforge.material.infrastructure.persistence.repository.MaterialRepository;
import com.clipforge.material.FileStorage;
import com.clipforge.material.MaterialQueryCondition;
import com.clipforge.material.MaterialUploadProperties;
import com.clipforge.material.StoredFile;
import com.clipforge.material.model.MaterialType;
import com.clipforge.messaging.rocketmq.material.common.MaterialAsyncProcessProperties;
import com.clipforge.messaging.rocketmq.material.image.ImageUnderstandingMessage;
import com.clipforge.messaging.rocketmq.material.image.ImageUnderstandingProducer;
import com.clipforge.messaging.rocketmq.material.video.VideoProcessingMessage;
import com.clipforge.messaging.rocketmq.material.video.VideoProcessingProducer;
import com.clipforge.task.TaskBizType;
import com.clipforge.task.TaskNextAction;
import com.clipforge.task.TaskPhase;
import com.clipforge.task.TaskStatusValue;
import com.clipforge.task.TaskType;
import com.clipforge.material.task.cache.MaterialProcessTaskStatus;
import com.clipforge.material.task.cache.MaterialProcessTaskStatusCache;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * MaterialApplicationService（素材应用服务）。
 *
 * <p>该服务负责编排图片、视频等素材上传、文件存储和素材元数据落库。
 */
@Service
@RequiredArgsConstructor
public class MaterialApplicationService implements MaterialQueryUseCase, MaterialUploadUseCase {

    /** materialRepositoryProvider（素材仓储提供器）用于在 db（数据库）Profile（配置环境）启用时获取仓储。 */
    private final ObjectProvider<MaterialRepository> materialRepositoryProvider;

    /** materialProcessTaskRepositoryProvider（素材处理任务仓储提供器）用于在 db（数据库）Profile（配置环境）启用时获取仓储。 */
    private final ObjectProvider<MaterialProcessTaskRepository> materialProcessTaskRepositoryProvider;

    /** materialProcessTaskStatusCache（素材处理任务状态缓存）用于在上传后预热 Redis（缓存）任务状态。 */
    private final MaterialProcessTaskStatusCache materialProcessTaskStatusCache;

    /** fileStorage（文件存储）用于保存上传文件。 */
    private final FileStorage fileStorage;

    /** materialUploadProperties（素材上传配置）用于校验允许的素材类型和大小。 */
    private final MaterialUploadProperties materialUploadProperties;

    /** materialAsyncProcessProperties（素材异步处理配置）用于控制上传后是否自动投递处理消息。 */
    private final MaterialAsyncProcessProperties materialAsyncProcessProperties;

    /** imageUnderstandingProducer（图片理解生产者）用于发送图片异步理解消息。 */
    private final ImageUnderstandingProducer imageUnderstandingProducer;

    /** videoProcessingProducer（视频处理生产者）用于发送视频异步处理消息。 */
    private final VideoProcessingProducer videoProcessingProducer;

    /**
     * uploadImage（上传图片）用于保存图片文件和素材元数据。
     *
     * @param file 图片文件
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @return 素材响应
     */
    @Transactional(rollbackFor = Exception.class)
    public MaterialResponse uploadImage(MultipartFile file, String userId, String projectId, String conversationId, String runId) {
        // 调用带上传批次编号的重载方法，保持旧测试和旧调用点兼容。
        return uploadImage(file, userId, projectId, conversationId, runId, null);
    }

    /**
     * uploadImage（上传图片）用于保存图片文件和素材元数据，并透传前端上传批次编号。
     *
     * @param file 图片文件
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @param uploadBatchId 上传批次编号
     * @return 素材响应
     */
    @Transactional(rollbackFor = Exception.class)
    public MaterialResponse uploadImage(MultipartFile file, String userId, String projectId, String conversationId, String runId, String uploadBatchId) {
        // 转换为不依赖 Web 层类型的上传命令。
        return upload(toUploadCommand(file, userId, projectId, conversationId, runId, uploadBatchId, MaterialType.IMAGE));
    }

    /**
     * uploadVideo（上传视频）用于保存视频文件和素材元数据。
     *
     * @param file 视频文件
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @return 素材响应
     */
    @Transactional(rollbackFor = Exception.class)
    public MaterialResponse uploadVideo(MultipartFile file, String userId, String projectId, String conversationId, String runId) {
        // 调用带上传批次编号的重载方法，保持旧测试和旧调用点兼容。
        return uploadVideo(file, userId, projectId, conversationId, runId, null);
    }

    /**
     * uploadVideo（上传视频）用于保存视频文件和素材元数据，并透传前端上传批次编号。
     *
     * @param file 视频文件
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @param uploadBatchId 上传批次编号
     * @return 素材响应
     */
    @Transactional(rollbackFor = Exception.class)
    public MaterialResponse uploadVideo(MultipartFile file, String userId, String projectId, String conversationId, String runId, String uploadBatchId) {
        // 转换为不依赖 Web 层类型的上传命令。
        return upload(toUploadCommand(file, userId, projectId, conversationId, runId, uploadBatchId, MaterialType.VIDEO));
    }

    /**
     * upload（上传素材）是 Material 模块命令边界的实现入口。
     *
     * @param command 素材上传命令
     * @return 素材响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialResponse upload(MaterialUploadCommand command) {
        // 解析并校验素材类型。
        MaterialType materialType = requiredMaterialType(command);
        // 校验上传命令。
        validateUploadCommand(command, materialType);
        // 保存素材。
        return uploadMaterial(command, materialType);
    }

    /**
     * uploadMaterial（上传素材）用于复用图片和视频的文件保存与元数据落库逻辑。
     *
     * @param command 素材上传命令
     * @param materialType 素材类型
     * @return 素材响应
     */
    private MaterialResponse uploadMaterial(MaterialUploadCommand command, MaterialType materialType) {
        // 先获取仓储，未启用数据库时在保存文件前失败，避免产生孤儿文件。
        MaterialRepository materialRepository = requiredMaterialRepository();
        // 获取素材处理任务仓储。
        MaterialProcessTaskRepository processTaskRepository = requiredMaterialProcessTaskRepository();
        // 保存文件到本地存储。
        StoredFile storedFile = fileStorage.store(command);
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 构造素材实体。
        MaterialEntity entity = MaterialEntity.builder()
                // 设置数据库主键。
                .id(newId("material_pk"))
                // 设置素材编号。
                .materialId(newId("material"))
                // 设置用户编号。
                .userId(blankToNull(command.getUserId()))
                // 设置项目编号。
                .projectId(blankToNull(command.getProjectId()))
                // 设置会话编号。
                .conversationId(blankToNull(command.getConversationId()))
                // 设置运行编号。
                .runId(blankToNull(command.getRunId()))
                // 设置素材类型。
                .materialType(materialType.value())
                // 设置原始文件名。
                .originalFilename(defaultOriginalFilename(command.getOriginalFilename(), storedFile.getStoredFilename()))
                // 设置存储文件名。
                .storedFilename(storedFile.getStoredFilename())
                // 设置文件路径。
                .filePath(storedFile.getFilePath())
                // 设置内容类型。
                .contentType(command.getContentType())
                // 设置文件大小。
                .fileSize(command.getFileSize())
                // 设置状态为已上传。
                .status("UPLOADED")
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记。
                .deleted(0)
                // 构建实体。
                .build();
        // 保存素材实体。
        MaterialEntity savedEntity = materialRepository.save(entity);
        // 为本次上传创建素材处理任务。
        MaterialProcessTaskEntity processTask = processTaskRepository.save(buildInitialProcessTask(savedEntity, blankToNull(command.getUploadBatchId()), now));
        // 尝试把初始处理任务状态写入 Redis（缓存），失败不影响上传主流程。
        safeSaveProcessTaskStatusCache(processTask);
        // 注册事务提交后的异步处理消息投递，避免消费者读到未提交的素材和任务。
        registerDispatchAfterCommit(savedEntity, processTask, blankToNull(command.getUploadBatchId()), materialType);
        // 转换为响应对象。
        return toMaterialResponse(savedEntity, processTask, blankToNull(command.getUploadBatchId()));
    }

    /**
     * registerDispatchAfterCommit（注册提交后投递）用于在素材和任务真正落库后发送 RocketMQ（消息队列）处理消息。
     *
     * @param material 素材实体
     * @param processTask 处理任务实体
     * @param uploadBatchId 上传批次编号
     * @param materialType 素材类型
     */
    private void registerDispatchAfterCommit(MaterialEntity material, MaterialProcessTaskEntity processTask, String uploadBatchId, MaterialType materialType) {
        // 没有启用素材异步处理或上传后自动投递时，不发送消息，保持手动处理接口可用。
        if (!materialAsyncProcessProperties.isEnabled() || !materialAsyncProcessProperties.isAutoDispatchOnUpload()) {
            // 直接返回。
            return;
        }
        // 构造真正投递逻辑。
        Runnable dispatchAction = () -> dispatchMaterialProcessMessage(material, processTask, uploadBatchId, materialType);
        // 如果当前处于事务中，则注册 afterCommit（提交后）回调。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 注册事务同步回调。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                /**
                 * afterCommit（提交后）用于保证数据库提交成功之后再投递消息。
                 */
                @Override
                public void afterCommit() {
                    // 执行消息投递。
                    dispatchAction.run();
                }
            });
            // 注册完成后返回。
            return;
        }
        // 没有事务时直接投递，方便单元测试和少数非事务调用场景。
        dispatchAction.run();
    }

    /**
     * dispatchMaterialProcessMessage（投递素材处理消息）用于根据素材类型选择图片或视频处理主题。
     *
     * @param material 素材实体
     * @param processTask 处理任务实体
     * @param uploadBatchId 上传批次编号
     * @param materialType 素材类型
     */
    private void dispatchMaterialProcessMessage(MaterialEntity material, MaterialProcessTaskEntity processTask, String uploadBatchId, MaterialType materialType) {
        try {
            // 图片素材投递图片理解消息。
            if (materialType == MaterialType.IMAGE) {
                // 发送图片理解消息。
                imageUnderstandingProducer.send(buildImageUnderstandingMessage(material, processTask, uploadBatchId));
                // 投递完成后返回。
                return;
            }
            // 视频素材投递视频处理消息。
            videoProcessingProducer.send(buildVideoProcessingMessage(material, processTask, uploadBatchId));
        } catch (RuntimeException exception) {
            // MQ（消息队列）投递失败时不回滚上传，而是把处理任务标记成可重试失败。
            markAsyncDispatchFailed(processTask, exception);
        }
    }

    /**
     * buildImageUnderstandingMessage（构建图片理解消息）用于发送单张图片异步理解任务。
     *
     * @param material 素材实体
     * @param processTask 处理任务实体
     * @param uploadBatchId 上传批次编号
     * @return 图片理解消息
     */
    private ImageUnderstandingMessage buildImageUnderstandingMessage(MaterialEntity material, MaterialProcessTaskEntity processTask, String uploadBatchId) {
        // 使用 Builder（建造者模式）组装图片理解消息。
        return ImageUnderstandingMessage.builder()
                // 设置消息编号。
                .messageId(newId("material_image_message"))
                // 设置处理任务编号。
                .processTaskId(processTask.getProcessTaskId())
                // 设置素材编号。
                .materialId(material.getMaterialId())
                // 设置用户编号。
                .userId(material.getUserId())
                // 设置项目编号。
                .projectId(material.getProjectId())
                // 设置会话编号。
                .conversationId(material.getConversationId())
                // 设置运行编号。
                .runId(material.getRunId())
                // 设置上传批次编号。
                .uploadBatchId(uploadBatchId)
                // 设置默认图片理解指令。
                .instruction(materialAsyncProcessProperties.getDefaultImageInstruction())
                // 设置创建时间。
                .createdAt(LocalDateTime.now())
                // 构建消息。
                .build();
    }

    /**
     * buildVideoProcessingMessage（构建视频处理消息）用于发送单个视频异步处理任务。
     *
     * @param material 素材实体
     * @param processTask 处理任务实体
     * @param uploadBatchId 上传批次编号
     * @return 视频处理消息
     */
    private VideoProcessingMessage buildVideoProcessingMessage(MaterialEntity material, MaterialProcessTaskEntity processTask, String uploadBatchId) {
        // 使用 Builder（建造者模式）组装视频处理消息。
        return VideoProcessingMessage.builder()
                // 设置消息编号。
                .messageId(newId("material_video_message"))
                // 设置处理任务编号。
                .processTaskId(processTask.getProcessTaskId())
                // 设置素材编号。
                .materialId(material.getMaterialId())
                // 设置用户编号。
                .userId(material.getUserId())
                // 设置项目编号。
                .projectId(material.getProjectId())
                // 设置会话编号。
                .conversationId(material.getConversationId())
                // 设置运行编号。
                .runId(material.getRunId())
                // 设置上传批次编号。
                .uploadBatchId(uploadBatchId)
                // 设置默认视频理解指令。
                .instruction(materialAsyncProcessProperties.getDefaultVideoInstruction())
                // 设置创建时间。
                .createdAt(LocalDateTime.now())
                // 构建消息。
                .build();
    }

    /**
     * markAsyncDispatchFailed（标记异步投递失败）用于在消息发送失败时更新任务状态并刷新缓存。
     *
     * @param processTask 处理任务实体
     * @param exception 投递异常
     */
    private void markAsyncDispatchFailed(MaterialProcessTaskEntity processTask, RuntimeException exception) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 设置失败状态。
        processTask.setStatus(TaskStatusValue.FAILED.value());
        // 设置失败阶段。
        processTask.setPhase(TaskPhase.FAILED.value());
        // 设置完成进度，表示本次处理链路已经结束在失败态。
        processTask.setProgressPercent(100);
        // 设置状态说明。
        processTask.setMessage("素材处理消息发送失败");
        // 设置异步处理不可用错误码。
        processTask.setErrorCode(ErrorCode.ASYNC_PROCESS_UNAVAILABLE.getCode());
        // 设置错误消息。
        processTask.setErrorMessage(exception.getMessage());
        // 设置允许重试。
        processTask.setCanRetry(1);
        // 设置下一步动作。
        processTask.setNextAction(TaskNextAction.RETRY_PROCESSING.value());
        // 设置更新时间。
        processTask.setUpdatedAt(now);
        // 设置结束时间。
        processTask.setEndedAt(now);
        // 设置数据库更新时间。
        processTask.setUpdateTime(now);
        // 更新数据库任务表。
        requiredMaterialProcessTaskRepository().update(processTask);
        // 刷新 Redis（缓存）任务状态。
        safeSaveProcessTaskStatusCache(processTask);
    }

    /**
     * listMaterials（查询素材列表）用于按用户、项目、会话、运行任务和素材类型读取素材集合。
     *
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @param materialType 素材类型
     * @return 素材响应列表
     */
    @Override
    public List<MaterialResponse> listMaterials(String userId, String projectId, String conversationId, String runId, String materialType) {
        // 构造素材查询条件，空白参数会统一转为 null，避免查询层处理无意义空字符串。
        MaterialQueryCondition condition = MaterialQueryCondition.builder()
                // 设置用户编号。
                .userId(blankToNull(userId))
                // 设置项目编号。
                .projectId(blankToNull(projectId))
                // 设置会话编号。
                .conversationId(blankToNull(conversationId))
                // 设置运行编号。
                .runId(blankToNull(runId))
                // 设置素材类型。
                .materialType(blankToNull(materialType))
                // 构建查询条件。
                .build();
        // 查询素材实体列表。
        List<MaterialEntity> entities = requiredMaterialRepository().listByCondition(condition);
        // 把素材实体列表转换为接口响应列表。
        return entities.stream()
                // 每个实体转换为 MaterialResponse（素材响应）。
                .map(this::toMaterialResponse)
                // 默认不向前端暴露系统派生的帧素材。
                .filter(this::isUserVisibleMaterial)
                // 收集为 List（列表）。
                .toList();
    }

    /**
     * listMaterialsByIds（按素材编号列表查询素材）用于读取用户明确选择的素材集合。
     *
     * @param materialIds 素材编号列表
     * @param materialType 素材类型，例如 IMAGE（图片）或 VIDEO（视频）
     * @return 素材响应列表
     */
    @Override
    public List<MaterialResponse> listMaterialsByIds(List<String> materialIds, String materialType) {
        // 清洗素材编号列表，去掉空白编号。
        List<String> safeMaterialIds = safeMaterialIds(materialIds);
        // 如果清洗后为空，则直接返回空列表。
        if (safeMaterialIds.isEmpty()) {
            // 返回空列表。
            return List.of();
        }
        // 查询素材实体列表。
        List<MaterialEntity> entities = requiredMaterialRepository().listByMaterialIds(safeMaterialIds, blankToNull(materialType));
        // 按用户传入顺序重新排序，避免数据库 in（包含）查询返回顺序不稳定。
        return safeMaterialIds.stream()
                // 按素材编号查找实体。
                .flatMap(materialId -> entities.stream().filter(entity -> materialId.equals(entity.getMaterialId())).findFirst().stream())
                // 转换为素材响应。
                .map(this::toMaterialResponse)
                // 收集为列表。
                .toList();
    }

    /**
     * queryMaterial（查询素材）用于按素材编号读取素材元数据。
     *
     * @param materialId 素材编号
     * @return 素材响应
     */
    @Override
    public MaterialResponse queryMaterial(String materialId) {
        // 查询素材实体。
        MaterialEntity entity = requiredMaterialRepository().findByMaterialId(materialId)
                // 找不到时抛出数据不存在异常。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到素材，materialId：" + materialId));
        // 系统派生的帧素材不对普通用户接口暴露。
        if (!isUserVisibleMaterial(entity)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到素材，或素材不对前端可见，materialId：" + materialId);
        }
        // 转换为响应。
        return toMaterialResponse(entity);
    }

    /**
     * toUploadCommand（转上传命令）用于把 Web 层 MultipartFile 转为 Material 模块命令对象。
     *
     * @param file 上传文件
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @param uploadBatchId 上传批次编号
     * @param materialType 素材类型
     * @return 素材上传命令
     */
    private MaterialUploadCommand toUploadCommand(MultipartFile file, String userId, String projectId, String conversationId,
                                                  String runId, String uploadBatchId, MaterialType materialType) {
        // MultipartFile 为空时仍构造命令交给统一校验，保证错误消息一致。
        if (file == null) {
            return MaterialUploadCommand.builder()
                    .materialType(materialType.value())
                    .userId(userId)
                    .projectId(projectId)
                    .conversationId(conversationId)
                    .runId(runId)
                    .uploadBatchId(uploadBatchId)
                    .build();
        }
        // 构造不依赖 Web 类型的上传命令。
        return MaterialUploadCommand.builder()
                .materialType(materialType.value())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .userId(userId)
                .projectId(projectId)
                .conversationId(conversationId)
                .runId(runId)
                .uploadBatchId(uploadBatchId)
                .content(file::getInputStream)
                .build();
    }

    /**
     * requiredMaterialType（读取必需素材类型）用于校验上传命令中的素材类型。
     *
     * @param command 素材上传命令
     * @return 素材类型
     */
    private MaterialType requiredMaterialType(MaterialUploadCommand command) {
        // 校验命令不能为空。
        if (command == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "素材上传命令不能为空");
        }
        // 校验素材类型不能为空。
        if (command.getMaterialType() == null || command.getMaterialType().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "素材类型不能为空");
        }
        // 标准化素材类型。
        String materialType = command.getMaterialType().strip().toUpperCase();
        // 图片素材。
        if (MaterialType.IMAGE.value().equals(materialType)) {
            return MaterialType.IMAGE;
        }
        // 视频素材。
        if (MaterialType.VIDEO.value().equals(materialType)) {
            return MaterialType.VIDEO;
        }
        // 其他类型当前上传接口不支持。
        throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的素材类型：" + command.getMaterialType());
    }

    /**
     * validateUploadCommand（校验上传命令）用于根据素材类型检查文件内容、内容类型和大小。
     *
     * @param command 素材上传命令
     * @param materialType 素材类型
     */
    private void validateUploadCommand(MaterialUploadCommand command, MaterialType materialType) {
        // 文件内容流不能为空。
        if (command.getContent() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, materialTypeText(materialType) + "文件不能为空");
        }
        // 文件大小不能为空或小于等于 0。
        if (command.getFileSize() == null || command.getFileSize() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, materialTypeText(materialType) + "文件不能为空");
        }
        // 读取内容类型。
        String contentType = command.getContentType();
        // 如果内容类型不在白名单中，则拒绝请求。
        if (!allowedContentTypes(materialType).contains(contentType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, allowedTypeMessage(materialType));
        }
        // 如果文件大小超过当前素材类型限制，则拒绝请求。
        if (command.getFileSize() > maxFileSizeBytes(materialType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, materialTypeText(materialType) + "文件大小超过限制，最大允许 " + maxFileSizeBytes(materialType) + " 字节");
        }
    }

    /**
     * validateFile（校验文件）用于根据素材类型检查文件非空、内容类型和文件大小。
     *
     * @param file 上传文件
     * @param materialType 素材类型
     */
    private void validateFile(MultipartFile file, MaterialType materialType) {
        // 如果文件为空引用，则拒绝请求。
        if (file == null) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, materialTypeText(materialType) + "文件不能为空");
        }
        // 如果文件内容为空，则拒绝请求。
        if (file.isEmpty()) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, materialTypeText(materialType) + "文件不能为空");
        }
        // 读取内容类型。
        String contentType = file.getContentType();
        // 如果内容类型不在白名单中，则拒绝请求。
        if (!allowedContentTypes(materialType).contains(contentType)) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, allowedTypeMessage(materialType));
        }
        // 如果文件大小超过当前素材类型限制，则拒绝请求。
        if (file.getSize() > maxFileSizeBytes(materialType)) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, materialTypeText(materialType) + "文件大小超过限制，最大允许 " + maxFileSizeBytes(materialType) + " 字节");
        }
    }

    /**
     * allowedContentTypes（允许内容类型）用于按素材类型读取 MIME（媒体类型）白名单。
     *
     * @param materialType 素材类型
     * @return 内容类型白名单
     */
    private List<String> allowedContentTypes(MaterialType materialType) {
        // 视频使用视频白名单。
        if (materialType == MaterialType.VIDEO) {
            // 返回视频内容类型白名单。
            return materialUploadProperties.getAllowedVideoContentTypes();
        }
        // 默认返回图片内容类型白名单。
        return materialUploadProperties.getAllowedImageContentTypes();
    }

    /**
     * maxFileSizeBytes（最大文件字节数）用于按素材类型读取大小限制。
     *
     * @param materialType 素材类型
     * @return 最大文件字节数
     */
    private long maxFileSizeBytes(MaterialType materialType) {
        // 视频使用视频文件大小限制。
        if (materialType == MaterialType.VIDEO) {
            // 返回视频大小限制。
            return materialUploadProperties.getMaxVideoFileSizeBytes();
        }
        // 默认返回图片大小限制。
        return materialUploadProperties.getMaxImageFileSizeBytes();
    }

    /**
     * allowedTypeMessage（允许类型提示）用于生成不同素材类型的错误提示。
     *
     * @param materialType 素材类型
     * @return 错误提示
     */
    private String allowedTypeMessage(MaterialType materialType) {
        // 视频类型错误提示。
        if (materialType == MaterialType.VIDEO) {
            // 返回视频格式提示。
            return "仅支持 mp4、mov、webm 视频";
        }
        // 返回图片格式提示。
        return "仅支持 jpg、jpeg、png、webp 图片";
    }

    /**
     * materialTypeText（素材类型中文名）用于组装错误提示。
     *
     * @param materialType 素材类型
     * @return 素材类型中文名
     */
    private String materialTypeText(MaterialType materialType) {
        // 视频返回中文名。
        if (materialType == MaterialType.VIDEO) {
            // 返回视频。
            return "视频";
        }
        // 默认返回图片。
        return "图片";
    }

    /**
     * isUserVisibleMaterial（是否对用户可见）用于过滤系统派生的帧素材。
     *
     * @param materialResponse 素材响应
     * @return 是否可见
     */
    private boolean isUserVisibleMaterial(MaterialResponse materialResponse) {
        if (materialResponse == null) {
            return false;
        }
        return isUserVisibleMaterial(materialResponse.getMaterialId(), materialResponse.getOriginalFilename());
    }

    /**
     * isUserVisibleMaterial（是否对用户可见）用于过滤系统派生的帧素材。
     *
     * @param entity 素材实体
     * @return 是否可见
     */
    private boolean isUserVisibleMaterial(MaterialEntity entity) {
        if (entity == null) {
            return false;
        }
        return isUserVisibleMaterial(entity.getMaterialId(), entity.getOriginalFilename());
    }

    /**
     * isUserVisibleMaterial（是否对用户可见）用于过滤系统派生的帧素材。
     *
     * @param materialId 素材编号
     * @param originalFilename 原始文件名
     * @return 是否可见
     */
    private boolean isUserVisibleMaterial(String materialId, String originalFilename) {
        if (materialId != null && materialId.startsWith("material_frame_")) {
            return false;
        }
        if (originalFilename != null && originalFilename.startsWith("视频帧")) {
            return false;
        }
        return true;
    }

    /**
     * requiredMaterialRepository（获取必需素材仓储）用于处理未启用数据库的场景。
     *
     * @return 素材仓储
     */
    private MaterialRepository requiredMaterialRepository() {
        // 从 Spring（Spring 框架）容器中获取素材仓储。
        MaterialRepository repository = materialRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "素材上传接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * requiredMaterialProcessTaskRepository（获取必需素材处理任务仓储）用于处理未启用数据库的场景。
     *
     * @return 素材处理任务仓储
     */
    private MaterialProcessTaskRepository requiredMaterialProcessTaskRepository() {
        // 从 Spring（Spring 框架）容器中获取素材处理任务仓储。
        MaterialProcessTaskRepository repository = materialProcessTaskRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "素材处理任务需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * toMaterialResponse（转素材响应）用于把数据库实体转换为接口响应对象。
     *
     * @param entity 素材实体
     * @return 素材响应
     */
    private MaterialResponse toMaterialResponse(MaterialEntity entity) {
        // 获取素材处理任务仓储，非 db（数据库）环境可能为空。
        MaterialProcessTaskRepository processTaskRepository = materialProcessTaskRepositoryProvider.getIfAvailable();
        // 尝试读取素材最近一次处理任务，历史数据没有任务记录时会走派生兜底。
        Optional<MaterialProcessTaskEntity> processTask = processTaskRepository == null
                // 没有任务仓储时返回空可选值。
                ? Optional.empty()
                // 有任务仓储时按素材编号查询最近任务。
                : processTaskRepository.findLatestByMaterialId(entity.getMaterialId());
        // 查询和列表接口没有上传批次入参，因此批次编号为空。
        return toMaterialResponse(entity, processTask.orElse(null), null);
    }

    /**
     * toMaterialResponse（转素材响应）用于把数据库实体转换为接口响应对象，并补充前端处理任务视图。
     *
     * @param entity 素材实体
     * @param uploadBatchId 上传批次编号
     * @return 素材响应
     */
    private MaterialResponse toMaterialResponse(MaterialEntity entity, MaterialProcessTaskEntity processTask, String uploadBatchId) {
        // 生成当前阶段的处理任务编号，优先使用真实任务表编号。
        String processTaskId = processTask == null ? processTaskId(entity.getMaterialId()) : processTask.getProcessTaskId();
        // 计算面向前端的处理状态，优先使用真实任务表状态。
        String processStatus = processTask == null ? processStatus(entity.getStatus()) : processTask.getStatus();
        // 计算面向前端的处理阶段，优先使用真实任务表阶段。
        String processPhase = processTask == null ? processPhase(entity.getMaterialType(), entity.getStatus()) : processTask.getPhase();
        // 计算进度百分比。
        Integer progressPercent = processTask == null ? progressPercent(processStatus) : processTask.getProgressPercent();
        // 判断素材是否可以直接进入创作链路。
        Boolean canUseForCreation = canUseForCreation(processStatus, processPhase);
        // 计算下一步动作。
        String nextAction = processTask == null ? nextAction(processStatus, canUseForCreation) : processTask.getNextAction();
        // 查询接口如果没有上传批次入参，则优先展示任务表批次编号。
        String responseUploadBatchId = uploadBatchId == null && processTask != null ? processTask.getUploadBatchId() : uploadBatchId;
        // 使用 Builder（建造者模式）组装素材响应。
        return MaterialResponse.builder()
                // 设置素材编号。
                .materialId(entity.getMaterialId())
                // 设置用户编号。
                .userId(entity.getUserId())
                // 设置项目编号。
                .projectId(entity.getProjectId())
                // 设置会话编号。
                .conversationId(entity.getConversationId())
                // 设置运行编号。
                .runId(entity.getRunId())
                // 设置素材类型。
                .materialType(entity.getMaterialType())
                // 设置原始文件名。
                .originalFilename(entity.getOriginalFilename())
                // 设置存储文件名。
                .storedFilename(entity.getStoredFilename())
                // 设置文件路径。
                .filePath(entity.getFilePath())
                // 设置内容类型。
                .contentType(entity.getContentType())
                // 设置文件大小。
                .fileSize(entity.getFileSize())
                // 设置状态。
                .status(entity.getStatus())
                // 设置上传批次编号。
                .uploadBatchId(responseUploadBatchId)
                // 设置处理任务编号。
                .processTaskId(processTaskId)
                // 设置处理状态。
                .processStatus(processStatus)
                // 设置处理阶段。
                .processPhase(processPhase)
                // 设置进度百分比。
                .progressPercent(progressPercent)
                // 设置是否可用于创作。
                .canUseForCreation(canUseForCreation)
                // 设置状态查询地址。
                .statusQueryUrl("/api/tasks/" + processTaskId + "/status")
                // 设置下一步动作。
                .nextAction(nextAction)
                // 构建响应。
                .build();
    }

    /**
     * processTaskId（处理任务编号）用于在真实任务表落地前，根据素材编号生成稳定的前端占位任务编号。
     *
     * @param materialId 素材编号
     * @return 处理任务编号
     */
    private String processTaskId(String materialId) {
        // 如果素材编号为空，返回空引用，避免异常扩散。
        if (materialId == null || materialId.isBlank()) {
            // 返回空引用。
            return null;
        }
        // 使用素材编号生成稳定处理任务编号，当前会同步写入真实素材处理任务表。
        return "process_task_" + materialId.replaceFirst("^material_", "");
    }

    /**
     * buildInitialProcessTask（构建初始处理任务）用于在素材上传完成后创建 PENDING（等待中）任务。
     *
     * @param entity 素材实体
     * @param uploadBatchId 上传批次编号
     * @param now 当前时间
     * @return 素材处理任务实体
     */
    private MaterialProcessTaskEntity buildInitialProcessTask(MaterialEntity entity, String uploadBatchId, LocalDateTime now) {
        // 计算任务阶段。
        String phase = initialPhase(entity.getMaterialType());
        // 构建素材处理任务实体。
        return MaterialProcessTaskEntity.builder()
                // 设置数据库主键。
                .id(newId("material_process_task_pk"))
                // 设置处理任务编号。
                .processTaskId(processTaskId(entity.getMaterialId()))
                // 设置素材编号。
                .materialId(entity.getMaterialId())
                // 设置上传批次编号。
                .uploadBatchId(uploadBatchId)
                // 设置用户编号。
                .userId(entity.getUserId())
                // 设置项目编号。
                .projectId(entity.getProjectId())
                // 设置会话编号。
                .conversationId(entity.getConversationId())
                // 设置运行编号。
                .runId(entity.getRunId())
                // 设置任务类型。
                .taskType(initialTaskType(entity.getMaterialType()))
                // 设置业务类型。
                .bizType(TaskBizType.MATERIAL.value())
                // 设置初始任务状态为等待中。
                .status(TaskStatusValue.PENDING.value())
                // 设置初始处理阶段。
                .phase(phase)
                // 设置初始进度。
                .progressPercent(0)
                // 设置状态说明。
                .message(initialMessage(phase))
                // 设置重试次数。
                .retryCount(0)
                // 等待中任务暂不允许重试。
                .canRetry(0)
                // 设置下一步动作。
                .nextAction(TaskNextAction.WAIT_PROCESSING.value())
                // 等待中尚未真正开始执行。
                .startedAt(null)
                // 设置状态更新时间。
                .updatedAt(now)
                // 等待中尚未结束。
                .endedAt(null)
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记。
                .deleted(0)
                // 构建实体。
                .build();
    }

    /**
     * safeSaveProcessTaskStatusCache（安全保存处理任务状态缓存）用于上传后预热 Redis（缓存）。
     *
     * @param entity 素材处理任务实体
     */
    private void safeSaveProcessTaskStatusCache(MaterialProcessTaskEntity entity) {
        try {
            // 写入素材处理任务状态缓存。
            materialProcessTaskStatusCache.save(toCacheStatus(entity));
        } catch (RuntimeException exception) {
            // 缓存失败不影响素材上传主流程。
        }
    }

    /**
     * toCacheStatus（转缓存状态）用于把素材处理任务实体转换为 Redis（缓存）内部对象。
     *
     * @param entity 素材处理任务实体
     * @return 素材处理任务缓存状态
     */
    private MaterialProcessTaskStatus toCacheStatus(MaterialProcessTaskEntity entity) {
        // 使用 Builder（建造者模式）组装缓存对象。
        return MaterialProcessTaskStatus.builder()
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
                // 构建缓存对象。
                .build();
    }

    /**
     * initialTaskType（初始任务类型）用于根据素材类型决定任务类型。
     *
     * @param materialType 素材类型
     * @return 任务类型
     */
    private String initialTaskType(String materialType) {
        // 视频素材对应视频处理任务。
        if (MaterialType.VIDEO.value().equals(materialType)) {
            // 返回视频处理任务类型。
            return TaskType.MATERIAL_VIDEO_PROCESSING.value();
        }
        // 默认返回图片理解任务类型。
        return TaskType.MATERIAL_IMAGE_UNDERSTANDING.value();
    }

    /**
     * initialPhase（初始处理阶段）用于根据素材类型决定等待阶段。
     *
     * @param materialType 素材类型
     * @return 初始处理阶段
     */
    private String initialPhase(String materialType) {
        // 视频素材等待视频处理流水线。
        if (MaterialType.VIDEO.value().equals(materialType)) {
            // 返回等待视频处理阶段。
            return TaskPhase.WAITING_VIDEO_PROCESSING.value();
        }
        // 默认等待图片理解。
        return TaskPhase.WAITING_IMAGE_UNDERSTANDING.value();
    }

    /**
     * initialMessage（初始状态说明）用于生成等待处理时的人类可读提示。
     *
     * @param phase 处理阶段
     * @return 状态说明
     */
    private String initialMessage(String phase) {
        // 图片等待说明。
        if (TaskPhase.WAITING_IMAGE_UNDERSTANDING.value().equals(phase)) {
            // 返回图片说明。
            return "图片已上传，等待图片理解处理";
        }
        // 返回视频说明。
        return "视频已上传，等待视频处理流水线";
    }

    /**
     * processStatus（处理状态）用于把素材状态映射为前端统一处理状态。
     *
     * @param materialStatus 素材状态
     * @return 处理状态
     */
    private String processStatus(String materialStatus) {
        // 如果素材状态为空，按等待处理兜底。
        if (materialStatus == null || materialStatus.isBlank()) {
            // 返回等待中。
            return "PENDING";
        }
        // 标准化状态。
        String normalizedStatus = materialStatus.strip().toUpperCase();
        // 已理解或可用表示处理成功。
        if ("UNDERSTOOD".equals(normalizedStatus) || "READY".equals(normalizedStatus) || "READY_FOR_CREATION".equals(normalizedStatus)) {
            // 返回成功。
            return TaskStatusValue.SUCCEEDED.value();
        }
        // 失败状态映射为失败。
        if ("FAILED".equals(normalizedStatus)) {
            // 返回失败。
            return TaskStatusValue.FAILED.value();
        }
        // 理解中或处理中映射为处理中。
        if ("UNDERSTANDING".equals(normalizedStatus) || "PROCESSING".equals(normalizedStatus)) {
            // 返回处理中。
            return TaskStatusValue.PROCESSING.value();
        }
        // UPLOADED（已上传）默认表示等待后续处理。
        return TaskStatusValue.PENDING.value();
    }

    /**
     * processPhase（处理阶段）用于根据素材类型和素材状态生成前端粗粒度阶段。
     *
     * @param materialType 素材类型
     * @param materialStatus 素材状态
     * @return 处理阶段
     */
    private String processPhase(String materialType, String materialStatus) {
        // 先计算统一处理状态。
        String status = processStatus(materialStatus);
        // 成功状态直接表示可用于创作。
        if ("SUCCEEDED".equals(status)) {
            // 返回可创作阶段。
            return TaskPhase.READY_FOR_CREATION.value();
        }
        // 失败状态直接表示处理失败。
        if ("FAILED".equals(status)) {
            // 返回失败阶段。
            return TaskPhase.FAILED.value();
        }
        // 视频素材等待进入视频处理流水线。
        if (MaterialType.VIDEO.value().equals(materialType)) {
            // 返回等待视频处理。
            return TaskPhase.WAITING_VIDEO_PROCESSING.value();
        }
        // 图片素材等待进入图片理解。
        return TaskPhase.WAITING_IMAGE_UNDERSTANDING.value();
    }

    /**
     * progressPercent（进度百分比）用于按统一处理状态生成前端粗粒度进度。
     *
     * @param processStatus 处理状态
     * @return 进度百分比
     */
    private Integer progressPercent(String processStatus) {
        // 成功状态表示 100%。
        if (TaskStatusValue.SUCCEEDED.value().equals(processStatus)) {
            // 返回完成进度。
            return 100;
        }
        // 处理中给出中间进度。
        if (TaskStatusValue.PROCESSING.value().equals(processStatus)) {
            // 返回处理中进度。
            return 50;
        }
        // 失败状态保留当前粗粒度进度。
        if (TaskStatusValue.FAILED.value().equals(processStatus)) {
            // 返回失败前进度。
            return 0;
        }
        // 等待中返回 0%。
        return 0;
    }

    /**
     * canUseForCreation（是否可用于创作）用于判断素材处理结果是否已经准备好。
     *
     * @param processStatus 处理状态
     * @param processPhase 处理阶段
     * @return 是否可用于创作
     */
    private Boolean canUseForCreation(String processStatus, String processPhase) {
        // 只有处理成功且阶段为可创作，才建议前端允许用户勾选进入创作。
        return TaskStatusValue.SUCCEEDED.value().equals(processStatus) && TaskPhase.READY_FOR_CREATION.value().equals(processPhase);
    }

    /**
     * nextAction（下一步动作）用于给前端提供简单动作提示。
     *
     * @param processStatus 处理状态
     * @param canUseForCreation 是否可用于创作
     * @return 下一步动作
     */
    private String nextAction(String processStatus, Boolean canUseForCreation) {
        // 已可用于创作时提示允许创作。
        if (Boolean.TRUE.equals(canUseForCreation)) {
            // 返回允许创作。
            return TaskNextAction.ALLOW_CREATION.value();
        }
        // 失败时提示重试处理。
        if (TaskStatusValue.FAILED.value().equals(processStatus)) {
            // 返回重试处理。
            return TaskNextAction.RETRY_PROCESSING.value();
        }
        // 其他状态提示等待处理。
        return TaskNextAction.WAIT_PROCESSING.value();
    }

    /**
     * blankToNull（空白转空引用）用于避免数据库保存无意义空字符串。
     *
     * @param value 原始字符串
     * @return 空白时返回 null，否则返回原值
     */
    private String blankToNull(String value) {
        // 如果字符串为空或空白，则返回 null。
        if (value == null || value.isBlank()) {
            // 返回空引用。
            return null;
        }
        // 返回原值。
        return value;
    }

    /**
     * safeMaterialIds（安全素材编号列表）用于去除空白素材编号并保留用户选择顺序。
     *
     * @param materialIds 原始素材编号列表
     * @return 清洗后的素材编号列表
     */
    private List<String> safeMaterialIds(List<String> materialIds) {
        // 如果原始列表为空，则返回空列表。
        if (materialIds == null || materialIds.isEmpty()) {
            // 返回空列表。
            return List.of();
        }
        // 去除空白编号，并按首次出现顺序去重。
        return materialIds.stream()
                // 过滤空引用。
                .filter(value -> value != null)
                // 去除首尾空白。
                .map(String::strip)
                // 过滤空白字符串。
                .filter(value -> !value.isBlank())
                // 按顺序去重。
                .distinct()
                // 收集为列表。
                .toList();
    }

    /**
     * defaultOriginalFilename（默认原始文件名）用于处理客户端未传原始文件名的少数情况。
     *
     * @param originalFilename 客户端上传时携带的原始文件名
     * @param storedFilename 系统保存时生成的文件名
     * @return 可落库的原始文件名
     */
    private String defaultOriginalFilename(String originalFilename, String storedFilename) {
        // 如果客户端原始文件名为空，则使用系统生成的存储文件名兜底，避免数据库非空字段写入失败。
        if (originalFilename == null || originalFilename.isBlank()) {
            // 返回存储文件名作为兜底值。
            return storedFilename;
        }
        // 返回客户端传入的原始文件名。
        return originalFilename;
    }

    /**
     * newId（生成编号）用于生成带业务前缀的随机编号。
     *
     * @param prefix 编号前缀
     * @return 随机编号
     */
    private String newId(String prefix) {
        // 使用 UUID（通用唯一识别码）生成本地开发阶段足够唯一的编号。
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
