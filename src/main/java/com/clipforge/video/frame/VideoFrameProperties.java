package com.clipforge.video.frame;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * VideoFrameProperties（视频帧配置属性）。
 *
 * <p>该配置集中管理候选帧输出目录、数量、边界时间和图片质量。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.video.frame")
public class VideoFrameProperties {

    /** outputRootPath（输出根路径）表示候选帧图片本地保存目录。 */
    private String outputRootPath = "data/uploads/video-frames";

    /** maxFrameCount（最大帧数量）表示单个视频最多抽取多少张候选帧。 */
    private int maxFrameCount = 8;

    /** rawIntervalSeconds（原始抽帧间隔秒数）表示优化后每隔多少秒抽一张原始候选帧。 */
    private double rawIntervalSeconds = 3;

    /** maxRawFrameCount（最大原始候选帧数量）用于限制较长视频的抽帧成本。 */
    private int maxRawFrameCount = 30;

    /** finalKeyFrameCount（最终关键帧数量）表示筛选后最多保留多少张关键帧。 */
    private int finalKeyFrameCount = 8;

    /** minBrightness（最低亮度）用于过滤过暗或近似黑屏画面。 */
    private double minBrightness = 30;

    /** maxBrightness（最高亮度）用于过滤过亮或近似白屏画面。 */
    private double maxBrightness = 225;

    /** minSharpnessScore（最低清晰度分数）用于过滤明显模糊或信息量过低的画面。 */
    private double minSharpnessScore = 8;

    /** duplicateHammingThreshold（重复汉明距离阈值）用于判断两张平均哈希是否过于相似。 */
    private int duplicateHammingThreshold = 8;

    /** hashSize（哈希尺寸）表示平均哈希使用的缩放边长，默认 8 表示 8x8。 */
    private int hashSize = 8;

    /** startMarginSeconds（开头避让秒数）表示不从视频最开头抽帧。 */
    private double startMarginSeconds = 1;

    /** endMarginSeconds（结尾避让秒数）表示不从视频最后一瞬间抽帧。 */
    private double endMarginSeconds = 1;

    /** imageFormat（图片格式）表示候选帧图片后缀，当前默认 jpg。 */
    private String imageFormat = "jpg";

    /** jpegQuality（JPEG 质量）表示 FFmpeg -q:v 参数，数值越小质量越高。 */
    private int jpegQuality = 2;
}
