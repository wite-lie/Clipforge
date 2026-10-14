package com.clipforge.material.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clipforge.material.infrastructure.persistence.typehandler.JsonbStringTypeHandler;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MaterialUnderstandingEntity（素材理解结果实体）。
 *
 * <p>该实体映射 cf_material_understanding（素材理解结果表），用于保存图片、视频候选帧、OCR（光学字符识别）等基础素材理解结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "cf_material_understanding", autoResultMap = true)
public class MaterialUnderstandingEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** understandingId（理解结果编号）是对外查询素材理解结果时使用的编号。 */
    private String understandingId;

    /** materialId（素材编号）表示该理解结果对应的素材。 */
    private String materialId;

    /** materialType（素材类型）表示 IMAGE（图片）等素材类型。 */
    private String materialType;

    /** understandingType（理解类型）表示 IMAGE_VISION（图片视觉理解）等类型。 */
    private String understandingType;

    /** status（理解状态）表示 PROCESSING（处理中）、SUCCEEDED（成功）或 FAILED（失败）。 */
    private String status;

    /** modelProvider（模型供应商）表示本次理解调用的模型供应商。 */
    private String modelProvider;

    /** modelName（模型名称）表示本次理解调用的模型名称。 */
    private String modelName;

    /** promptVersion（提示词版本）用于判断理解结果是否可复用。 */
    private String promptVersion;

    /** contentHash（内容哈希）用于判断素材文件内容是否变化。 */
    private String contentHash;

    /** promptHash（提示词哈希）用于记录本次提示词快照的哈希。 */
    private String promptHash;

    /** contentSummary（内容摘要）表示面向创作规划的单素材摘要。 */
    private String contentSummary;

    /** structuredResultJson（结构化结果 JSON）保存 ImageUnderstandingResult（图片理解结果）等结构化内容。 */
    @TableField(value = "structured_result_json", typeHandler = JsonbStringTypeHandler.class)
    private String structuredResultJson;

    /** rawResponseJson（原始响应 JSON）保存模型原始响应的 JSON 包装。 */
    @TableField(value = "raw_response_json", typeHandler = JsonbStringTypeHandler.class)
    private String rawResponseJson;

    /** promptSnapshot（提示词快照）保存本次理解使用的提示词摘要。 */
    private String promptSnapshot;

    /** inputTokenCount（输入 Token 数）保存模型输入消耗。 */
    private Integer inputTokenCount;

    /** outputTokenCount（输出 Token 数）保存模型输出消耗。 */
    private Integer outputTokenCount;

    /** totalTokenCount（总 Token 数）保存模型总消耗。 */
    private Integer totalTokenCount;

    /** errorCode（错误码）保存失败时的机器可读错误编码。 */
    private String errorCode;

    /** errorMessage（错误消息）保存失败时的人类可读错误信息。 */
    private String errorMessage;

    /** startedAt（开始时间）表示素材理解开始时间。 */
    private LocalDateTime startedAt;

    /** endedAt（结束时间）表示素材理解结束时间。 */
    private LocalDateTime endedAt;

    /** createTime（创建时间）表示记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
