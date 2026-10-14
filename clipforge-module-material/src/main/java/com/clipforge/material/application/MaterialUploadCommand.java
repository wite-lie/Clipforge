package com.clipforge.material.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MaterialUploadCommand（素材上传命令）。
 *
 * <p>该命令对象承载素材上传需要的业务字段和文件内容流，供接口层、任务层或未来 MCP（模型上下文协议）适配层统一调用。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialUploadCommand {

    /** materialType（素材类型）表示 IMAGE（图片）或 VIDEO（视频）。 */
    private String materialType;

    /** originalFilename（原始文件名）表示客户端上传时携带的文件名。 */
    private String originalFilename;

    /** contentType（内容类型）表示文件 MIME（媒体类型）。 */
    private String contentType;

    /** fileSize（文件大小）表示上传文件字节数。 */
    private Long fileSize;

    /** userId（用户编号）表示素材所属用户。 */
    private String userId;

    /** projectId（项目编号）表示素材所属项目。 */
    private String projectId;

    /** conversationId（会话编号）表示素材所属会话。 */
    private String conversationId;

    /** runId（运行编号）表示素材关联的创作运行。 */
    private String runId;

    /** uploadBatchId（上传批次编号）表示前端一次选择多文件上传时的批次。 */
    private String uploadBatchId;

    /** content（文件内容）表示可打开的上传文件输入流。 */
    private MaterialUploadContent content;
}
