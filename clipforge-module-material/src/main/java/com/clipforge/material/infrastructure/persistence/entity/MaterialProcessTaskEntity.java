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
 * MaterialProcessTaskEntity（素材处理任务实体）。
 *
 * <p>该实体映射 cf_material_process_task（素材处理任务表），用于保存图片、视频上传后的处理任务状态。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_material_process_task")
public class MaterialProcessTaskEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** processTaskId（处理任务编号）是前端查询任务状态的业务编号。 */
    private String processTaskId;

    /** parentProcessTaskId（父处理任务编号）用于把视频子任务关联到同一个视频父任务。 */
    private String parentProcessTaskId;

    /** subTaskKey（子任务键）用于区分 FRAME_BRANCH（抽帧图像分支）、AUDIO_BRANCH（音频分支）和 SUMMARY（汇总分支）。 */
    private String subTaskKey;

    /** dependsOnTaskIds（依赖任务编号列表）用于后续描述子任务依赖关系，第一版保存 JSON（对象表示格式）字符串。 */
    private String dependsOnTaskIds;

    /** materialId（素材编号）表示任务关联的素材。 */
    private String materialId;

    /** uploadBatchId（上传批次编号）表示前端一次多文件上传的批次。 */
    private String uploadBatchId;

    /** userId（用户编号）表示素材所属用户。 */
    private String userId;

    /** projectId（项目编号）表示素材所属项目。 */
    private String projectId;

    /** conversationId（会话编号）表示素材所属会话。 */
    private String conversationId;

    /** runId（运行编号）表示素材关联的 Agent Run（智能体运行）。 */
    private String runId;

    /** taskType（任务类型）表示图片理解或视频处理等任务类型。 */
    private String taskType;

    /** bizType（业务类型）表示任务关联的业务对象类型。 */
    private String bizType;

    /** status（任务状态）表示 PENDING（等待中）、PROCESSING（处理中）等状态。 */
    private String status;

    /** phase（处理阶段）表示当前任务所处的粗粒度阶段。 */
    private String phase;

    /** progressPercent（进度百分比）用于前端展示任务进度。 */
    private Integer progressPercent;

    /** message（状态说明）用于返回人类可读的任务状态描述。 */
    private String message;

    /** errorCode（错误码）用于失败时返回机器可读错误编码。 */
    private String errorCode;

    /** errorMessage（错误消息）用于失败时返回人类可读失败原因。 */
    private String errorMessage;

    /** retryCount（重试次数）表示任务已经重试的次数。 */
    private Integer retryCount;

    /** canRetry（是否允许重试）0 表示否，1 表示是。 */
    private Integer canRetry;

    /** nextAction（下一步动作）用于提示前端下一步应该等待、允许创作或重试。 */
    private String nextAction;

    /** startedAt（开始时间）表示任务真正开始处理的时间，等待中可以为空。 */
    private LocalDateTime startedAt;

    /** updatedAt（状态更新时间）表示任务状态最近刷新时间。 */
    private LocalDateTime updatedAt;

    /** endedAt（结束时间）表示任务结束时间，未结束可以为空。 */
    private LocalDateTime endedAt;

    /** createTime（创建时间）表示记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
