package com.clipforge.contract.knowledge;

import com.clipforge.contract.knowledge.KnowledgeDocumentResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ProcessKnowledgeDocumentResponse（处理知识文档响应）。
 *
 * <p>该对象用于返回一次同步文档解析和切片后的结果摘要，方便本地调试和接口观察。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessKnowledgeDocumentResponse {

    /** document（知识文档响应）表示处理后的文档状态。 */
    private KnowledgeDocumentResponse document;

    /** chunksPreview（切片预览列表）表示本次生成的前若干个切片，避免响应过大。 */
    private List<KnowledgeChunkResponse> chunksPreview;
}
