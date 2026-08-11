package com.clipforge.video.frame;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoFrameQuality（视频帧质量结果）。
 *
 * <p>该对象保存单张候选帧的亮度、清晰度和过滤判断结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFrameQuality {

    /** valid（是否有效）表示该帧是否通过基础质量过滤。 */
    private boolean valid;

    /** brightness（平均亮度）表示图片整体明暗程度。 */
    private double brightness;

    /** sharpnessScore（清晰度分数）表示相邻像素灰度差均值。 */
    private double sharpnessScore;

    /** qualityScore（质量分）表示当前帧综合质量。 */
    private double qualityScore;

    /** blackFrame（是否黑屏）表示图片亮度极低且细节很少。 */
    private boolean blackFrame;

    /** tooDark（是否过暗）表示平均亮度低于配置阈值。 */
    private boolean tooDark;

    /** tooBright（是否过亮）表示平均亮度高于配置阈值。 */
    private boolean tooBright;

    /** tooBlurred（是否模糊）表示清晰度分数低于配置阈值。 */
    private boolean tooBlurred;

    /** rejectReason（过滤原因）表示未通过质量过滤的具体原因。 */
    private String rejectReason;
}
