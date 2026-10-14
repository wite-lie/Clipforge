package com.clipforge.ai.api.rerank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RerankCandidate（重排序候选）。
 *
 * <p>该对象表示送入 Rerank Model（重排序模型）的一个候选文档，index（索引）用于和模型返回结果对齐。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RerankCandidate {

    /** index（索引）表示候选在本次请求中的位置。 */
    private Integer index;

    /** candidateId（候选编号）表示业务侧的候选标识，当前通常是 chunkId（切片编号）。 */
    private String candidateId;

    /** text（文本）表示送入 Rerank Model（重排序模型）的候选正文。 */
    private String text;
}
