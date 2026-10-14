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
 * ProjectMemoryEntity（项目记忆实体）。
 *
 * <p>该实体映射 cf_project_memory（项目记忆表），用于保存项目级创作定位、受众和内容风格。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "cf_project_memory", autoResultMap = true)
public class ProjectMemoryEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** memoryId（记忆编号）是对外使用的唯一编号。 */
    private String memoryId;

    /** projectId（项目编号）表示该记忆属于哪个 Project（项目）。 */
    private String projectId;

    /** userId（用户编号）表示该项目记忆归属的用户。 */
    private String userId;

    /** summary（摘要）保存项目长期定位。 */
    private String summary;

    /** targetPlatform（目标平台）保存项目常用平台。 */
    private String targetPlatform;

    /** targetAudience（目标受众）保存项目长期面向的人群。 */
    private String targetAudience;

    /** contentStyle（内容风格）保存项目稳定的内容调性。 */
    private String contentStyle;

    /** constraintsJson（约束 JSON）保存项目级额外约束。 */
    @TableField(value = "constraints_json", typeHandler = JsonbStringTypeHandler.class)
    private String constraintsJson;

    /** status（状态）表示项目记忆是否启用。 */
    private String status;

    /** createTime（创建时间）表示记录创建时刻。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近一次更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
