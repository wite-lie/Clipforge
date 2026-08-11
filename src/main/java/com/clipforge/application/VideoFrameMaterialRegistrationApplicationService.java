package com.clipforge.application;

import com.clipforge.api.dto.VideoFrameResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.MaterialEntity;
import com.clipforge.infra.persistence.entity.VideoFrameEntity;
import com.clipforge.infra.persistence.repository.MaterialRepository;
import com.clipforge.infra.persistence.repository.VideoFrameRepository;
import com.clipforge.material.model.MaterialType;
import com.clipforge.video.frame.VideoFrameExtractStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * VideoFrameMaterialRegistrationApplicationService（视频帧素材注册应用服务）。
 *
 * <p>该服务负责把已经抽取成功的视频候选帧注册为 cf_material（素材表）中的 IMAGE（图片）素材，
 * 使视频帧可以复用图片理解和素材绑定链路。
 */
@Service
@RequiredArgsConstructor
public class VideoFrameMaterialRegistrationApplicationService {

    /** materialRepositoryProvider（素材仓储提供器）用于查询原视频素材并保存帧图片素材。 */
    private final ObjectProvider<MaterialRepository> materialRepositoryProvider;

    /** videoFrameRepositoryProvider（视频候选帧仓储提供器）用于查询候选帧并回写帧图片素材编号。 */
    private final ObjectProvider<VideoFrameRepository> videoFrameRepositoryProvider;

    /**
     * registerFrameMaterials（注册帧图片素材）用于把某个视频下已抽取成功的候选帧注册成图片素材。
     *
     * @param materialId 原视频素材编号
     * @return 注册后的候选帧响应列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<VideoFrameResponse> registerFrameMaterials(String materialId) {
        // 查询并校验原视频素材。
        MaterialEntity videoMaterial = requiredVideoMaterial(materialId);
        // 查询当前视频下的候选帧。
        List<VideoFrameEntity> frames = requiredVideoFrameRepository().listByMaterialId(materialId);
        // 遍历候选帧并注册可注册的帧图片。
        for (VideoFrameEntity frame : frames) {
            // 如果当前帧不需要注册，则跳过。
            if (!shouldRegister(frame)) {
                // 继续处理下一帧。
                continue;
            }
            // 创建帧图片素材记录。
            MaterialEntity frameMaterial = buildFrameMaterial(videoMaterial, frame);
            // 保存帧图片素材。
            requiredMaterialRepository().save(frameMaterial);
            // 回写候选帧和图片素材之间的关系。
            requiredVideoFrameRepository().updateFrameMaterialId(frame.getFrameId(), frameMaterial.getMaterialId());
            // 同步更新当前内存对象，保证接口可以直接返回新素材编号。
            frame.setFrameMaterialId(frameMaterial.getMaterialId());
        }
        // 转换为接口响应。
        return frames.stream()
                // 转换每个候选帧实体。
                .map(this::toResponse)
                // 收集为列表。
                .toList();
    }

    /**
     * shouldRegister（是否应该注册）用于判断候选帧是否满足注册为图片素材的条件。
     *
     * @param frame 候选帧实体
     * @return 满足注册条件时返回 true（真）
     */
    private boolean shouldRegister(VideoFrameEntity frame) {
        // 已经注册过帧图片素材时跳过，保证接口幂等。
        if (frame.getFrameMaterialId() != null && !frame.getFrameMaterialId().isBlank()) {
            // 返回不需要注册。
            return false;
        }
        // 只有抽取成功的候选帧才允许注册为图片素材。
        if (!VideoFrameExtractStatus.SUCCEEDED.value().equals(frame.getExtractStatus())) {
            // 返回不需要注册。
            return false;
        }
        // 新数据只注册最终关键帧；历史数据没有质量分析结果时仍允许按旧逻辑注册。
        if (isAnalyzedAndRejected(frame)) {
            // 返回不需要注册。
            return false;
        }
        // 文件路径为空时无法注册。
        if (frame.getFilePath() == null || frame.getFilePath().isBlank()) {
            // 返回不需要注册。
            return false;
        }
        // 候选帧图片文件必须真实存在。
        return Files.exists(Path.of(frame.getFilePath()));
    }

    /**
     * buildFrameMaterial（构建帧图片素材）用于根据原视频素材和候选帧生成 cf_material（素材表）记录。
     *
     * @param videoMaterial 原视频素材
     * @param frame 候选帧
     * @return 帧图片素材实体
     */
    private MaterialEntity buildFrameMaterial(MaterialEntity videoMaterial, VideoFrameEntity frame) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 读取帧图片路径。
        Path framePath = Path.of(frame.getFilePath());
        // 读取帧图片文件名。
        String filename = framePath.getFileName().toString();
        // 构建素材实体。
        return MaterialEntity.builder()
                // 设置数据库主键。
                .id(newId("material_pk"))
                // 设置帧图片素材编号。
                .materialId(newId("material_frame"))
                // 继承原视频素材所属用户。
                .userId(videoMaterial.getUserId())
                // 继承原视频素材所属项目。
                .projectId(videoMaterial.getProjectId())
                // 继承原视频素材所属会话。
                .conversationId(videoMaterial.getConversationId())
                // 继承原视频素材所属运行编号。
                .runId(videoMaterial.getRunId())
                // 设置素材类型为 IMAGE（图片）。
                .materialType(MaterialType.IMAGE.value())
                // 设置便于识别的原始文件名。
                .originalFilename("视频帧-" + frame.getFrameIndex() + "-" + filename)
                // 设置存储文件名。
                .storedFilename(filename)
                // 设置帧图片文件路径。
                .filePath(frame.getFilePath())
                // 设置内容类型。
                .contentType(contentType(filename))
                // 设置文件大小。
                .fileSize(fileSize(framePath))
                // 设置素材状态为已上传。
                .status("UPLOADED")
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
     * isAnalyzedAndRejected（是否已分析且被过滤）用于区分新筛选数据和迁移后的历史数据。
     *
     * @param frame 候选帧实体
     * @return 已经过关键帧筛选且未入选时返回 true（真）
     */
    private boolean isAnalyzedAndRejected(VideoFrameEntity frame) {
        // 未明确标记为未入选时，不视为被过滤。
        if (frame.getKeyFrameSelected() == null || frame.getKeyFrameSelected() != 0) {
            return false;
        }
        // 有质量分、哈希或过滤原因，说明这是新筛选逻辑处理过的帧。
        return frame.getQualityScore() != null
                || frame.getImageHash() != null
                || frame.getRejectReason() != null;
    }

