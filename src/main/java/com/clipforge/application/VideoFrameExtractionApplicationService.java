package com.clipforge.application;

import com.clipforge.api.dto.VideoFrameResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.MaterialEntity;
import com.clipforge.infra.persistence.entity.VideoFrameEntity;
import com.clipforge.infra.persistence.entity.VideoMetadataEntity;
import com.clipforge.infra.persistence.repository.MaterialRepository;
import com.clipforge.infra.persistence.repository.VideoFrameRepository;
import com.clipforge.infra.persistence.repository.VideoMetadataRepository;
import com.clipforge.material.model.MaterialType;
import com.clipforge.video.ffmpeg.FFmpegFrameExtractClient;
import com.clipforge.video.ffmpeg.FFmpegFrameExtractException;
import com.clipforge.video.ffmpeg.FrameExtractRequest;
import com.clipforge.video.frame.VideoFrameCandidate;
import com.clipforge.video.frame.VideoFrameExtractPlan;
import com.clipforge.video.frame.VideoFrameExtractPlanner;
import com.clipforge.video.frame.VideoFrameExtractStatus;
import com.clipforge.video.frame.VideoKeyFrameSelector;
import com.clipforge.video.frame.VideoFrameProperties;
import com.clipforge.video.metadata.VideoMetadataProbeStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * VideoFrameExtractionApplicationService（视频帧抽取应用服务）。
 *
 * <p>该服务负责编排视频素材校验、元数据读取、抽帧计划生成、FFmpeg（音视频处理工具）抽帧和候选帧持久化。
 */
@Service
@RequiredArgsConstructor
public class VideoFrameExtractionApplicationService {

    /** materialRepositoryProvider（素材仓储提供器）用于读取视频素材元数据。 */
    private final ObjectProvider<MaterialRepository> materialRepositoryProvider;

    /** videoMetadataRepositoryProvider（视频元数据仓储提供器）用于读取视频元数据。 */
    private final ObjectProvider<VideoMetadataRepository> videoMetadataRepositoryProvider;

    /** videoFrameRepositoryProvider（视频候选帧仓储提供器）用于保存和查询候选帧。 */
    private final ObjectProvider<VideoFrameRepository> videoFrameRepositoryProvider;

    /** videoFrameExtractPlanner（视频帧抽取规划器）用于生成候选帧时间点。 */
    private final VideoFrameExtractPlanner videoFrameExtractPlanner;

    /** frameExtractClient（帧抽取客户端）用于调用 FFmpeg（音视频处理工具）抽帧。 */
    private final FFmpegFrameExtractClient frameExtractClient;

    /** videoFrameProperties（视频帧配置）提供输出目录、图片格式和质量参数。 */
    private final VideoFrameProperties videoFrameProperties;

    /** videoKeyFrameSelector（视频关键帧选择器）用于抽帧后筛选最终关键帧。 */
    private final VideoKeyFrameSelector videoKeyFrameSelector;

    /**
     * extractFrames（抽取帧）用于触发候选帧抽取并保存结果。
     *
     * @param materialId 素材编号
     * @return 候选帧响应列表
     */
    public List<VideoFrameResponse> extractFrames(String materialId) {
        // 查询并校验视频素材。
        MaterialEntity material = requiredVideoMaterial(materialId);
        // 如果视频文件不存在，则抛出错误请求异常。
        if (material.getFilePath() == null || material.getFilePath().isBlank() || !Files.exists(Path.of(material.getFilePath()))) {
            // 抛出视频文件不存在异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频文件不存在，无法抽帧：" + material.getFilePath());
        }
        // 查询视频元数据。
        VideoMetadataEntity metadata = requiredVideoMetadata(materialId);
        // 校验元数据必须读取成功。
        if (!VideoMetadataProbeStatus.SUCCEEDED.value().equals(metadata.getProbeStatus())) {
            // 抛出缺少成功元数据异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频元数据未成功读取，无法抽帧，probeStatus：" + metadata.getProbeStatus());
        }
        // 校验视频时长必须存在。
        if (metadata.getDurationSeconds() == null || metadata.getDurationSeconds() <= 0) {
            // 抛出缺少时长异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频元数据缺少有效时长，无法抽帧");
        }
        // 根据视频时长生成抽帧计划。
        VideoFrameExtractPlan plan = videoFrameExtractPlanner.plan(materialId, metadata.getDurationSeconds());
        // 保存本次抽帧生成的候选帧实体。
        List<VideoFrameEntity> entities = new ArrayList<>();
        // 遍历候选帧时间点。
        for (VideoFrameCandidate candidate : plan.getCandidates()) {
            // 构建输出路径。
            Path outputPath = buildOutputPath(materialId, candidate.getFrameIndex());
            // 抽取单张候选帧。
            VideoFrameEntity entity = extractSingleFrame(material, metadata, candidate, outputPath);
            // 添加到实体集合。
            entities.add(entity);
        }
        // 清理旧候选帧已经注册出的图片素材，避免重复抽帧后素材列表残留历史帧。
        cleanupOldFrameMaterials(materialId);
        // 对抽取成功的候选帧进行质量分析、相似度去重和最终关键帧筛选。
        List<VideoFrameEntity> analyzedEntities = videoKeyFrameSelector.select(entities);
        // 保存抽帧和关键帧筛选结果，重抽时会逻辑删除旧帧。
        List<VideoFrameEntity> savedEntities = requiredVideoFrameRepository().replaceByMaterialId(materialId, analyzedEntities);
        // 转换为接口响应。
        return savedEntities.stream()
                // 转换每条候选帧实体。
                .map(this::toResponse)
                // 收集为列表。
                .toList();
    }

