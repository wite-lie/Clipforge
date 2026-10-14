package com.clipforge.application;

import com.clipforge.contract.material.VideoMetadataResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.material.application.VideoMetadataQueryUseCase;
import com.clipforge.material.infrastructure.persistence.entity.MaterialEntity;
import com.clipforge.material.infrastructure.persistence.entity.VideoMetadataEntity;
import com.clipforge.material.infrastructure.persistence.repository.MaterialRepository;
import com.clipforge.material.infrastructure.persistence.repository.VideoMetadataRepository;
import com.clipforge.material.model.MaterialType;
import com.clipforge.video.ffmpeg.FFmpegProperties;
import com.clipforge.video.ffmpeg.FFprobeClient;
import com.clipforge.video.ffmpeg.FFprobeException;
import com.clipforge.video.ffmpeg.FFprobeResult;
import com.clipforge.video.metadata.VideoMetadata;
import com.clipforge.video.metadata.VideoMetadataParser;
import com.clipforge.video.metadata.VideoMetadataProbeStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * VideoMetadataApplicationService（视频元数据应用服务）。
 *
 * <p>该服务负责编排视频素材校验、FFprobe（视频探测工具）读取、元数据解析和持久化。
 */
@Service
@RequiredArgsConstructor
public class VideoMetadataApplicationService implements VideoMetadataQueryUseCase {

    /** materialRepositoryProvider（素材仓储提供器）用于读取视频素材元数据。 */
    private final ObjectProvider<MaterialRepository> materialRepositoryProvider;

    /** videoMetadataRepositoryProvider（视频元数据仓储提供器）用于保存和查询视频元数据。 */
    private final ObjectProvider<VideoMetadataRepository> videoMetadataRepositoryProvider;

    /** ffprobeClient（视频探测客户端）用于调用本地 FFprobe（视频探测工具）。 */
    private final FFprobeClient ffprobeClient;

    /** videoMetadataParser（视频元数据解析器）用于解析 FFprobe JSON（对象表示格式）。 */
    private final VideoMetadataParser videoMetadataParser;

    /** ffmpegProperties（音视频处理配置）提供视频时长范围等配置。 */
    private final FFmpegProperties ffmpegProperties;

    /**
     * readMetadata（读取元数据）用于触发 FFprobe 读取视频元数据并保存结果。
     *
     * @param materialId 素材编号
     * @return 视频元数据响应
     */
    public VideoMetadataResponse readMetadata(String materialId) {
        // 查询并校验视频素材。
        MaterialEntity material = requiredVideoMaterial(materialId);
        // 如果文件路径不存在，则保存 FILE_NOT_FOUND（文件不存在）状态。
        if (material.getFilePath() == null || material.getFilePath().isBlank() || !Files.exists(Path.of(material.getFilePath()))) {
            // 保存失败状态并返回。
            return toResponse(saveFailure(material.getMaterialId(), VideoMetadataProbeStatus.FILE_NOT_FOUND, "视频文件不存在：" + material.getFilePath(), null));
        }
        try {
            // 调用 FFprobe（视频探测工具）读取原始 JSON。
            FFprobeResult result = ffprobeClient.probe(Path.of(material.getFilePath()));
            // 解析原始 JSON 为结构化元数据。
            VideoMetadata metadata = videoMetadataParser.parse(result.getRawJson());
            // 保存成功状态和结构化字段。
            VideoMetadataEntity savedEntity = saveSuccess(material.getMaterialId(), metadata);
            // 返回响应。
            return toResponse(savedEntity);
        } catch (FFprobeException exception) {
            // FFprobe 执行失败时保存失败状态，便于接口查询和复盘。
            return toResponse(saveFailure(material.getMaterialId(), exception.getProbeStatus(), exception.getMessage(), null));
        } catch (BusinessException exception) {
            // 解析失败时保存 FAILED（读取失败）状态。
            return toResponse(saveFailure(material.getMaterialId(), VideoMetadataProbeStatus.FAILED, exception.getMessage(), null));
        } catch (Exception exception) {
            // 兜底保存 FAILED（读取失败）状态。
            return toResponse(saveFailure(material.getMaterialId(), VideoMetadataProbeStatus.FAILED, exception.getMessage(), null));
        }
    }

    /**
     * queryMetadata（查询元数据）用于读取已经保存的视频元数据，不会重新调用 FFprobe。
     *
     * @param materialId 素材编号
     * @return 视频元数据响应
     */
    @Override
    public VideoMetadataResponse queryMetadata(String materialId) {
        // 查询视频素材，确保素材存在且类型正确。
        requiredVideoMaterial(materialId);
        // 查询视频元数据。
        VideoMetadataEntity entity = requiredVideoMetadataRepository().findByMaterialId(materialId)
                // 找不到时抛出数据不存在异常。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到视频元数据，请先调用读取接口，materialId：" + materialId));
        // 转换为接口响应。
        return toResponse(entity);
    }