    /**
     * requiredVideoMaterial（获取必需视频素材）用于校验原素材存在且类型为 VIDEO（视频）。
     *
     * @param materialId 素材编号
     * @return 视频素材实体
     */
    private MaterialEntity requiredVideoMaterial(String materialId) {
        // 校验素材编号不能为空。
        if (materialId == null || materialId.isBlank()) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "materialId（素材编号）不能为空");
        }
        // 查询素材实体。
        MaterialEntity material = requiredMaterialRepository().findByMaterialId(materialId)
                // 找不到时抛出数据不存在异常。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到视频素材，materialId：" + materialId));
        // 校验素材类型必须是 VIDEO（视频）。
        if (!MaterialType.VIDEO.value().equalsIgnoreCase(material.getMaterialType())) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有 VIDEO（视频）素材可以注册候选帧素材，当前素材类型：" + material.getMaterialType());
        }
        // 返回视频素材实体。
        return material;
    }

    /**
     * toResponse（转响应）用于把候选帧实体转换为接口响应。
     *
     * @param entity 候选帧实体
     * @return 候选帧响应
     */
    private VideoFrameResponse toResponse(VideoFrameEntity entity) {
        // 构建候选帧响应。
        return VideoFrameResponse.builder()
                // 设置候选帧编号。
                .frameId(entity.getFrameId())
                // 设置原视频素材编号。
                .materialId(entity.getMaterialId())
                // 设置帧图片素材编号。
                .frameMaterialId(entity.getFrameMaterialId())
                // 设置帧序号。
                .frameIndex(entity.getFrameIndex())
                // 设置时间点。
                .timestampSeconds(entity.getTimestampSeconds())
                // 设置文件路径。
                .filePath(entity.getFilePath())
                // 设置宽度。
                .width(entity.getWidth())
                // 设置高度。
                .height(entity.getHeight())
                // 设置抽取状态。
                .extractStatus(entity.getExtractStatus())
                // 设置错误消息。
                .errorMessage(entity.getErrorMessage())
                // 设置是否最终关键帧。
                .keyFrameSelected(entity.getKeyFrameSelected() != null && entity.getKeyFrameSelected() == 1)
                // 设置关键帧评分。
                .keyFrameScore(entity.getKeyFrameScore())
                // 设置质量分。
                .qualityScore(entity.getQualityScore())
                // 设置平均亮度。
                .brightness(entity.getBrightness())
                // 设置清晰度分数。
                .sharpnessScore(entity.getSharpnessScore())
                // 设置图像哈希。
                .imageHash(entity.getImageHash())
                // 设置过滤原因。
                .rejectReason(entity.getRejectReason())
                // 构建响应。
                .build();
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
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频帧素材注册接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * requiredVideoFrameRepository（获取必需视频候选帧仓储）用于处理未启用数据库的场景。
     *
     * @return 视频候选帧仓储
     */
    private VideoFrameRepository requiredVideoFrameRepository() {
        // 从 Spring（Spring 框架）容器中获取视频候选帧仓储。
        VideoFrameRepository repository = videoFrameRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频帧素材注册接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * contentType（内容类型）用于根据文件后缀推断图片 MIME（媒体类型）。
     *
     * @param filename 文件名
     * @return 内容类型
     */
    private String contentType(String filename) {
        // 小写文件名便于判断后缀。
        String lowerFilename = filename.toLowerCase();
        // png（便携式网络图形）后缀返回 image/png。
        if (lowerFilename.endsWith(".png")) {
            // 返回 PNG 内容类型。
            return "image/png";
        }
        // webp（网页图片格式）后缀返回 image/webp。
        if (lowerFilename.endsWith(".webp")) {
            // 返回 WebP 内容类型。
            return "image/webp";
        }
        // 默认按 JPEG（联合图像专家组格式）处理。
        return "image/jpeg";
    }

    /**
     * fileSize（文件大小）用于读取帧图片字节数，读取失败时返回 0。
     *
     * @param path 文件路径
     * @return 文件大小
     */
    private long fileSize(Path path) {
        try {
            // 返回文件真实大小。
            return Files.size(path);
        } catch (Exception exception) {
            // 读取失败时返回 0，避免注册流程因为非核心字段失败。
            return 0L;
        }
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
