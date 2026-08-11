package com.clipforge.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeDocumentResponse（知识文档响应）。
 *
 * <p>该对象用于接口返回 KnowledgeDocument（知识文档）的文件元数据和处理状态。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentResponse {

    /** documentId（知识文档编号）是对外使用的文档唯一编号。 */
    private String documentId;

    /** knowledgeBaseId（知识库编号）表示文档归属的知识库。 */
    private String knowledgeBaseId;

    /** userId（用户编号）表示文档所属用户。 */
    private String userId;

    /** sourceType（来源类型）当前上传接口写入 LOCAL_UPLOAD（本地上传）。 */
    private String sourceType;

    /** sourceLocation（来源位置）URL（统一资源定位符）导入时保存原始地址，本地上传时可以为空。 */
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

    /** parseStatus（解析状态）表示文档解析是否完成。 */
    private String parseStatus;

    /** chunkStatus（切片状态）表示文档切片是否完成。 */
    private String chunkStatus;

    /** embeddingStatus（嵌入状态）表示向量化是否完成。 */
    private String embeddingStatus;

    /** overallStatus（整体状态）表示文档当前是否可检索。 */
    private String overallStatus;

    /** errorMessage（错误消息）保存最近一次处理失败原因。 */
    private String errorMessage;

    /** chunkStrategy（切片策略）表示该文档后续处理时使用的切片方式。 */
    private String chunkStrategy;

    /** chunkConfigJson（切片配置 JSON）表示该文档后续处理时使用的切片配置。 */
    private String chunkConfigJson;

    /** metadataJson（元数据 JSON）保存文档上传来源、扩展名等补充信息。 */
    private String metadataJson;

    /** chunkCount（切片数量）表示当前有效切片数量。 */
    private Integer chunkCount;

    /** charCount（字符数）表示解析后文本字符数。 */
    private Integer charCount;

    /** tokenCount（词元数）表示估算 Token（词元）数量。 */
    private Integer tokenCount;

    /** enabled（是否启用）1 表示启用，0 表示禁用。 */
    private Integer enabled;

    /** createTime（创建时间）表示文档记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示文档记录最近更新时间。 */
    private LocalDateTime updateTime;
}
