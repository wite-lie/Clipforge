package com.clipforge.contract.material;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MaterialResponse（素材响应）。
 *
 * <p>该对象用于图片、视频上传和素材查询接口返回素材元数据。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialResponse {

    /** materialId（素材编号）表示素材唯一编号。 */
    private String materialId;

    /** userId（用户编号）表示素材所属用户。 */
    private String userId;

    /** projectId（项目编号）表示素材所属项目。 */
    private String projectId;

    /** conversationId（会话编号）表示素材所属会话。 */
    private String conversationId;

    /** runId（运行编号）表示素材关联的 Agent Run（智能体运行）。 */
    private String runId;

    /** materialType（素材类型）表示 IMAGE（图片）或 VIDEO（视频）。 */
    private String materialType;

    /** originalFilename（原始文件名）表示用户上传时的文件名。 */
    private String originalFilename;

    /** storedFilename（存储文件名）表示系统生成的安全文件名。 */
    private String storedFilename;

    /** filePath（文件路径）表示文件保存位置。 */
    private String filePath;

    /** contentType（内容类型）表示文件 MIME（媒体类型）。 */
    private String contentType;

    /** fileSize（文件大小）单位为字节。 */
    private Long fileSize;

    /** status（素材状态）表示 UPLOADED（已上传）等处理状态。 */
    private String status;

    /** uploadBatchId（上传批次编号）表示前端一次选择多个文件上传时的批次标识，当前阶段只在上传响应中透传。 */
    private String uploadBatchId;

    /** processTaskId（处理任务编号）表示前端查询素材处理进度时使用的任务编号，当前阶段由素材编号稳定派生。 */
    private String processTaskId;

    /** processStatus（处理状态）表示 PENDING（等待中）、PROCESSING（处理中）、SUCCEEDED（成功）或 FAILED（失败）。 */
    private String processStatus;

    /** processPhase（处理阶段）表示素材处理所处的粗粒度阶段。 */
    private String processPhase;

    /** progressPercent（进度百分比）用于前端展示上传后处理进度。 */
    private Integer progressPercent;

    /** canUseForCreation（是否可用于创作）表示当前素材是否已经具备注入创作 Workflow（工作流）的条件。 */
    private Boolean canUseForCreation;

    /** statusQueryUrl（状态查询地址）表示后续统一任务状态接口的建议查询地址。 */
    private String statusQueryUrl;

    /** nextAction（下一步动作）用于提示前端应该等待处理、重试处理或允许创作。 */
    private String nextAction;
}
