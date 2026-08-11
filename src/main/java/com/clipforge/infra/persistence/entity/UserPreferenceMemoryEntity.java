package com.clipforge.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UserPreferenceMemoryEntity（用户偏好记忆实体）。
 *
 * <p>该实体映射 cf_user_preference_memory（用户偏好记忆表），用于保存跨项目生效的用户长期偏好。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_user_preference_memory")
public class UserPreferenceMemoryEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** memoryId（记忆编号）是对外使用的唯一编号。 */
    private String memoryId;

    /** userId（用户编号）表示该偏好属于哪个用户。 */
    private String userId;

    /** memoryType（记忆类型）表示风格、平台、受众、主题或负向偏好。 */
    private String memoryType;

    /** memoryKey（记忆键）表示偏好的具体维度。 */
    private String memoryKey;

    /** memoryValue（记忆值）保存可注入 Prompt（提示词）的偏好内容。 */
    private String memoryValue;

    /** confidence（置信度）表示该记忆可信程度。 */
    private BigDecimal confidence;

    /** sourceType（来源类型）表示该记忆来自用户明确表达、系统推断或人工维护。 */
    private String sourceType;

    /** sourceRunId（来源运行编号）记录沉淀该记忆的 Agent Run（智能体运行）。 */
    private String sourceRunId;

    /** status（状态）表示记忆是否启用。 */
    private String status;

    /** createTime（创建时间）表示记录创建时刻。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近一次更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
