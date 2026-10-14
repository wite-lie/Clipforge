package com.clipforge.contract.creation;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationMarkdownExportResponse（创作 Markdown 导出响应）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationMarkdownExportResponse {

    /** exportId（导出编号）用于查询和下载导出文件。 */
    private String exportId;

    /** runId（运行编号）表示导出来源 Agent Run（智能体运行）。 */
    private String runId;

    /** outputId（产物编号）表示导出来源通用创作产物。 */
    private String outputId;

    /** outputType（产物类型）表示导出的内容形态。 */
    private CreationOutputType outputType;

    /** exportFormat（导出格式）当前固定为 MARKDOWN。 */
    private String exportFormat;

    /** fileName（文件名）表示导出的 Markdown 文件名。 */
    private String fileName;

    /** filePath（文件路径）表示服务端本地保存路径。 */
    private String filePath;

    /** contentType（内容类型）表示下载时使用的 MIME 类型。 */
    private String contentType;

    /** sizeBytes（文件大小）表示导出文件字节数。 */
    private Long sizeBytes;

    /** status（导出状态）表示 SUCCEEDED（成功）或 FAILED（失败）。 */
    private String status;

    /** errorMessage（错误信息）表示导出失败原因。 */
    private String errorMessage;

    /** createTime（创建时间）表示导出记录创建时间。 */
    private LocalDateTime createTime;
}
