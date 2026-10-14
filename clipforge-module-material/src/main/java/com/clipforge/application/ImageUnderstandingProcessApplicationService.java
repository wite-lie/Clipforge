package com.clipforge.application;

import com.clipforge.contract.material.MaterialResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.material.infrastructure.persistence.entity.MaterialEntity;
import com.clipforge.material.infrastructure.persistence.entity.MaterialProcessTaskEntity;
import com.clipforge.material.infrastructure.persistence.entity.MaterialUnderstandingEntity;
import com.clipforge.material.infrastructure.persistence.repository.MaterialProcessTaskRepository;
import com.clipforge.material.infrastructure.persistence.repository.MaterialRepository;
import com.clipforge.material.infrastructure.persistence.repository.MaterialUnderstandingRepository;
import com.clipforge.material.MaterialContentHashCalculator;
import com.clipforge.material.understanding.MaterialUnderstandingStatus;
import com.clipforge.material.understanding.MaterialUnderstandingType;
import com.clipforge.ai.api.vision.ImageUnderstandingResult;
import com.clipforge.ai.infrastructure.vision.VisionModelProperties;
import com.clipforge.ai.api.vision.VisionModelResponse;
import com.clipforge.task.TaskBizType;
import com.clipforge.task.TaskNextAction;
import com.clipforge.task.TaskPhase;
import com.clipforge.task.TaskStatusValue;
import com.clipforge.material.task.cache.MaterialProcessTaskStatus;
import com.clipforge.material.task.cache.MaterialProcessTaskStatusCache;
import com.clipforge.tool.ImageUnderstandingTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ImageUnderstandingProcessApplicationService（图片理解处理应用服务）。
 *
 * <p>该服务负责“处理期”的图片理解：读取素材、计算内容哈希、复用历史结果、必要时调用 VLM（视觉语言模型），并把结果保存到 cf_material_understanding（素材理解结果表）。
 */
@Service
@Profile("db")
@RequiredArgsConstructor
public class ImageUnderstandingProcessApplicationService {

    /** PROMPT_VERSION（提示词版本）用于判断历史图片理解结果是否可复用。 */
    private static final String PROMPT_VERSION = "image-understanding-v1";

    /** IMAGE_MATERIAL_TYPE（图片素材类型）用于校验当前服务只处理图片素材。 */
    private static final String IMAGE_MATERIAL_TYPE = "IMAGE";

    /** materialRepository（素材仓储）用于读取素材元数据。 */
    private final MaterialRepository materialRepository;

    /** processTaskRepository（处理任务仓储）用于更新素材处理任务状态。 */
    private final MaterialProcessTaskRepository processTaskRepository;

    /** materialUnderstandingRepository（素材理解结果仓储）用于保存和查询图片理解结果。 */
    private final MaterialUnderstandingRepository materialUnderstandingRepository;

    /** contentHashCalculator（内容哈希计算器）用于判断素材文件内容是否发生变化。 */
    private final MaterialContentHashCalculator contentHashCalculator;

    /** imageUnderstandingTool（图片理解工具）用于在未命中复用时调用 VLM（视觉语言模型）。 */
    private final ImageUnderstandingTool imageUnderstandingTool;

    /** visionModelProperties（视觉模型配置）用于读取模型供应商和模型名称。 */
    private final VisionModelProperties visionModelProperties;

    /** statusCache（状态缓存）用于刷新 Redis（缓存）中的素材处理状态。 */
    private final MaterialProcessTaskStatusCache statusCache;

    /** objectMapper（对象映射器）用于 JSON（对象表示格式）序列化。 */
    private final ObjectMapper objectMapper;

    /** transactionTemplate（事务模板）用于控制数据库短事务边界，避免把 VLM 慢调用包进事务。 */
    private final TransactionTemplate transactionTemplate;

