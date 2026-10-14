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
 * CreationExportEntity（创作导出实体）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_creation_export")
public class CreationExportEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** exportId（导出编号）用于对外查询和下载。 */
    private String exportId;

    /** runId（运行编号）表示导出来源 Agent Run（智能体运行）。 */
    private String runId;

    /** outputId（产物编号）表示导出来源通用创作产物。 */
    private String outputId;

    /** outputType（产物类型）表示导出来源产物类型。 */
    private String outputType;

    /** exportFormat（导出格式）当前为 MARKDOWN。 */
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

    /** updateTime（更新时间）表示导出记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
