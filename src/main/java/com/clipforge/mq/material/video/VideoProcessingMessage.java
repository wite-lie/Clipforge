package com.clipforge.mq.material.video;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoProcessingMessage（视频处理消息）。
 *
 * <p>该消息用于 RocketMQ（消息队列）中传递“处理某个视频素材”的异步任务。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoProcessingMessage {

    /** messageId（消息编号）表示业务侧生成的消息唯一标识。 */
    private String messageId;

    /** processTaskId（处理任务编号）表示 cf_material_process_task（素材处理任务表）中的业务编号。 */
    private String processTaskId;

    /** materialId（素材编号）表示要处理的视频素材。 */
    private String materialId;

    /** userId（用户编号）表示素材所属用户。 */
    private String userId;

    /** projectId（项目编号）表示素材所属项目。 */
    private String projectId;

    /** conversationId（会话编号）表示素材所属会话。 */
    private String conversationId;

    /** runId（运行编号）表示素材关联的 Agent Run（智能体运行）。 */
    private String runId;

    /** uploadBatchId（上传批次编号）表示前端一次多文件上传的批次。 */
    private String uploadBatchId;

    /** instruction（理解指令）表示视频理解任务目标。 */
    private String instruction;

    /** createdAt（创建时间）表示消息创建时间。 */
    private LocalDateTime createdAt;
}
