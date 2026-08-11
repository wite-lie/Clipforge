package com.clipforge.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clipforge.infra.persistence.typehandler.JsonbStringTypeHandler;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationPlanVersionEntity（创作方案版本实体）。
 *
 * <p>该实体映射 cf_creation_plan_version（创作方案版本表），用于保存一次运行中的初始方案、修正版和最终版标记。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "cf_creation_plan_version", autoResultMap = true)
public class CreationPlanVersionEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** versionId（版本编号）是对外展示和关联使用的方案版本编号。 */
    private String versionId;

    /** runId（运行编号）用于关联一次 Agent（智能体）执行。 */
    private String runId;

    /** taskId（任务编号）用于关联创作任务，可以为空。 */
    private String taskId;

    /** projectId（项目编号）用于项目级质量复盘。 */
    private String projectId;

    /** conversationId（会话编号）用于会话级质量复盘。 */
    private String conversationId;

    /** versionNo（版本序号）表示该运行下第几个方案版本。 */
    private Integer versionNo;

    /** versionType（版本类型）表示 INITIAL（初始生成）或 REVISED（修正版）。 */
    private String versionType;

    /** sourceVersionId（来源版本编号）表示修正版来源于哪个版本。 */
    private String sourceVersionId;

    /** planJson（方案 JSON）保存完整创作方案。 */
    @TableField(value = "plan_json", typeHandler = JsonbStringTypeHandler.class)
    private String planJson;

    /** score（评分）保存该版本综合评分。 */
    private Integer score;

    /** createdByPurpose（创建用途）表示创建该版本的模型调用用途。 */
    private String createdByPurpose;

    /** modelCallRecordId（模型调用记录编号）用于关联创建该版本的模型调用记录。 */
    private String modelCallRecordId;

    /** finalVersion（是否最终版本）表示本次运行最终采用该版本。 */
    private Boolean finalVersion;

    /** createTime（创建时间）保存版本创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）保存版本更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
