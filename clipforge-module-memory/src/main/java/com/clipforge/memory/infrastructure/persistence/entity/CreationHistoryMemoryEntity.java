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
 * CreationHistoryMemoryEntity（创作历史记忆实体）。
 *
 * <p>该实体映射 cf_creation_history_memory（创作历史记忆表），用于保存历史高质量创作方案摘要。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "cf_creation_history_memory", autoResultMap = true)
public class CreationHistoryMemoryEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** memoryId（记忆编号）是对外使用的唯一编号。 */
    private String memoryId;

    /** userId（用户编号）表示历史记忆归属的用户。 */
    private String userId;

    /** projectId（项目编号）表示历史记忆归属的项目，可以为空。 */
    private String projectId;

    /** conversationId（会话编号）表示历史记忆来源会话，可以为空。 */
    private String conversationId;

    /** runId（运行编号）表示历史记忆来源的 Agent Run（智能体运行）。 */
    private String runId;

    /** theme（创作主题）保存历史方案主题。 */
    private String theme;

    /** platform（目标平台）保存历史方案面向的平台。 */
    private String platform;

    /** summary（摘要）保存历史方案可复用经验。 */
    private String summary;

    /** score（评分）保存历史方案最终评分。 */
    private Integer score;

    /** tagsJson（标签 JSON）保存主题、风格或经验标签。 */
    @TableField(value = "tags_json", typeHandler = JsonbStringTypeHandler.class)
    private String tagsJson;

    /** status（状态）表示历史记忆是否启用。 */
    private String status;

    /** createTime（创建时间）表示记录创建时刻。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近一次更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
