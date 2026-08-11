package com.clipforge.model.rerank;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RerankModelRequest（重排序模型请求）。
 *
 * <p>该对象承载一次专用 Rerank Model（重排序模型）调用所需的 query（查询文本）、documents（候选文档）和 topN（保留数量）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RerankModelRequest {

    /** query（查询文本）表示用户问题或检索意图。 */
    private String query;

    /** candidates（候选列表）表示混合检索召回后需要重排序的候选文档。 */
    private List<RerankCandidate> candidates;

    /** topN（保留数量）表示重排序后最多返回多少条结果。 */
    private Integer topN;

    /** returnDocuments（返回文档）表示是否要求供应商返回命中的文档文本。 */
    private Boolean returnDocuments;
}
