package com.clipforge.application;

import com.clipforge.api.dto.VideoAudioResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.MaterialEntity;
import com.clipforge.infra.persistence.entity.VideoAudioEntity;
import com.clipforge.infra.persistence.entity.VideoMetadataEntity;
import com.clipforge.infra.persistence.repository.MaterialRepository;
import com.clipforge.infra.persistence.repository.VideoAudioRepository;
import com.clipforge.infra.persistence.repository.VideoMetadataRepository;
import com.clipforge.material.model.MaterialType;
import com.clipforge.video.audio.AudioExtractStatus;
import com.clipforge.video.audio.SpeechRecognitionStatus;
import com.clipforge.video.audio.VideoAudioProperties;
import com.clipforge.video.ffmpeg.AudioExtractRequest;
import com.clipforge.video.ffmpeg.FFmpegAudioExtractClient;
import com.clipforge.video.ffmpeg.FFmpegAudioExtractException;
import com.clipforge.video.metadata.VideoMetadataProbeStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * VideoAudioExtractionApplicationService（视频音频提取应用服务）。
 *
 * <p>负责编排视频素材校验、视频元数据校验、无音轨分支、FFmpeg（音视频处理工具）音频提取和结果持久化。
 */
@Service
@RequiredArgsConstructor
public class VideoAudioExtractionApplicationService {

    private final ObjectProvider<MaterialRepository> materialRepositoryProvider;
    private final ObjectProvider<VideoMetadataRepository> videoMetadataRepositoryProvider;
    private final ObjectProvider<VideoAudioRepository> videoAudioRepositoryProvider;
    private final FFmpegAudioExtractClient audioExtractClient;
    private final VideoAudioProperties videoAudioProperties;

    /**
     * extractAudio（提取音频）用于触发视频音频提取并保存结果。
     */
    public VideoAudioResponse extractAudio(String materialId) {
        MaterialEntity material = requiredVideoMaterial(materialId);
        if (material.getFilePath() == null || material.getFilePath().isBlank() || !Files.exists(Path.of(material.getFilePath()))) {
            return toResponse(saveResult(materialId, null, null, 0, AudioExtractStatus.VIDEO_FILE_NOT_FOUND,
                    SpeechRecognitionStatus.SKIPPED, null, "视频文件不存在：" + material.getFilePath()));
        }
        VideoMetadataEntity metadata = requiredVideoMetadata(materialId);
        if (!VideoMetadataProbeStatus.SUCCEEDED.value().equals(metadata.getProbeStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频元数据未成功读取，无法提取音频，probeStatus：" + metadata.getProbeStatus());
        }
        if (metadata.getHasAudio() == null || metadata.getHasAudio() != 1
                || metadata.getAudioStreamCount() == null || metadata.getAudioStreamCount() <= 0) {
            return toResponse(saveResult(materialId, null, metadata.getDurationSeconds(), 0,
                    AudioExtractStatus.NO_AUDIO_STREAM, SpeechRecognitionStatus.SKIPPED,
                    null, "视频没有音轨，跳过音频提取和 ASR（自动语音识别）"));
        }
        Path outputPath = buildOutputPath(materialId);
        try {
            audioExtractClient.extractAudio(AudioExtractRequest.builder()
                    .videoPath(Path.of(material.getFilePath()))
                    .outputPath(outputPath)
                    .sampleRate(videoAudioProperties.getSampleRate())
                    .channels(videoAudioProperties.getChannels())
                    .build());
            return toResponse(saveResult(materialId, outputPath.toString(), metadata.getDurationSeconds(), 1,
                    AudioExtractStatus.SUCCEEDED, SpeechRecognitionStatus.PENDING, null, null));
        } catch (FFmpegAudioExtractException exception) {
            return toResponse(saveResult(materialId, outputPath.toString(), metadata.getDurationSeconds(), 1,
                    exception.getExtractStatus(), SpeechRecognitionStatus.SKIPPED, null, exception.getMessage()));
        } catch (Exception exception) {
            return toResponse(saveResult(materialId, outputPath.toString(), metadata.getDurationSeconds(), 1,
                    AudioExtractStatus.FAILED, SpeechRecognitionStatus.SKIPPED, null, exception.getMessage()));
        }
    }

    /**
     * queryAudio（查询音频）用于读取已经保存的视频音频记录。
     */
    public VideoAudioResponse queryAudio(String materialId) {
        requiredVideoMaterial(materialId);
        VideoAudioEntity entity = requiredVideoAudioRepository().findByMaterialId(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND,
                        "未查询到视频音频记录，请先调用音频提取接口，materialId：" + materialId));
        return toResponse(entity);
    }

