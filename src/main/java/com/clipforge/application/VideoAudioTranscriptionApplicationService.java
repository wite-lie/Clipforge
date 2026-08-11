package com.clipforge.application;

import com.clipforge.api.dto.VideoAudioResponse;
import com.clipforge.api.dto.VideoSubtitleSegmentResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.VideoAudioEntity;
import com.clipforge.infra.persistence.entity.VideoSubtitleSegmentEntity;
import com.clipforge.infra.persistence.repository.VideoAudioRepository;
import com.clipforge.infra.persistence.repository.VideoSubtitleSegmentRepository;
import com.clipforge.model.speech.SpeechRecognitionModelClient;
import com.clipforge.model.speech.SpeechRecognitionModelRequest;
import com.clipforge.model.speech.SpeechRecognitionModelResponse;
import com.clipforge.model.speech.SpeechRecognitionSegment;
import com.clipforge.video.audio.AudioExtractStatus;
import com.clipforge.video.audio.AudioSilenceDetectClient;
import com.clipforge.video.audio.AudioSilenceDetectRequest;
import com.clipforge.video.audio.AudioSilenceDetectResult;
import com.clipforge.video.audio.SpeechRecognitionStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * VideoAudioTranscriptionApplicationService（视频音频转写应用服务）。
 *
 * <p>负责校验已提取音频、执行静音检测、调用 ASR（自动语音识别）模型，并持久化全文转写和字幕时间戳。
 */
@Service
@RequiredArgsConstructor
public class VideoAudioTranscriptionApplicationService {

    private final ObjectProvider<VideoAudioRepository> videoAudioRepositoryProvider;
    private final ObjectProvider<VideoSubtitleSegmentRepository> videoSubtitleSegmentRepositoryProvider;
    private final SpeechRecognitionModelClient speechRecognitionModelClient;
    private final AudioSilenceDetectClient audioSilenceDetectClient;

    /**
     * transcribe（转写）用于触发 ASR（自动语音识别）并保存转写文本和字幕片段。
     */
    @Transactional(rollbackFor = Exception.class)
    public VideoAudioResponse transcribe(String materialId) {
        VideoAudioEntity audio = requiredVideoAudio(materialId);
        if (audio.getHasAudioStream() == null || audio.getHasAudioStream() != 1) {
            replaceSubtitleSegments(materialId, List.of());
            return toResponse(saveTranscription(audio, SpeechRecognitionStatus.SKIPPED, null,
                    false, null, "视频没有音轨，跳过 ASR（自动语音识别）", null, null), 0);
        }
        if (!AudioExtractStatus.SUCCEEDED.value().equals(audio.getAudioExtractStatus())) {
            replaceSubtitleSegments(materialId, List.of());
            return toResponse(saveTranscription(audio, SpeechRecognitionStatus.SKIPPED, null,
                    false, null, "音频提取未成功，跳过 ASR（自动语音识别），audioExtractStatus：" + audio.getAudioExtractStatus(),
                    null, null), 0);
        }
        if (audio.getAudioFilePath() == null || audio.getAudioFilePath().isBlank() || !Files.exists(Path.of(audio.getAudioFilePath()))) {
            replaceSubtitleSegments(materialId, List.of());
            return toResponse(saveTranscription(audio, SpeechRecognitionStatus.FAILED, null,
                    false, null, "音频文件不存在：" + audio.getAudioFilePath(), null, null), 0);
        }

        AudioSilenceDetectResult silenceResult = audioSilenceDetectClient.detect(AudioSilenceDetectRequest.builder()
                .audioPath(Path.of(audio.getAudioFilePath()))
                .durationSeconds(audio.getDurationSeconds())
                .build());
        if (silenceResult.isSilenceDetected()) {
            replaceSubtitleSegments(materialId, List.of());
            return toResponse(saveTranscription(audio, SpeechRecognitionStatus.NO_SPEECH_DETECTED, null,
                    false, true, silenceResult.getMessage(), null, null), 0);
        }

        try {
            SpeechRecognitionModelResponse response = speechRecognitionModelClient.transcribe(SpeechRecognitionModelRequest.builder()
                    .materialId(audio.getMaterialId())
                    .audioId(audio.getAudioId())
                    .audioPath(Path.of(audio.getAudioFilePath()))
                    .language(null)
                    .build());
            if (response.getTranscriptText() == null || response.getTranscriptText().isBlank()
                    || Boolean.FALSE.equals(response.getSpeechDetected())) {
                replaceSubtitleSegments(materialId, List.of());
                return toResponse(saveTranscription(audio, SpeechRecognitionStatus.NO_SPEECH_DETECTED, null,
                        false, false, "ASR（自动语音识别）未返回有效人声文本", response.getModelName(), response.getRawContent()), 0);
            }
            List<VideoSubtitleSegmentEntity> segments = buildSubtitleSegments(audio, response);
            replaceSubtitleSegments(materialId, segments);
            VideoAudioEntity saved = saveTranscription(audio, SpeechRecognitionStatus.SUCCEEDED, response.getTranscriptText(),
                    true, false, silenceResult.getMessage(), response.getModelName(), response.getRawContent());
            return toResponse(saved, segments.size());
        } catch (Exception exception) {
            replaceSubtitleSegments(materialId, List.of());
            return toResponse(saveTranscription(audio, SpeechRecognitionStatus.FAILED, null,
                    false, false, exception.getMessage(), null, null), 0);
        }
    }

