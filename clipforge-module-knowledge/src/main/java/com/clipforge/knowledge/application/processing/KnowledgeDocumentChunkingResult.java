package com.clipforge.knowledge.application.processing;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KnowledgeDocumentChunkingResult（知识文档解析切片结果）。
 *
 * <p>该对象是 Knowledge 模块内部结果，不承载 HTTP 响应格式。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentChunkingResult {

    /** document（文档实体）表示完成解析切片后的最新文档状态。 */
    private KnowledgeDocumentEntity document;

    /** chunksPreview（切片预览）用于调试和上层适配器展示少量切片。 */
    private List<KnowledgeChunkEntity> chunksPreview;

    /** chunkCount（切片数量）表示本次新生成的有效切片数量。 */
    private Integer chunkCount;

    /** charCount（字符数）表示解析后正文字符数。 */
    private Integer charCount;

    /** tokenCount（估算词元数）表示解析后正文的粗略 Token 数量。 */
    private Integer tokenCount;
}