    /**
     * listFrames（查询帧列表）用于读取已经保存的视频候选帧。
     *
     * @param materialId 素材编号
     * @return 候选帧响应列表
     */
    public List<VideoFrameResponse> listFrames(String materialId) {
        // 校验视频素材存在且类型正确。
        requiredVideoMaterial(materialId);
        // 查询候选帧列表。
        List<VideoFrameEntity> entities = requiredVideoFrameRepository().listByMaterialId(materialId);
        // 转换为接口响应。
        return entities.stream()
                // 转换候选帧实体。
                .map(this::toResponse)
                // 收集为列表。
                .toList();
    }

    /**
     * extractSingleFrame（抽取单帧）用于执行单个候选帧抽取并生成实体。
     *
     * @param material 视频素材实体
     * @param metadata 视频元数据实体
     * @param candidate 候选帧
     * @param outputPath 输出路径
     * @return 候选帧实体
     */
    private VideoFrameEntity extractSingleFrame(MaterialEntity material, VideoMetadataEntity metadata, VideoFrameCandidate candidate, Path outputPath) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 生成候选帧业务编号。
        String frameId = newId("video_frame");
        try {
            // 调用 FFmpeg（音视频处理工具）抽取单帧。
            frameExtractClient.extractFrame(FrameExtractRequest.builder()
                    // 设置原视频路径。
                    .videoPath(Path.of(material.getFilePath()))
                    // 设置抽帧时间点。
                    .timestampSeconds(candidate.getTimestampSeconds())
                    // 设置输出路径。
                    .outputPath(outputPath)
                    // 设置 JPEG 质量。
                    .jpegQuality(videoFrameProperties.getJpegQuality())
                    // 构建请求。
                    .build());
            // 构建成功实体。
            return baseEntity(frameId, material.getMaterialId(), candidate, now)
                    // 设置候选帧图片路径。
                    .filePath(outputPath.toString())
                    // 设置图片宽度，第一版直接使用视频宽度。
                    .width(metadata.getWidth())
                    // 设置图片高度，第一版直接使用视频高度。
                    .height(metadata.getHeight())
                    // 设置抽取状态为成功。
                    .extractStatus(VideoFrameExtractStatus.SUCCEEDED.value())
                    // 构建实体。
                    .build();
        } catch (FFmpegFrameExtractException exception) {
            // 构建失败实体。
            return baseEntity(frameId, material.getMaterialId(), candidate, now)
                    // 设置原计划输出路径，便于复盘。
                    .filePath(outputPath.toString())
                    // 设置抽取状态。
                    .extractStatus(exception.getExtractStatus().value())
                    // 设置错误消息。
                    .errorMessage(exception.getMessage())
                    // 构建实体。
                    .build();
        } catch (Exception exception) {
            // 构建兜底失败实体。
            return baseEntity(frameId, material.getMaterialId(), candidate, now)
                    // 设置原计划输出路径。
                    .filePath(outputPath.toString())
                    // 设置普通失败状态。
                    .extractStatus(VideoFrameExtractStatus.FAILED.value())
                    // 设置错误消息。
                    .errorMessage(exception.getMessage())
                    // 构建实体。
                    .build();
        }
    }

    /**
     * baseEntity（基础实体）用于构建候选帧通用字段。
     *
     * @param frameId 帧编号
     * @param materialId 素材编号
     * @param candidate 候选帧
     * @param now 当前时间
     * @return 候选帧实体建造器
     */
    private VideoFrameEntity.VideoFrameEntityBuilder baseEntity(String frameId, String materialId, VideoFrameCandidate candidate, LocalDateTime now) {
        // 返回候选帧实体建造器。
        return VideoFrameEntity.builder()
                // 设置数据库主键。
                .id(newId("video_frame_pk"))
                // 设置候选帧编号。
                .frameId(frameId)
                // 设置素材编号。
                .materialId(materialId)
                // 设置帧序号。
                .frameIndex(candidate.getFrameIndex())
                // 设置时间点。
                .timestampSeconds(candidate.getTimestampSeconds())
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记。
                .deleted(0);
    }

    /**
     * buildOutputPath（构建输出路径）用于生成候选帧图片保存位置。
     *
     * @param materialId 素材编号
     * @param frameIndex 帧序号
     * @return 输出路径
     */
    private Path buildOutputPath(String materialId, int frameIndex) {
        // 读取图片格式并去除可能存在的点号。
        String imageFormat = videoFrameProperties.getImageFormat().replace(".", "");
        // 生成文件名。
        String filename = materialId + "_frame_" + frameIndex + "." + imageFormat;
        // 按日期和素材编号分目录，避免单目录文件过多。
        return Path.of(videoFrameProperties.getOutputRootPath(), LocalDate.now().toString(), materialId, filename);
    }

    /**
     * requiredVideoMaterial（获取必需视频素材）用于校验素材存在且类型为 VIDEO（视频）。
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
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到素材，materialId：" + materialId));
        // 校验素材类型必须是 VIDEO（视频）。
        if (!MaterialType.VIDEO.value().equalsIgnoreCase(material.getMaterialType())) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有 VIDEO（视频）素材可以抽取视频帧，当前素材类型：" + material.getMaterialType());
        }
        // 返回视频素材实体。
        return material;
    }

    /**
     * requiredVideoMetadata（获取必需视频元数据）用于读取成功探测过的视频元数据。
     *
     * @param materialId 素材编号
     * @return 视频元数据实体
     */
    private VideoMetadataEntity requiredVideoMetadata(String materialId) {
        // 查询视频元数据。
        return requiredVideoMetadataRepository().findByMaterialId(materialId)
                // 找不到时抛出错误请求异常。
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "缺少视频元数据，请先调用 /api/videos/{materialId}/metadata/read"));
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
                // 设置素材编号。
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
     * cleanupOldFrameMaterials（清理旧帧图片素材）用于在重抽帧前逻辑删除旧候选帧注册出的图片素材。
     *
     * @param materialId 原视频素材编号
     */
    private void cleanupOldFrameMaterials(String materialId) {
        // 查询当前视频下旧候选帧。
        List<VideoFrameEntity> oldFrames = requiredVideoFrameRepository().listByMaterialId(materialId);
        // 收集旧候选帧对应的图片素材编号。
        List<String> oldFrameMaterialIds = oldFrames.stream()
                // 读取帧图片素材编号。
                .map(VideoFrameEntity::getFrameMaterialId)
                // 过滤空引用。
                .filter(value -> value != null)
                // 去除首尾空白。
                .map(String::strip)
                // 过滤空白字符串。
                .filter(value -> !value.isBlank())
                // 去重，避免重复删除。
                .distinct()
                // 收集为列表。
                .toList();
        // 如果没有旧帧图片素材，则无需清理。
        if (oldFrameMaterialIds.isEmpty()) {
            // 结束方法。
            return;
        }
        // 逻辑删除旧帧图片素材。
        requiredMaterialRepository().logicalDeleteByMaterialIds(oldFrameMaterialIds);
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
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频抽帧接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * requiredVideoMetadataRepository（获取必需视频元数据仓储）用于处理未启用数据库的场景。
     *
     * @return 视频元数据仓储
     */
    private VideoMetadataRepository requiredVideoMetadataRepository() {
        // 从 Spring（Spring 框架）容器中获取视频元数据仓储。
        VideoMetadataRepository repository = videoMetadataRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频抽帧接口需要启用 db Profile（数据库配置环境）");
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
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频抽帧接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
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
