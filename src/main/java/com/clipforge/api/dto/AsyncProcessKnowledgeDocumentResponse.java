package com.clipforge.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AsyncProcessKnowledgeDocumentResponse（异步处理知识文档响应）。
 *
 * <p>该对象用于在接口快速提交 RocketMQ（消息队列）文档处理任务后，返回可用于查询进度的 documentId（文档编号）和 processLogId（处理日志编号）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncProcessKnowledgeDocumentResponse {

    /** documentId（知识文档编号）表示本次提交处理的目标文档。 */
    private String documentId;

    /** knowledgeBaseId（知识库编号）表示目标文档所属知识库。 */
    private String knowledgeBaseId;

    /** processLogId（处理日志编号）用于查询本次异步处理任务状态和处理日志。 */
    private String processLogId;

    /** messageId（消息编号）用于定位 RocketMQ（消息队列）中的业务消息。 */
    private String messageId;

    /** status（处理状态）表示接口返回时任务已经进入 RUNNING（运行中）。 */
    private String status;

    /** phase（处理阶段）表示接口返回时任务处于 PARSE_CHUNK（解析切片）阶段。 */
    private String phase;

    /** message（响应消息）用于说明异步任务已经提交。 */
    private String message;

    /** acceptedAt（接收时间）表示服务端接收异步处理请求的时间。 */
    private LocalDateTime acceptedAt;

    /** statusUrl（状态查询地址）用于前端或测试按处理日志编号轮询任务状态。 */
    private String statusUrl;
}
