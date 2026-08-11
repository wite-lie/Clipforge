package com.clipforge.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clipforge.infra.persistence.typehandler.JsonbStringTypeHandler;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeDocumentEntity（知识文档实体）。
 *
 * <p>该实体映射 cf_knowledge_document（知识文档表），用于保存上传文件、URL（统一资源定位符）导入文件或系统种子文件的元数据和处理状态。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "cf_knowledge_document", autoResultMap = true)
public class KnowledgeDocumentEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** documentId（知识文档编号）是对外使用的唯一编号。 */
    private String documentId;

    /** knowledgeBaseId（知识库编号）表示文档归属的知识库。 */
    private String knowledgeBaseId;

    /** userId（用户编号）表示文档所属用户，系统文档可以为空。 */
    private String userId;

    /** sourceType（来源类型）当前上传接口写入 LOCAL_UPLOAD（本地上传）。 */
    private String sourceType;

    /** sourceLocation（来源位置）URL（统一资源定位符）导入时保存原始地址。 */
    private String sourceLocation;

    /** originalFilename（原始文件名）表示用户上传时的文件名。 */
    private String originalFilename;

    /** storedFilename（存储文件名）表示系统保存时生成的安全文件名。 */
    private String storedFilename;

    /** filePath（文件路径）表示文件在本地磁盘中的保存位置。 */
    private String filePath;

    /** contentType（内容类型）表示上传文件 MIME（媒体类型）。 */
    private String contentType;

    /** fileSize（文件大小）单位为字节。 */
    private Long fileSize;

    /** fileHash（文件哈希）用于后续去重和变更检测。 */
    private String fileHash;

    /** parseStatus（解析状态）表示 PENDING（待处理）、PROCESSING（处理中）、SUCCESS（成功）或 FAILED（失败）。 */
    private String parseStatus;

    /** chunkStatus（切片状态）表示 PENDING（待处理）、PROCESSING（处理中）、SUCCESS（成功）或 FAILED（失败）。 */
    private String chunkStatus;

    /** embeddingStatus（嵌入状态）表示 PENDING（待处理）、PROCESSING（处理中）、SUCCESS（成功）或 FAILED（失败）。 */
    private String embeddingStatus;

    /** overallStatus（整体状态）表示 PENDING（待处理）、PROCESSING（处理中）、CHUNKED（已切片）、READY（可检索）或 FAILED（失败）。 */
    private String overallStatus;

    /** errorMessage（错误消息）保存最近一次处理失败原因。 */
    private String errorMessage;

    /** chunkStrategy（切片策略）表示该文档后续处理时使用的切片方式。 */
    private String chunkStrategy;

    /** chunkConfigJson（切片配置 JSON）用于保存 JSONB（JSON 二进制类型）格式的切片配置。 */
    @TableField(value = "chunk_config_json", typeHandler = JsonbStringTypeHandler.class)
    private String chunkConfigJson;

    /** metadataJson（元数据 JSON）用于保存 JSONB（JSON 二进制类型）格式的文档扩展属性。 */
    @TableField(value = "metadata_json", typeHandler = JsonbStringTypeHandler.class)
    private String metadataJson;

    /** chunkCount（切片数量）表示当前有效切片数量。 */
    private Integer chunkCount;

    /** charCount（字符数）表示解析后文本字符数。 */
    private Integer charCount;

    /** tokenCount（词元数）表示估算 Token（词元）数量。 */
    private Integer tokenCount;

    /** enabled（是否启用）1 表示启用，0 表示禁用。 */
    private Integer enabled;

    /** scheduleEnabled（是否启用定时同步）当前本地上传文件默认为 0。 */
    private Integer scheduleEnabled;

    /** scheduleCron（定时同步表达式）当前本地上传文件为空。 */
    private String scheduleCron;

    /** lastEtag（最近 ETag）URL（统一资源定位符）导入时用于变更检测。 */
    private String lastEtag;

    /** lastModified（最近修改时间）URL（统一资源定位符）导入时用于变更检测。 */
    private String lastModified;

    /** lastContentHash（最近内容哈希）URL（统一资源定位符）导入时用于变更检测。 */
    private String lastContentHash;

    /** lastProcessTime（最近处理时间）表示文档最近一次处理完成时间。 */
    private LocalDateTime lastProcessTime;

    /** createTime（创建时间）表示记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
