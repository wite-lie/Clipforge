package com.clipforge.memory.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clipforge.memory.infrastructure.persistence.typehandler.JsonbStringTypeHandler;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationOutputEntity（创作产物实体）。
 *
 * <p>该实体映射 cf_creation_output（通用创作产物表），用于保存短视频方案、图文方案等不同内容产物。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "cf_creation_output", autoResultMap = true)
public class CreationOutputEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** outputId（产物编号）用于对外标识一份创作产物。 */
    private String outputId;

    /** taskId（任务编号）用于关联创作任务。 */
    private String taskId;

    /** runId（运行编号）用于关联一次 Agent（智能体）执行。 */
    private String runId;

    /** outputType（产物类型）表示 VIDEO_PLAN（短视频方案）或 ARTICLE_PLAN（图文方案）。 */
    private String outputType;

    /** schemaVersion（结构版本）用于兼容后续产物 JSON 结构升级。 */
    private String schemaVersion;

    /** payloadJson（产物内容 JSON）保存完整结构化产物。 */
    @TableField(value = "payload_json", typeHandler = JsonbStringTypeHandler.class)
    private String payloadJson;

    /** createTime（创建时间）保存产物创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）保存产物最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