    /**
     * saveSuccess（保存成功结果）用于保存 FFprobe 成功解析出的视频元数据。
     *
     * @param materialId 素材编号
     * @param metadata 视频元数据
     * @return 保存后的实体
     */
    private VideoMetadataEntity saveSuccess(String materialId, VideoMetadata metadata) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 构建成功实体。
        VideoMetadataEntity entity = VideoMetadataEntity.builder()
                // 设置数据库主键。
                .id(newId("video_metadata_pk"))
                // 设置业务编号。
                .metadataId(newId("video_metadata"))
                // 设置素材编号。
                .materialId(materialId)
                // 设置视频时长。
                .durationSeconds(metadata.getDurationSeconds())
                // 设置视频宽度。
                .width(metadata.getVideoStream().getWidth())
                // 设置视频高度。
                .height(metadata.getVideoStream().getHeight())
                // 设置视频帧率。
                .frameRate(metadata.getVideoStream().getFrameRate())
                // 设置整体码率。
                .bitRate(metadata.getBitRate())
                // 设置容器格式。
                .formatName(metadata.getFormatName())
                // 设置视频编码。
                .videoCodec(metadata.getVideoStream().getCodecName())
                // 设置是否有音轨。
                .hasAudio(metadata.hasAudio() ? 1 : 0)
                // 设置音频编码。
                .audioCodec(metadata.getAudioStream() == null ? null : metadata.getAudioStream().getCodecName())
                // 设置音轨数量。
                .audioStreamCount(metadata.getAudioStreamCount() == null ? 0 : metadata.getAudioStreamCount())
                // 设置探测状态为成功。
                .probeStatus(VideoMetadataProbeStatus.SUCCEEDED.value())
                // 成功时没有错误消息。
                .probeErrorMessage(null)
                // 保存原始 JSON。
                .rawProbeJson(metadata.getRawProbeJson())
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记。
                .deleted(0)
                // 构建实体。
                .build();
        // 保存或更新实体。
        return requiredVideoMetadataRepository().saveOrUpdateByMaterialId(entity);
    }

    /**
     * saveFailure（保存失败结果）用于保存视频元数据读取失败状态。
     *
     * @param materialId 素材编号
     * @param status 探测状态
     * @param errorMessage 错误消息
     * @param rawJson 原始 JSON
     * @return 保存后的实体
     */
    private VideoMetadataEntity saveFailure(String materialId, VideoMetadataProbeStatus status, String errorMessage, String rawJson) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 构建失败实体。
        VideoMetadataEntity entity = VideoMetadataEntity.builder()
                // 设置数据库主键。
                .id(newId("video_metadata_pk"))
                // 设置业务编号。
                .metadataId(newId("video_metadata"))
                // 设置素材编号。
                .materialId(materialId)
                // 设置没有音轨。
                .hasAudio(0)
                // 设置音轨数量。
                .audioStreamCount(0)
                // 设置探测状态。
                .probeStatus(status.value())
                // 设置错误消息。
                .probeErrorMessage(errorMessage)
                // 保存可能存在的原始 JSON。
                .rawProbeJson(rawJson)
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记。
                .deleted(0)
                // 构建实体。
                .build();
        // 保存或更新失败状态。
        return requiredVideoMetadataRepository().saveOrUpdateByMaterialId(entity);
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
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有 VIDEO（视频）素材可以读取视频元数据，当前素材类型：" + material.getMaterialType());
        }
        // 返回视频素材实体。
        return material;
    }

    /**
     * toResponse（转响应）用于把视频元数据实体转换为接口响应。
     *
     * @param entity 视频元数据实体
     * @return 视频元数据响应
     */
    private VideoMetadataResponse toResponse(VideoMetadataEntity entity) {
        // 构建基础响应。
        VideoMetadataResponse response = VideoMetadataResponse.builder()
                // 设置元数据编号。
                .metadataId(entity.getMetadataId())
                // 设置素材编号。
                .materialId(entity.getMaterialId())
                // 设置视频时长。
                .durationSeconds(entity.getDurationSeconds())
                // 设置视频宽度。
                .width(entity.getWidth())
                // 设置视频高度。
                .height(entity.getHeight())
                // 设置视频帧率。
                .frameRate(entity.getFrameRate())
                // 设置整体码率。
                .bitRate(entity.getBitRate())
                // 设置容器格式。
                .formatName(entity.getFormatName())
                // 设置视频编码。
                .videoCodec(entity.getVideoCodec())
                // 设置是否有音轨。
                .hasAudio(entity.getHasAudio() != null && entity.getHasAudio() == 1)
                // 设置音频编码。
                .audioCodec(entity.getAudioCodec())
                // 设置音轨数量。
                .audioStreamCount(entity.getAudioStreamCount())
                // 设置探测状态。
                .probeStatus(entity.getProbeStatus())
                // 设置错误消息。
                .probeErrorMessage(entity.getProbeErrorMessage())
                // 构建响应。
                .build();
        // 补充时长范围提示。
        fillDurationRange(response);
        // 返回响应。
        return response;
    }

    /**
     * fillDurationRange（填充时长范围）用于根据配置生成视频时长提示。
     *
     * @param response 视频元数据响应
     */
    private void fillDurationRange(VideoMetadataResponse response) {
        // 读取视频时长。
        Double duration = response.getDurationSeconds();
        // 时长为空时无法判断。
        if (duration == null) {
            // 设置为不可判断。
            response.setDurationWithinConfiguredRange(null);
            // 返回。
            return;
        }
        // 判断是否短于配置最小时长。
        if (duration < ffmpegProperties.getMinDurationSeconds()) {
            // 标记不在范围内。
            response.setDurationWithinConfiguredRange(false);
            // 设置提示消息。
            response.setDurationWarningMessage("视频时长短于建议最小时长 " + ffmpegProperties.getMinDurationSeconds() + " 秒");
            // 返回。
            return;
        }
        // 判断是否长于配置最大时长。
        if (duration > ffmpegProperties.getMaxDurationSeconds()) {
            // 标记不在范围内。
            response.setDurationWithinConfiguredRange(false);
            // 设置提示消息。
            response.setDurationWarningMessage("视频时长超过建议最大时长 " + ffmpegProperties.getMaxDurationSeconds() + " 秒");
            // 返回。
            return;
        }
        // 标记时长符合配置范围。
        response.setDurationWithinConfiguredRange(true);
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
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频元数据接口需要启用 db Profile（数据库配置环境）");
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
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频元数据接口需要启用 db Profile（数据库配置环境）");
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
