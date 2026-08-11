package com.clipforge.video.understanding;

import com.clipforge.agent.material.MaterialUnderstandingAgent;
import com.clipforge.api.dto.MaterialResponse;
import com.clipforge.application.MaterialApplicationService;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.VideoAudioEntity;
import com.clipforge.infra.persistence.entity.VideoFrameEntity;
import com.clipforge.infra.persistence.entity.VideoMetadataEntity;
import com.clipforge.infra.persistence.entity.VideoSubtitleSegmentEntity;
import com.clipforge.infra.persistence.repository.VideoAudioRepository;
import com.clipforge.infra.persistence.repository.VideoFrameRepository;
import com.clipforge.infra.persistence.repository.VideoMetadataRepository;
import com.clipforge.infra.persistence.repository.VideoSubtitleSegmentRepository;
import com.clipforge.material.MaterialUnderstandingResult;
import com.clipforge.video.audio.SpeechRecognitionStatus;
import com.clipforge.video.frame.VideoFrameExtractStatus;
import com.clipforge.video.metadata.VideoMetadataProbeStatus;
import com.clipforge.video.understanding.highlight.VideoSubtitleSegmentContext;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * VideoUnderstandingContextBuilder（视频理解上下文构造器）。
 *
 * <p>该组件只负责收集和整理视频理解所需事实，不负责生成结论，保证规则摘要器和 LLM（大语言模型）摘要器可以复用同一事实包。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoUnderstandingContextBuilder {

    /** videoMetadataRepositoryProvider（视频元数据仓储提供器）用于读取视频元数据。 */
    private final ObjectProvider<VideoMetadataRepository> videoMetadataRepositoryProvider;

    /** videoFrameRepositoryProvider（视频候选帧仓储提供器）用于读取抽帧结果。 */
    private final ObjectProvider<VideoFrameRepository> videoFrameRepositoryProvider;

    /** videoAudioRepositoryProvider（视频音频仓储提供器）用于读取音频和转写结果。 */
    private final ObjectProvider<VideoAudioRepository> videoAudioRepositoryProvider;

    /** videoSubtitleSegmentRepositoryProvider（视频字幕片段仓储提供器）用于读取 ASR（自动语音识别）字幕时间线。 */
    private final ObjectProvider<VideoSubtitleSegmentRepository> videoSubtitleSegmentRepositoryProvider;

    /** materialApplicationService（素材应用服务）用于按帧素材编号读取图片素材。 */
    private final MaterialApplicationService materialApplicationService;

    /** materialUnderstandingAgent（素材理解智能体）用于复用图片理解链路理解候选帧。 */
    private final MaterialUnderstandingAgent materialUnderstandingAgent;

    /**
     * build（构建上下文）用于读取视频元数据、候选帧、帧图片素材和音频转写。
     *
     * @param runId 运行编号
     * @param materialId 视频素材编号
     * @param instruction 理解指令
     * @return 视频理解上下文
     */
    public VideoUnderstandingContext build(String runId, String materialId, String instruction) {
        // 校验素材编号不能为空。
        if (materialId == null || materialId.isBlank()) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "materialId（素材编号）不能为空");
        }
        // 创建警告列表。
        List<String> warnings = new ArrayList<>();
        // 读取并校验视频元数据。
        VideoMetadataEntity metadata = requiredSucceededMetadata(materialId);
        // 读取候选帧记录。
        List<VideoFrameEntity> frameEntities = requiredVideoFrameRepository().listByMaterialId(materialId);
        // 过滤可参与视觉理解的候选帧，优先使用最终关键帧，历史数据降级使用普通成功帧。
        List<VideoFrameEntity> usableFrames = filterUsableFrames(materialId, frameEntities, warnings);
        // 转换为视频理解帧上下文。
        List<VideoUnderstandingFrameContext> frameContexts = usableFrames.stream().map(this::toFrameContext).toList();
        // 读取帧图片素材编号。
        List<String> frameMaterialIds = usableFrames.stream().map(VideoFrameEntity::getFrameMaterialId).toList();
        // 查询帧图片素材。
        List<MaterialResponse> frameMaterials = materialApplicationService.listMaterialsByIds(frameMaterialIds, "IMAGE");
        // 如果帧图片素材数量少于可用帧数量，则记录警告。
        if (frameMaterials.size() < usableFrames.size()) {
            // 添加素材缺失警告。
            warnings.add("部分候选帧已经抽取但没有查询到对应 IMAGE（图片）素材，视频理解将只使用可用图片。");
        }
        // 调用图片素材理解局部 Agent（智能体）。
        MaterialUnderstandingResult materialUnderstandingResult = materialUnderstandingAgent.understand(runId, frameMaterials, buildFrameInstruction(instruction));
        // 读取音频上下文。
        VideoUnderstandingAudioContext audioContext = buildAudioContext(materialId, warnings);
        // 读取字幕片段上下文。
        List<VideoSubtitleSegmentContext> subtitleSegments = buildSubtitleSegments(materialId);
        // 构建视频理解上下文。
        return VideoUnderstandingContext.builder()
                .runId(runId)
                .materialId(materialId)
                .instruction(instruction)
                .metadata(metadata)
                .frames(frameContexts)
                .materialUnderstandingResult(materialUnderstandingResult)
                .audioContext(audioContext)
                .subtitleSegments(subtitleSegments)
                .warnings(warnings)
                .build();
    }

    /**
     * requiredSucceededMetadata（读取成功元数据）用于保证视频理解有基础媒体信息。
     *
     * @param materialId 素材编号
     * @return 视频元数据实体
     */
    private VideoMetadataEntity requiredSucceededMetadata(String materialId) {
        // 查询视频元数据。
        VideoMetadataEntity metadata = requiredVideoMetadataRepository().findByMaterialId(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到视频元数据，请先调用视频元数据读取接口，materialId：" + materialId));
        // 校验元数据读取状态。
        if (!VideoMetadataProbeStatus.SUCCEEDED.value().equals(metadata.getProbeStatus())) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频元数据读取未成功，不能进行视频理解，probeStatus：" + metadata.getProbeStatus());
        }
        // 返回元数据。
        return metadata;
    }

    /**
     * filterUsableFrames（过滤可用候选帧）用于优先保留最终关键帧，历史数据降级保留普通成功帧。
     *
     * @param frames 原始候选帧记录
     * @param warnings 警告列表
     * @return 可用候选帧记录
     */
    private List<VideoFrameEntity> filterUsableFrames(String materialId, List<VideoFrameEntity> frames, List<String> warnings) {
        // 如果没有候选帧，则记录警告并返回空列表。
        if (frames == null || frames.isEmpty()) {
            // 添加缺少候选帧警告。
            warnings.add("当前视频没有候选帧记录，视频理解将缺少视觉线索。");
            // 返回空列表。
            return List.of();
        }
        // 创建新逻辑下的最终关键帧列表。
        List<VideoFrameEntity> selectedKeyFrames = new ArrayList<>();
        // 创建历史数据兜底帧列表。
        List<VideoFrameEntity> fallbackFrames = new ArrayList<>();
        // 统计被跳过的候选帧，避免逐条输出太多噪声。
        int skippedNullCount = 0;
        int skippedNotSucceededCount = 0;
        int skippedNoMaterialCount = 0;
        // 遍历候选帧。
        for (VideoFrameEntity frame : frames) {
            // 跳过空对象。
            if (frame == null) {
                skippedNullCount++;
                // 继续下一帧。
                continue;
            }
            // 如果抽帧状态不是成功，则跳过。
            if (!VideoFrameExtractStatus.SUCCEEDED.value().equals(frame.getExtractStatus())) {
                skippedNotSucceededCount++;
                // 继续下一帧。
                continue;
            }
            // 如果是新逻辑选出的最终关键帧，则优先使用。
            if (frame.getKeyFrameSelected() != null && frame.getKeyFrameSelected() == 1) {
                if (frame.getFrameMaterialId() == null || frame.getFrameMaterialId().isBlank()) {
                    skippedNoMaterialCount++;
                    continue;
                }
                selectedKeyFrames.add(frame);
                continue;
            }
            // 如果是历史数据，没有关键帧分析结果，则作为降级候选。
            if (isLegacyFrame(frame)) {
                if (frame.getFrameMaterialId() == null || frame.getFrameMaterialId().isBlank()) {
                    skippedNoMaterialCount++;
                    continue;
                }
                fallbackFrames.add(frame);
            }
        }
        if (skippedNullCount > 0) {
            warnings.add("已跳过 " + skippedNullCount + " 个空候选帧对象。");
        }
        if (skippedNotSucceededCount > 0) {
            warnings.add("已跳过 " + skippedNotSucceededCount + " 个抽取未成功的候选帧。");
        }
        if (skippedNoMaterialCount > 0) {
            warnings.add("已跳过 " + skippedNoMaterialCount + " 个尚未注册为图片素材的候选帧。");
        }
        // 如果存在最终关键帧，则直接使用关键帧。
        if (!selectedKeyFrames.isEmpty()) {
            logSelectedKeyFrames(materialId, selectedKeyFrames, "KEY_FRAME_SELECTED");
            return selectedKeyFrames;
        }
        // 如果没有最终关键帧但存在历史帧，则降级使用历史帧。
        if (!fallbackFrames.isEmpty()) {
            warnings.add("当前视频没有最终关键帧筛选结果，已降级使用历史候选帧。");
            logSelectedKeyFrames(materialId, fallbackFrames, "LEGACY_FALLBACK_FRAME");
            return fallbackFrames;
        }
        // 如果过滤后没有可用帧，则记录警告。
        warnings.add("候选帧存在但没有可用于图片理解的帧素材。");
        // 返回空列表。
        return List.of();
    }

    /**
     * isLegacyFrame（是否历史帧）用于兼容迁移前已经抽取但没有关键帧分析结果的候选帧。
     *
     * @param frame 候选帧实体
     * @return 历史帧返回 true（真）
     */
    /**
     * logSelectedKeyFrames（打印入选关键帧）用于观测视频理解最终使用了哪些帧。
     *
     * @param materialId 视频素材编号
     * @param frames 入选帧列表
     * @param source 入选来源
     */
    private void logSelectedKeyFrames(String materialId, List<VideoFrameEntity> frames, String source) {
        if (!log.isInfoEnabled()) {
            return;
        }
        if (frames == null || frames.isEmpty()) {
            log.info("视频理解入选关键帧为空，materialId={}，source={}", materialId, source);
            return;
        }
        log.info("视频理解入选关键帧，materialId={}，source={}，count={}", materialId, source, frames.size());
        for (VideoFrameEntity frame : frames) {
            if (frame == null) {
                continue;
            }
            log.info("视频理解入选关键帧明细，materialId={}，frameId={}，frameIndex={}，timestampSeconds={}，frameMaterialId={}，keyFrameScore={}，qualityScore={}，brightness={}，sharpnessScore={}",
                    materialId,
                    frame.getFrameId(),
                    frame.getFrameIndex(),
                    frame.getTimestampSeconds(),
                    frame.getFrameMaterialId(),
                    frame.getKeyFrameScore(),
                    frame.getQualityScore(),
                    frame.getBrightness(),
                    frame.getSharpnessScore());
        }
    }

    private boolean isLegacyFrame(VideoFrameEntity frame) {
        // 没有关键帧标记时视为历史数据。
        if (frame.getKeyFrameSelected() == null) {
            return true;
        }
        // 迁移后旧数据会得到默认 0，但质量分、哈希和过滤原因仍为空。
        return frame.getKeyFrameSelected() == 0
                && frame.getQualityScore() == null
                && frame.getImageHash() == null
                && frame.getRejectReason() == null;
    }

    /**
     * toFrameContext（转帧上下文）用于把数据库实体转换为领域上下文。
     *
     * @param frame 候选帧实体
     * @return 帧上下文
     */
    private VideoUnderstandingFrameContext toFrameContext(VideoFrameEntity frame) {
        // 构建帧上下文。
        return VideoUnderstandingFrameContext.builder()
                .frameId(frame.getFrameId())
                .frameMaterialId(frame.getFrameMaterialId())
                .frameIndex(frame.getFrameIndex())
                .timestampSeconds(frame.getTimestampSeconds())
                .filePath(frame.getFilePath())
                .keyFrameSelected(frame.getKeyFrameSelected())
                .keyFrameScore(frame.getKeyFrameScore())
                .qualityScore(frame.getQualityScore())
                .rejectReason(frame.getRejectReason())
                .build();
    }

    /**
     * buildFrameInstruction（构建帧理解指令）用于把视频理解目标传给图片素材理解链路。
     *
     * @param instruction 原始理解指令
     * @return 图片理解指令
     */
    private String buildFrameInstruction(String instruction) {
        // 读取调用方指令。
        String safeInstruction = instruction == null || instruction.isBlank() ? "请从短视频创作规划角度理解这些视频候选帧。" : instruction;
        // 返回更具体的帧理解指令。
        return safeInstruction + "\n请把这些图片视为同一个视频的候选帧，重点识别主体、场景变化、可讲解画面和潜在风险。";
    }

    /**
     * buildAudioContext（构建音频上下文）用于读取音频提取和转写结果。
     *
     * @param materialId 素材编号
     * @param warnings 警告列表
     * @return 音频上下文
     */
    private VideoUnderstandingAudioContext buildAudioContext(String materialId, List<String> warnings) {
        // 查询视频音频记录。
        VideoAudioEntity audio = requiredVideoAudioRepository().findByMaterialId(materialId).orElse(null);
        // 如果没有音频记录，则返回空音频上下文。
        if (audio == null) {
            // 添加未处理音频警告。
            warnings.add("当前视频没有音频处理记录，视频理解将跳过音频线索。");
            // 返回空音频上下文。
            return VideoUnderstandingAudioContext.builder()
                    .hasAudioStream(false)
                    .audioExtractStatus("NOT_PROCESSED")
                    .speechRecognitionStatus("NOT_PROCESSED")
                    .build();
        }
        // 如果语音识别没有成功，则记录警告。
        if (!SpeechRecognitionStatus.SUCCEEDED.value().equals(audio.getSpeechRecognitionStatus())) {
            // 添加 ASR（自动语音识别）未成功警告。
            warnings.add("音频转写未成功，speechRecognitionStatus：" + audio.getSpeechRecognitionStatus());
        }
        // 构建音频上下文。
        return VideoUnderstandingAudioContext.builder()
                .audioId(audio.getAudioId())
                .hasAudioStream(audio.getHasAudioStream() != null && audio.getHasAudioStream() == 1)
                .audioExtractStatus(audio.getAudioExtractStatus())
                .speechRecognitionStatus(audio.getSpeechRecognitionStatus())
                .transcriptText(audio.getTranscriptText())
                .errorMessage(audio.getErrorMessage())
                .build();
    }

    /**
     * buildSubtitleSegments（构建字幕片段）用于读取 ASR（自动语音识别）字幕时间线。
     */
    private List<VideoSubtitleSegmentContext> buildSubtitleSegments(String materialId) {
        VideoSubtitleSegmentRepository repository = videoSubtitleSegmentRepositoryProvider.getIfAvailable();
        if (repository == null) {
            return List.of();
        }
        List<VideoSubtitleSegmentEntity> entities = repository.listByMaterialId(materialId);
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .filter(entity -> entity != null && entity.getText() != null && !entity.getText().isBlank())
                .map(this::toSubtitleSegmentContext)
                .toList();
    }

    /**
     * toSubtitleSegmentContext（转字幕片段上下文）用于把数据库实体转为领域上下文。
     */
    private VideoSubtitleSegmentContext toSubtitleSegmentContext(VideoSubtitleSegmentEntity entity) {
        return VideoSubtitleSegmentContext.builder()
                .segmentId(entity.getSegmentId())
                .segmentIndex(entity.getSegmentIndex())
                .startSeconds(entity.getStartSeconds())
                .endSeconds(entity.getEndSeconds())
                .text(entity.getText())
                .confidence(entity.getConfidence())
                .build();
    }

    /**
     * requiredVideoMetadataRepository（获取必需视频元数据仓储）用于处理未启用数据库的场景。
     *
     * @return 视频元数据仓储
     */
    private VideoMetadataRepository requiredVideoMetadataRepository() {
        // 从 Spring（Spring 框架）容器中读取仓储。
        VideoMetadataRepository repository = videoMetadataRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，则抛出持久化未启用异常。
        if (repository == null) {
            // 抛出异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频理解接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回仓储。
        return repository;
    }

    /**
     * requiredVideoFrameRepository（获取必需视频候选帧仓储）用于处理未启用数据库的场景。
     *
     * @return 视频候选帧仓储
     */
    private VideoFrameRepository requiredVideoFrameRepository() {
        // 从 Spring（Spring 框架）容器中读取仓储。
        VideoFrameRepository repository = videoFrameRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，则抛出持久化未启用异常。
        if (repository == null) {
            // 抛出异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频理解接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回仓储。
        return repository;
    }

    /**
     * requiredVideoAudioRepository（获取必需视频音频仓储）用于处理未启用数据库的场景。
     *
     * @return 视频音频仓储
     */
    private VideoAudioRepository requiredVideoAudioRepository() {
        // 从 Spring（Spring 框架）容器中读取仓储。
        VideoAudioRepository repository = videoAudioRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，则抛出持久化未启用异常。
        if (repository == null) {
            // 抛出异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频理解接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回仓储。
        return repository;
    }
}
