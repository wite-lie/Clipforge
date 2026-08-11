package com.clipforge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoFrameResponse（视频候选帧响应）。
 *
 * <p>该对象用于视频抽帧和候选帧查询接口返回帧图片信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFrameResponse {

    /** frameId（帧编号）表示候选帧业务编号。 */
    private String frameId;

    /** materialId（素材编号）表示原视频素材编号。 */
    private String materialId;

    /** frameMaterialId（帧素材编号）表示候选帧图片注册为 IMAGE（图片）素材后的素材编号。 */
    private String frameMaterialId;

    /** frameIndex（帧序号）表示候选帧顺序。 */
    private Integer frameIndex;

    /** timestampSeconds（时间点秒数）表示候选帧在视频中的时间位置。 */
    private Double timestampSeconds;

    /** filePath（文件路径）表示候选帧图片保存位置。 */
    private String filePath;

    /** width（宽度）表示候选帧图片宽度，单位像素。 */
    private Integer width;

    /** height（高度）表示候选帧图片高度，单位像素。 */
    private Integer height;

    /** extractStatus（抽取状态）表示候选帧抽取结果。 */
    private String extractStatus;

    /** errorMessage（错误消息）表示抽取失败原因。 */
    private String errorMessage;

    /** keyFrameSelected（关键帧是否入选）表示该候选帧是否最终用于视频理解。 */
    private Boolean keyFrameSelected;

    /** keyFrameScore（关键帧评分）用于前端展示关键帧筛选依据。 */
    private Double keyFrameScore;

    /** qualityScore（质量分）表示候选帧画面质量。 */
    private Double qualityScore;

    /** brightness（平均亮度）表示候选帧整体明暗程度。 */
    private Double brightness;

    /** sharpnessScore（清晰度分数）表示候选帧清晰程度。 */
    private Double sharpnessScore;

    /** imageHash（图像哈希）用于调试相似帧去重。 */
    private String imageHash;

    /** rejectReason（过滤原因）表示候选帧未入选最终关键帧的原因。 */
    private String rejectReason;
}