    private VideoAudioEntity saveResult(String materialId, String audioFilePath, Double durationSeconds, int hasAudioStream,
                                        AudioExtractStatus audioExtractStatus, SpeechRecognitionStatus speechRecognitionStatus,
                                        String transcriptText, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        VideoAudioEntity entity = VideoAudioEntity.builder()
                .id(newId("video_audio_pk"))
                .audioId(newId("video_audio"))
                .materialId(materialId)
                .audioFilePath(audioFilePath)
                .durationSeconds(durationSeconds)
                .hasAudioStream(hasAudioStream)
                .audioExtractStatus(audioExtractStatus.value())
                .speechRecognitionStatus(speechRecognitionStatus.value())
                .transcriptText(transcriptText)
                .speechDetected(null)
                .silenceDetected(null)
                .audioQualityMessage(null)
                .asrModelName(null)
                .asrRawResponse(null)
                .errorMessage(errorMessage)
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        return requiredVideoAudioRepository().saveOrUpdateByMaterialId(entity);
    }

    private Path buildOutputPath(String materialId) {
        String audioFormat = videoAudioProperties.getAudioFormat().replace(".", "");
        String filename = materialId + "_audio." + audioFormat;
        return Path.of(videoAudioProperties.getOutputRootPath(), LocalDate.now().toString(), materialId, filename);
    }

    private MaterialEntity requiredVideoMaterial(String materialId) {
        if (materialId == null || materialId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "materialId（素材编号）不能为空");
        }
        MaterialEntity material = requiredMaterialRepository().findByMaterialId(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到素材，materialId：" + materialId));
        if (!MaterialType.VIDEO.value().equalsIgnoreCase(material.getMaterialType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有 VIDEO（视频）素材可以提取音频，当前素材类型：" + material.getMaterialType());
        }
        return material;
    }

    private VideoMetadataEntity requiredVideoMetadata(String materialId) {
        return requiredVideoMetadataRepository().findByMaterialId(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST,
                        "缺少视频元数据，请先调用 /api/videos/{materialId}/metadata/read"));
    }

    private VideoAudioResponse toResponse(VideoAudioEntity entity) {
        return VideoAudioResponse.builder()
                .audioId(entity.getAudioId())
                .materialId(entity.getMaterialId())
                .audioFilePath(entity.getAudioFilePath())
                .durationSeconds(entity.getDurationSeconds())
                .hasAudioStream(entity.getHasAudioStream() != null && entity.getHasAudioStream() == 1)
                .audioExtractStatus(entity.getAudioExtractStatus())
                .speechRecognitionStatus(entity.getSpeechRecognitionStatus())
                .transcriptText(entity.getTranscriptText())
                .speechDetected(toApiBoolean(entity.getSpeechDetected()))
                .silenceDetected(toApiBoolean(entity.getSilenceDetected()))
                .audioQualityMessage(entity.getAudioQualityMessage())
                .asrModelName(entity.getAsrModelName())
                .subtitleSegmentCount(null)
                .errorMessage(entity.getErrorMessage())
                .build();
    }

    private Boolean toApiBoolean(Integer value) {
        if (value == null) {
            return null;
        }
        return value == 1;
    }

    private MaterialRepository requiredMaterialRepository() {
        MaterialRepository repository = materialRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频音频接口需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private VideoMetadataRepository requiredVideoMetadataRepository() {
        VideoMetadataRepository repository = videoMetadataRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频音频接口需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private VideoAudioRepository requiredVideoAudioRepository() {
        VideoAudioRepository repository = videoAudioRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频音频接口需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
