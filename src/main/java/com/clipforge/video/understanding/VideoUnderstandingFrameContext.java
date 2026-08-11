package com.clipforge.video.understanding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoUnderstandingFrameContext（视频理解帧上下文）。
 *
 * <p>该对象描述一个候选帧在视频中的位置，以及它注册成图片素材后的素材编号。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUnderstandingFrameContext {

    /** frameId（帧编号）表示候选帧业务编号。 */
    private String frameId;

    /** frameMaterialId（帧素材编号）表示候选帧对应的 IMAGE（图片）素材编号。 */
    private String frameMaterialId;

    /** frameIndex（帧序号）表示候选帧顺序。 */
    private Integer frameIndex;

    /** timestampSeconds（时间点秒数）表示候选帧在视频中的位置。 */
    private Double timestampSeconds;

    /** filePath（文件路径）表示候选帧图片文件位置。 */
    private String filePath;

    /** keyFrameSelected（关键帧是否入选）1 表示最终入选，0 表示未入选。 */
    private Integer keyFrameSelected;

    /** keyFrameScore（关键帧评分）用于高光推荐排序。 */
    private Double keyFrameScore;

    /** qualityScore（质量分）表示亮度和清晰度综合质量。 */
    private Double qualityScore;

    /** rejectReason（过滤原因）表示候选帧未入选最终关键帧的原因。 */
    private String rejectReason;
}
