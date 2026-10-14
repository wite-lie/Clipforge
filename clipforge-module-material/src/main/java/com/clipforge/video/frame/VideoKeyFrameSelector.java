package com.clipforge.video.frame;

import com.clipforge.material.infrastructure.persistence.entity.VideoFrameEntity;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * VideoKeyFrameSelector（视频关键帧选择器）。
 *
 * <p>该组件在固定间隔多抽帧之后，基于质量过滤和平均哈希相似度去重选出最终关键帧。</p>
 */
@Component
@RequiredArgsConstructor
public class VideoKeyFrameSelector {

    /** 视频帧配置。 */
    private final VideoFrameProperties videoFrameProperties;

    /** 视频帧质量分析器。 */
    private final VideoFrameQualityAnalyzer videoFrameQualityAnalyzer;

    /** 平均哈希计算器。 */
    private final AverageHashCalculator averageHashCalculator;

    /**
     * 选择关键帧。
     *
     * @param frames 候选帧列表
     * @return 更新筛选结果后的候选帧列表
     */
    public List<VideoFrameEntity> select(List<VideoFrameEntity> frames) {
        if (frames == null || frames.isEmpty()) {
            return List.of();
        }

        List<SelectedFrame> selectedFrames = new ArrayList<>();

        for (VideoFrameEntity frame : frames) {
            markRejected(frame, "未通过关键帧筛选");

            if (!VideoFrameExtractStatus.SUCCEEDED.value().equals(frame.getExtractStatus())) {
                markRejected(frame, "抽帧未成功");
                continue;
            }

            VideoFrameQuality quality = videoFrameQualityAnalyzer.analyze(frame.getFilePath());
            applyQuality(frame, quality);

            if (!quality.isValid()) {
                markRejected(frame, quality.getRejectReason());
                continue;
            }

            ImageHash imageHash = calculateHash(frame.getFilePath());
            frame.setImageHash(imageHash.getHex());

            if (isDuplicate(imageHash, selectedFrames)) {
                markRejected(frame, "与已选关键帧画面相似");
                continue;
            }

            if (selectedFrames.size() >= finalKeyFrameCount()) {
                markRejected(frame, "关键帧数量已达上限");
                continue;
            }

            markSelected(frame, quality);
            selectedFrames.add(new SelectedFrame(frame, imageHash));
        }

        if (selectedFrames.isEmpty()) {
            for (VideoFrameEntity frame : frames) {
                if (VideoFrameExtractStatus.SUCCEEDED.value().equals(frame.getExtractStatus())) {
                    markSelected(frame, VideoFrameQuality.builder()
                            .valid(true)
                            .brightness(0)
                            .sharpnessScore(0)
                            .qualityScore(0)
                            .blackFrame(false)
                            .tooDark(false)
                            .tooBright(false)
                            .tooBlurred(false)
                            .rejectReason(null)
                            .build());
                    selectedFrames.add(new SelectedFrame(frame, ImageHash.builder().value(0L).hex("0000000000000000").build()));
                    break;
                }
            }
        }

        return frames;
    }

    private boolean isDuplicate(ImageHash imageHash, List<SelectedFrame> selectedFrames) {
        for (SelectedFrame selectedFrame : selectedFrames) {
            int distance = imageHash.distanceTo(selectedFrame.imageHash());
            if (distance <= videoFrameProperties.getDuplicateHammingThreshold()) {
                return true;
            }
        }
        return false;
    }

    private ImageHash calculateHash(String filePath) {
        try {
            if (filePath == null || filePath.isBlank()) {
                return ImageHash.builder().value(0L).hex("0000000000000000").build();
            }
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                return ImageHash.builder().value(0L).hex("0000000000000000").build();
            }
            BufferedImage image = ImageIO.read(path.toFile());
            return averageHashCalculator.calculate(image, videoFrameProperties.getHashSize());
        } catch (Exception exception) {
            return ImageHash.builder().value(0L).hex("0000000000000000").build();
        }
    }

    private void applyQuality(VideoFrameEntity frame, VideoFrameQuality quality) {
        frame.setBrightness(quality.getBrightness());
        frame.setSharpnessScore(quality.getSharpnessScore());
        frame.setQualityScore(quality.getQualityScore());
        frame.setUpdateTime(LocalDateTime.now());
    }

    private void markSelected(VideoFrameEntity frame, VideoFrameQuality quality) {
        frame.setKeyFrameSelected(1);
        frame.setKeyFrameScore(quality.getQualityScore());
        frame.setRejectReason(null);
        frame.setUpdateTime(LocalDateTime.now());
    }

    private void markRejected(VideoFrameEntity frame, String reason) {
        frame.setKeyFrameSelected(0);
        frame.setKeyFrameScore(0.0);
        frame.setRejectReason(reason);
        frame.setUpdateTime(LocalDateTime.now());
    }

    private int finalKeyFrameCount() {
        return Math.max(1, videoFrameProperties.getFinalKeyFrameCount());
    }

    private record SelectedFrame(VideoFrameEntity frame, ImageHash imageHash) {
    }
}
