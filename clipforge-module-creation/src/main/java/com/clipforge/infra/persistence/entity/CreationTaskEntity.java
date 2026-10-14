package com.clipforge.memory.infrastructure.persistence.entity;

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
 * CreationTaskEntity（创作任务实体）。
 *
 * <p>该实体映射数据库表 cf_creation_task（创作任务表），用于保存一次创作请求的基础输入和任务状态。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_creation_task")
public class CreationTaskEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** taskId（任务编号）是对外查询创作任务时使用的编号。 */
    private String taskId;

    /** runId（运行编号）用于串联 Agent（智能体）结果和执行链路。 */
    private String runId;

    /** goal（创作目标）保存用户输入的创作目标。 */
    private String goal;

    /** platform（目标平台）保存 douyin/xiaohongshu/bilibili/tiktok 等平台标识。 */
    private String platform;

    /** stylePreference（风格偏好）保存用户可选的风格要求。 */
    private String stylePreference;

    /** status（任务状态）保存 CREATED/RUNNING/SUCCEEDED/FAILED 等状态。 */
    private String status;

    /** errorMessage（错误消息）保存任务失败时的原因。 */
    private String errorMessage;

    /** createTime（创建时间）保存任务创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）保存任务最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
