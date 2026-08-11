package com.clipforge.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeChunkResponse（知识切片响应）。
 *
 * <p>该对象用于接口返回 KnowledgeChunk（知识切片）的正文、标题路径、统计信息和元数据。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunkResponse {

    /** chunkId（知识切片编号）是对外使用的切片唯一编号。 */
    private String chunkId;

    /** knowledgeBaseId（知识库编号）表示切片归属的知识库。 */
    private String knowledgeBaseId;

    /** documentId（知识文档编号）表示切片归属的文档。 */
    private String documentId;

    /** chunkIndex（切片序号）用于保持切片在原文中的顺序。 */
    private Integer chunkIndex;

    /** titlePath（标题路径）表示切片所在的 Markdown（标记文档）标题层级。 */
    private String titlePath;

    /** content（切片正文）表示可检索的文本片段。 */
    private String content;

    /** contentHash（内容哈希）用于后续变更检测和人工精修判断。 */
    private String contentHash;

    /** charCount（字符数）表示切片正文字符数。 */
    private Integer charCount;

    /** tokenCount（词元数）表示估算 Token（词元）数量。 */
    private Integer tokenCount;

    /** metadataJson（元数据 JSON）保存切片策略、来源文件和解析信息。 */
    private String metadataJson;

    /** enabled（是否启用）1 表示启用，0 表示禁用。 */
    private Integer enabled;

    /** createTime（创建时间）表示切片记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示切片记录最近更新时间。 */
    private LocalDateTime updateTime;
}