    /**
     * process（处理图片理解）用于执行单个图片素材的理解落库流程。
     *
     * @param processTaskId 处理任务编号
     * @param instruction 图片理解指令
     * @return 素材理解结果实体
     */
    public MaterialUnderstandingEntity process(String processTaskId, String instruction) {
        // 查询处理任务，确保任务存在。
        MaterialProcessTaskEntity task = processTaskRepository.findByProcessTaskId(processTaskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "素材处理任务不存在，processTaskId=" + processTaskId));
        // 标记任务进入图片理解中。
        updateTask(task, TaskStatusValue.PROCESSING.value(), TaskPhase.IMAGE_UNDERSTANDING.value(), 40, "正在理解图片素材", null, null, TaskNextAction.WAIT_PROCESSING.value(), null);
        try {
            // 根据任务中的素材编号读取素材元数据。
            MaterialEntity material = materialRepository.findByMaterialId(task.getMaterialId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "素材不存在，materialId=" + task.getMaterialId()));
            // 校验素材类型，当前服务只处理图片。
            validateImageMaterial(material);
            // 计算素材内容哈希，用于判断历史结果是否可复用。
            String contentHash = contentHashCalculator.calculateSha256(material.getFilePath());
            // 解析当前模型名称，作为复用条件之一。
            String expectedModelName = resolveExpectedModelName();
            // 优先复用同素材、同模型、同提示词版本、同内容哈希的成功结果。
            MaterialUnderstandingEntity reusable = materialUnderstandingRepository
                    .findReusable(material.getMaterialId(), MaterialUnderstandingType.IMAGE_VISION.value(), expectedModelName, PROMPT_VERSION, contentHash)
                    .orElse(null);
            // 命中复用时不再调用 VLM（视觉语言模型）。
            if (reusable != null) {
                updateTask(task, TaskStatusValue.SUCCEEDED.value(), TaskPhase.READY_FOR_CREATION.value(), 100, "图片理解结果已复用，可用于创作", null, null, TaskNextAction.ALLOW_CREATION.value(), LocalDateTime.now());
                return reusable;
            }
            // 构建单图片素材响应，复用已有图片理解工具的请求构造和模型调用逻辑。
            MaterialResponse materialResponse = toMaterialResponse(material);
            // 调用 VLM（视觉语言模型）生成图片理解结果。
            VisionModelResponse response = imageUnderstandingTool.understand(task.getRunId(), List.of(materialResponse), instruction);
            // 读取单图理解结果。
            ImageUnderstandingResult imageResult = firstImageResult(response, material.getMaterialId());
            // 保存成功的图片理解结果。
            MaterialUnderstandingEntity saved = saveSucceededUnderstanding(material, contentHash, instruction, response, imageResult);
            // 标记处理任务完成。
            updateTask(task, TaskStatusValue.SUCCEEDED.value(), TaskPhase.READY_FOR_CREATION.value(), 100, "图片理解完成，可用于创作", null, null, TaskNextAction.ALLOW_CREATION.value(), LocalDateTime.now());
            // 返回新保存的理解结果。
            return saved;
        } catch (RuntimeException exception) {
            // 捕获异常并把任务状态改为失败，方便前端状态轮询展示。
            String errorCode = exception instanceof BusinessException businessException ? businessException.getErrorCode().getCode() : ErrorCode.INTERNAL_ERROR.getCode();
            updateTask(task, TaskStatusValue.FAILED.value(), TaskPhase.FAILED.value(), 100, "图片理解失败", errorCode, exception.getMessage(), TaskNextAction.RETRY_PROCESSING.value(), LocalDateTime.now());
            // 继续抛出异常，让调用方感知失败。
            throw exception;
        }
    }

    /**
     * processMaterial（按素材处理图片理解）用于视频候选帧等内部派生图片素材的理解落库。
     *
     * <p>该方法不更新 cf_material_process_task（素材处理任务表），适合被视频处理编排服务复用。
     *
     * @param materialId 图片素材编号
     * @param runId 运行编号
     * @param instruction 图片理解指令
     * @return 素材理解结果实体
     */
    public MaterialUnderstandingEntity processMaterial(String materialId, String runId, String instruction) {
        // 查询图片素材元数据。
        MaterialEntity material = materialRepository.findByMaterialId(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "素材不存在，materialId=" + materialId));
        // 校验当前素材必须是图片。
        validateImageMaterial(material);
        // 计算素材内容哈希，用于复用判断。
        String contentHash = contentHashCalculator.calculateSha256(material.getFilePath());
        // 解析当前模型名称，作为复用条件之一。
        String expectedModelName = resolveExpectedModelName();
        // 优先读取可复用的成功结果。
        MaterialUnderstandingEntity reusable = materialUnderstandingRepository
                .findReusable(material.getMaterialId(), MaterialUnderstandingType.IMAGE_VISION.value(), expectedModelName, PROMPT_VERSION, contentHash)
                .orElse(null);
        // 命中复用时直接返回旧结果，不再调用 VLM（视觉语言模型）。
        if (reusable != null) {
            return reusable;
        }
        // 把素材实体转换为图片理解工具需要的响应对象。
        MaterialResponse materialResponse = toMaterialResponse(material);
        // 调用 VLM（视觉语言模型）理解单张图片。
        VisionModelResponse response = imageUnderstandingTool.understand(runId, List.of(materialResponse), instruction);
        // 读取第一条图片理解结果。
        ImageUnderstandingResult imageResult = firstImageResult(response, material.getMaterialId());
        // 保存并返回图片理解结果。
        return saveSucceededUnderstanding(material, contentHash, instruction, response, imageResult);
    }

    /**
     * validateImageMaterial（校验图片素材）用于拦截非图片素材误调用图片理解处理服务。
     *
     * @param material 素材实体
     */
    private void validateImageMaterial(MaterialEntity material) {
        // 非 IMAGE（图片）素材不允许进入图片理解处理。
        if (!IMAGE_MATERIAL_TYPE.equalsIgnoreCase(material.getMaterialType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前素材不是图片，无法执行图片理解，materialId=" + material.getMaterialId());
        }
    }

    /**
     * saveSucceededUnderstanding（保存成功理解结果）用于把 VLM（视觉语言模型）返回结果落库。
     *
     * @param material 素材实体
     * @param contentHash 内容哈希
     * @param instruction 理解指令
     * @param response 视觉模型响应
     * @param imageResult 图片理解结果
     * @return 已保存素材理解结果
     */
    private MaterialUnderstandingEntity saveSucceededUnderstanding(MaterialEntity material, String contentHash, String instruction, VisionModelResponse response, ImageUnderstandingResult imageResult) {
        // 获取当前时间作为任务结束和记录创建时间。
        LocalDateTime now = LocalDateTime.now();
        // 构建素材理解结果实体。
        MaterialUnderstandingEntity entity = MaterialUnderstandingEntity.builder()
                .id(newId("material_understanding_pk"))
                .understandingId(newId("material_understanding"))
                .materialId(material.getMaterialId())
                .materialType(material.getMaterialType())
                .understandingType(MaterialUnderstandingType.IMAGE_VISION.value())
                .status(MaterialUnderstandingStatus.SUCCEEDED.value())
                .modelProvider(response.getProvider())
                .modelName(response.getModelName())
                .promptVersion(PROMPT_VERSION)
                .contentHash(contentHash)
                .promptHash(sha256(instruction == null ? "" : instruction))
                .contentSummary(imageResult.getSummary())
                .structuredResultJson(toJson(imageResult))
                .rawResponseJson(toJson(response))
                .promptSnapshot(instruction)
                .inputTokenCount(response.getPromptTokens())
                .outputTokenCount(response.getCompletionTokens())
                .totalTokenCount(response.getTotalTokens())
                .startedAt(now)
                .endedAt(now)
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        // 保存素材理解结果。
        // 使用短事务保存理解结果，避免外部模型调用占用数据库事务。
        return transactionTemplate.execute(status -> materialUnderstandingRepository.save(entity));
    }

    /**
     * firstImageResult（读取第一条图片理解结果）用于处理单图调用场景。
     *
     * @param response 视觉模型响应
     * @param materialId 素材编号
     * @return 图片理解结果
     */
    private ImageUnderstandingResult firstImageResult(VisionModelResponse response, String materialId) {
        // 校验模型响应结果是否存在。
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "视觉模型没有返回图片理解结果，materialId=" + materialId);
        }
        // 返回第一条结果。
        return response.getResults().getFirst();
    }

    /**
     * toMaterialResponse（转素材响应）用于复用图片理解工具的输入结构。
     *
     * @param material 素材实体
     * @return 素材响应
     */
    private MaterialResponse toMaterialResponse(MaterialEntity material) {
        // 构建图片理解工具需要的素材元数据。
        return MaterialResponse.builder()
                .materialId(material.getMaterialId())
                .userId(material.getUserId())
                .projectId(material.getProjectId())
                .conversationId(material.getConversationId())
                .runId(material.getRunId())
                .materialType(material.getMaterialType())
                .originalFilename(material.getOriginalFilename())
                .storedFilename(material.getStoredFilename())
                .filePath(material.getFilePath())
                .contentType(material.getContentType())
                .fileSize(material.getFileSize())
                .status(material.getStatus())
                .build();
    }

    /**
     * updateTask（更新任务）用于同步数据库任务状态和 Redis（缓存）状态。
     *
     * @param task 处理任务实体
     * @param status 状态
     * @param phase 阶段
     * @param progressPercent 进度百分比
     * @param message 状态说明
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param nextAction 下一步动作
     * @param endedAt 结束时间
     */
    private void updateTask(MaterialProcessTaskEntity task, String status, String phase, Integer progressPercent, String message, String errorCode, String errorMessage, String nextAction, LocalDateTime endedAt) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 第一次进入处理中时记录 startedAt（开始时间）。
        if (task.getStartedAt() == null && TaskStatusValue.PROCESSING.value().equals(status)) {
            task.setStartedAt(now);
        }
        // 写入最新状态字段。
        task.setStatus(status);
        task.setPhase(phase);
        task.setProgressPercent(progressPercent);
        task.setMessage(message);
        task.setErrorCode(errorCode);
        task.setErrorMessage(errorMessage);
        task.setNextAction(nextAction);
        task.setUpdatedAt(now);
        task.setEndedAt(endedAt);
        task.setUpdateTime(now);
        // 使用短事务更新数据库任务表。
        transactionTemplate.executeWithoutResult(transactionStatus -> processTaskRepository.update(task));
        // 刷新 Redis（缓存）中的统一任务状态。
        statusCache.save(toCacheStatus(task));
    }

    /**
     * toCacheStatus（转缓存状态）用于把数据库任务实体转换为 Redis（缓存）保存对象。
     *
     * @param task 处理任务实体
     * @return 缓存状态对象
     */
    private MaterialProcessTaskStatus toCacheStatus(MaterialProcessTaskEntity task) {
        // 构建缓存状态。
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
     * resolveExpectedModelName（解析期望模型名称）用于在调用前确定可复用结果的模型条件。
     *
     * @return 模型名称
     */
    private String resolveExpectedModelName() {
        // Mock（模拟）模式使用 MockVisionModelClient 固定返回的模型名称。
        if (visionModelProperties.isMockEnabled()) {
            return "mock-vision-model";
        }
        // 真实模式使用配置中的视觉模型名称。
        return visionModelProperties.getModelName();
    }

    /**
     * toJson（转 JSON）用于把对象序列化为 JSON（对象表示格式）字符串。
     *
     * @param value 任意对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            // 使用 Jackson（JSON 处理库）序列化对象。
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            // 序列化失败说明对象结构异常。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "序列化素材理解结果失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * sha256（计算文本哈希）用于记录提示词快照的哈希。
     *
     * @param value 原始文本
     * @return 十六进制 SHA-256 哈希
     */
    private String sha256(String value) {
        try {
            // 创建 SHA-256（安全哈希算法）摘要器。
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            // 计算 UTF-8（统一字符编码）文本哈希。
            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            // 构建十六进制结果。
            StringBuilder builder = new StringBuilder(digest.length * 2);
            // 遍历哈希字节。
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            // 返回哈希字符串。
            return builder.toString();
        } catch (Exception exception) {
            // SHA-256 为 JDK（Java 开发工具包）内置算法，这里只做兜底。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "计算提示词哈希失败", exception);
        }
    }

    /**
     * newId（生成编号）用于生成带业务前缀的随机编号。
     *
     * @param prefix 编号前缀
     * @return 随机编号
     */
    private String newId(String prefix) {
        // 使用 UUID（通用唯一识别码）生成当前阶段足够稳定的业务编号。
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
