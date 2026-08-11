package com.clipforge.mq.material.video;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoSubTaskProcessingMessage（视频子任务处理消息）。
 *
 * <p>该消息用于 RocketMQ（消息队列）中传递 FRAME_BRANCH（抽帧图像分支）、AUDIO_BRANCH（音频分支）和 SUMMARY（汇总）子任务。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSubTaskProcessingMessage {

    /** messageId（消息编号）表示业务侧生成的消息唯一标识。 */
    private String messageId;

    /** parentProcessTaskId（父处理任务编号）表示视频父任务编号。 */
    private String parentProcessTaskId;

    /** processTaskId（处理任务编号）表示当前子任务编号。 */
    private String processTaskId;

    /** materialId（素材编号）表示原视频素材编号。 */
    private String materialId;

    /** subTaskType（子任务类型）表示 FRAME_BRANCH、AUDIO_BRANCH 或 SUMMARY。 */
    private String subTaskType;

    /** runId（运行编号）表示素材关联的 Agent Run（智能体运行）。 */
    private String runId;

    /** instruction（理解指令）表示视频处理任务目标。 */
    private String instruction;

    /** createdAt（创建时间）表示消息创建时间。 */
    private LocalDateTime createdAt;
}
