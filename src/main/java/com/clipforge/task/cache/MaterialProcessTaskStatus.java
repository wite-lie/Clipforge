package com.clipforge.task.cache;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MaterialProcessTaskStatus（素材处理任务缓存状态）。
 *
 * <p>该对象用于 Redis（缓存）中保存素材处理任务状态，Redis 第一版使用 String（字符串）类型保存 JSON（对象表示格式）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialProcessTaskStatus {

    /** taskId（任务编号）表示前端查询任务状态时使用的编号。 */
    private String taskId;

    /** taskType（任务类型）表示图片理解或视频处理。 */
    private String taskType;

    /** bizId（业务编号）表示任务关联的素材编号。 */
    private String bizId;

    /** bizType（业务类型）当前为 MATERIAL（素材）。 */
    private String bizType;

    /** status（任务状态）表示 PENDING（等待中）、PROCESSING（处理中）、SUCCEEDED（成功）或 FAILED（失败）。 */
    private String status;

    /** phase（处理阶段）表示当前任务所处的粗粒度阶段。 */
    private String phase;

    /** progressPercent（进度百分比）用于前端展示任务处理进度。 */
    private Integer progressPercent;

    /** message（状态说明）用于展示人类可读的任务状态。 */
    private String message;

    /** errorCode（错误码）用于失败时展示机器可读错误编码。 */
    private String errorCode;

    /** errorMessage（错误消息）用于失败时展示人类可读失败原因。 */
    private String errorMessage;

    /** retryCount（重试次数）表示任务已经重试的次数。 */
    private Integer retryCount;

    /** canRetry（是否允许重试）表示前端是否可以展示重试入口。 */
    private Boolean canRetry;

    /** nextAction（下一步动作）用于提示前端等待处理、允许创作或重试处理。 */
    private String nextAction;

    /** startedAt（开始时间）表示任务真正开始处理的时间。 */
    private LocalDateTime startedAt;

    /** updatedAt（更新时间）表示任务状态最近更新时间。 */
    private LocalDateTime updatedAt;

    /** endedAt（结束时间）表示任务结束时间。 */
    private LocalDateTime endedAt;
}
