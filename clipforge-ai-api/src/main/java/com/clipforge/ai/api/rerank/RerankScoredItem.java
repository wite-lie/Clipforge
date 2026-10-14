package com.clipforge.ai.api.rerank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RerankScoredItem（重排序得分项）。
 *
 * <p>该对象表示 Rerank Model（重排序模型）对某个候选文档给出的相关性分数。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RerankScoredItem {

    /** index（索引）表示该结果对应输入 candidates（候选列表）中的位置。 */
    private Integer index;

    /** candidateId（候选编号）表示业务侧候选标识，当前通常是 chunkId（切片编号）。 */
    private String candidateId;

    /** relevanceScore（相关性分数）表示模型输出的相关性得分，越大越相关。 */
    private Double relevanceScore;

    /** documentText（文档文本）表示供应商返回的候选文档文本，可为空。 */
    private String documentText;
}
