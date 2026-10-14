package com.clipforge.contract.knowledge;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ModelCallRecordView（模型调用记录视图）。
 *
 * <p>该对象用于接口返回单次模型调用的 Token（词元）、耗时和成功状态。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCallRecordView {

    /** recordId（记录编号）表示模型调用记录的业务编号。 */
    private String recordId;

    /** provider（供应商）表示模型服务来源。 */
    private String provider;

    /** modelName（模型名称）表示实际调用的模型。 */
    private String modelName;

    /** purpose（调用用途）表示生成、修复、评估等调用场景。 */
    private String purpose;

    /** promptTokens（输入词元数）表示提示词消耗。 */
    private Integer promptTokens;

    /** completionTokens（输出词元数）表示模型回答消耗。 */
    private Integer completionTokens;

    /** totalTokens（总词元数）表示输入和输出合计消耗。 */
    private Integer totalTokens;

    /** estimatedInputCost（估算输入成本）表示本次调用输入 Token（词元）的估算成本。 */
    private BigDecimal estimatedInputCost;

    /** estimatedOutputCost（估算输出成本）表示本次调用输出 Token（词元）的估算成本。 */
    private BigDecimal estimatedOutputCost;

    /** estimatedTotalCost（估算总成本）表示本次调用输入和输出合计估算成本。 */
    private BigDecimal estimatedTotalCost;

    /** currency（币种）表示成本估算使用的货币单位。 */
    private String currency;

    /** priceMatched（是否命中价格）表示本次调用是否找到了供应商和模型对应的价格配置。 */
    private Boolean priceMatched;

    /** latencyMs（调用耗时毫秒）表示模型请求耗时。 */
    private Long latencyMs;

    /** success（是否成功）表示模型调用是否成功。 */
    private Boolean success;

    /** errorCode（错误码）表示失败时的错误类型。 */
    private String errorCode;

    /** errorMessage（错误消息）表示失败时的简要原因。 */
    private String errorMessage;
}
