package com.clipforge.model.rerank;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RerankModelResponse（重排序模型响应）。
 *
 * <p>该对象统一包装专用 Rerank Model（重排序模型）的返回结果和用量信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RerankModelResponse {

    /** provider（供应商）表示本次调用的模型供应商。 */
    private String provider;

    /** modelName（模型名称）表示本次调用的重排序模型。 */
    private String modelName;

    /** results（重排序结果）表示按相关性排序后的候选得分项。 */
    private List<RerankScoredItem> results;

    /** promptTokens（输入词元数）表示供应商返回或本地估算的输入 Token（词元）数量。 */
    private Integer promptTokens;

    /** totalTokens（总词元数）表示供应商返回或本地估算的总 Token（词元）数量。 */
    private Integer totalTokens;
}