    /**
     * listSubtitleSegments（查询字幕片段）用于读取某个视频素材已经保存的字幕时间线。
     */
    public List<VideoSubtitleSegmentResponse> listSubtitleSegments(String materialId) {
        if (materialId == null || materialId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "materialId（素材编号）不能为空");
        }
        return requiredVideoSubtitleSegmentRepository().listByMaterialId(materialId).stream()
                .map(this::toSegmentResponse)
                .toList();
    }

    private VideoAudioEntity saveTranscription(VideoAudioEntity audio, SpeechRecognitionStatus status, String transcriptText,
                                               Boolean speechDetected, Boolean silenceDetected, String qualityMessage,
                                               String modelName, String rawResponse) {
        LocalDateTime now = LocalDateTime.now();
        VideoAudioEntity entity = VideoAudioEntity.builder()
                .id(audio.getId())
                .audioId(audio.getAudioId())
                .materialId(audio.getMaterialId())
                .audioFilePath(audio.getAudioFilePath())
                .durationSeconds(audio.getDurationSeconds())
                .hasAudioStream(audio.getHasAudioStream())
                .audioExtractStatus(audio.getAudioExtractStatus())
                .speechRecognitionStatus(status.value())
                .transcriptText(transcriptText)
                .speechDetected(toDbBoolean(speechDetected))
                .silenceDetected(toDbBoolean(silenceDetected))
                .audioQualityMessage(qualityMessage)
                .asrModelName(modelName)
                .asrRawResponse(rawResponse)
                .errorMessage(status == SpeechRecognitionStatus.FAILED ? qualityMessage : null)
                .createTime(audio.getCreateTime())
                .updateTime(now)
                .deleted(0)
                .build();
        return requiredVideoAudioRepository().saveOrUpdateByMaterialId(entity);
    }

    private List<VideoSubtitleSegmentEntity> buildSubtitleSegments(VideoAudioEntity audio, SpeechRecognitionModelResponse response) {
        List<SpeechRecognitionSegment> modelSegments = response.getSegments();
        if (modelSegments == null || modelSegments.isEmpty()) {
            modelSegments = List.of(SpeechRecognitionSegment.builder()
                    .startSeconds(0.0)
                    .endSeconds(audio.getDurationSeconds())
                    .text(response.getTranscriptText())
                    .confidence(null)
                    .build());
        }
        List<VideoSubtitleSegmentEntity> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < modelSegments.size(); index++) {
            SpeechRecognitionSegment segment = modelSegments.get(index);
            result.add(VideoSubtitleSegmentEntity.builder()
                    .id(newId("video_subtitle_pk"))
                    .segmentId(newId("video_subtitle"))
                    .audioId(audio.getAudioId())
                    .materialId(audio.getMaterialId())
                    .segmentIndex(index)
                    .startSeconds(segment.getStartSeconds())
                    .endSeconds(segment.getEndSeconds())
                    .text(segment.getText())
                    .confidence(segment.getConfidence())
                    .source(response.getProvider() == null ? "ASR" : response.getProvider().toUpperCase())
                    .createTime(now)
                    .updateTime(now)
                    .deleted(0)
                    .build());
        }
        return result;
    }

    private void replaceSubtitleSegments(String materialId, List<VideoSubtitleSegmentEntity> segments) {
        requiredVideoSubtitleSegmentRepository().replaceByMaterialId(materialId, segments);
    }

    private VideoAudioEntity requiredVideoAudio(String materialId) {
        if (materialId == null || materialId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "materialId（素材编号）不能为空");
        }
        return requiredVideoAudioRepository().findByMaterialId(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND,
                        "未查询到视频音频记录，请先调用音频提取接口，materialId：" + materialId));
    }

    private VideoAudioResponse toResponse(VideoAudioEntity entity, Integer segmentCount) {
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
                .subtitleSegmentCount(segmentCount)
                .errorMessage(entity.getErrorMessage())
                .build();
    }

    private VideoSubtitleSegmentResponse toSegmentResponse(VideoSubtitleSegmentEntity entity) {
        return VideoSubtitleSegmentResponse.builder()
                .segmentId(entity.getSegmentId())
                .audioId(entity.getAudioId())
                .materialId(entity.getMaterialId())
                .segmentIndex(entity.getSegmentIndex())
                .startSeconds(entity.getStartSeconds())
                .endSeconds(entity.getEndSeconds())
                .text(entity.getText())
                .confidence(entity.getConfidence())
                .source(entity.getSource())
                .build();
    }

    private Integer toDbBoolean(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? 1 : 0;
    }

    private Boolean toApiBoolean(Integer value) {
        if (value == null) {
            return null;
        }
        return value == 1;
    }

    private VideoAudioRepository requiredVideoAudioRepository() {
        VideoAudioRepository repository = videoAudioRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频音频转写接口需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private VideoSubtitleSegmentRepository requiredVideoSubtitleSegmentRepository() {
        VideoSubtitleSegmentRepository repository = videoSubtitleSegmentRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频字幕片段接口需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
