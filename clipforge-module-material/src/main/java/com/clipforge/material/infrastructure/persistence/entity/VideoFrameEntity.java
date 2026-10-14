package com.clipforge.material.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoFrameEntity（视频候选帧实体）。
 *
 * <p>该实体映射 cf_video_frame（视频候选帧表），用于保存 FFmpeg（音视频处理工具）抽取出的帧图片。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_video_frame")
public class VideoFrameEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** frameId（帧编号）是候选帧业务编号。 */
    private String frameId;

    /** materialId（素材编号）表示原视频素材编号。 */
    private String materialId;

    /** frameMaterialId（帧素材编号）表示该候选帧注册到 cf_material（素材表）后的图片素材编号。 */
    private String frameMaterialId;

    /** frameIndex（帧序号）表示候选帧在本次抽帧结果中的顺序。 */
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

    /** keyFrameSelected（关键帧是否入选）1 表示最终入选，0 表示未入选。 */
    private Integer keyFrameSelected;

    /** keyFrameScore（关键帧评分）用于解释筛选顺序和入选优先级。 */
    private Double keyFrameScore;

    /** qualityScore（质量分）表示亮度和清晰度综合质量。 */
    private Double qualityScore;

    /** brightness（平均亮度）表示候选帧整体明暗程度。 */
    private Double brightness;

    /** sharpnessScore（清晰度分数）表示候选帧边缘和细节粗略丰富程度。 */
    private Double sharpnessScore;

    /** imageHash（图像哈希）用于相似帧去重。 */
    private String imageHash;

    /** rejectReason（过滤原因）表示候选帧未入选最终关键帧的原因。 */
    private String rejectReason;

    /** createTime（创建时间）表示记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
