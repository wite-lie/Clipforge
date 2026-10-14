package com.clipforge.ai.api.vision;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VisionModelResponse（视觉模型响应）。
 *
 * <p>该对象封装 VLM（视觉语言模型）返回的原始文本、结构化图片理解结果和 Token（词元）用量。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisionModelResponse {

    /** provider（供应商）表示本次响应来自哪个模型供应商。 */
    private String provider;

    /** modelName（模型名称）表示本次调用使用的视觉模型名称。 */
    private String modelName;

    /** rawContent（原始内容）表示模型返回的未经业务加工的文本。 */
    private String rawContent;

    /** results（图片理解结果列表）表示每张图片对应的结构化理解结果。 */
    private List<ImageUnderstandingResult> results;

    /** promptTokens（输入 Token 数）表示提示词和图片输入折算消耗的 Token（词元）数量，未知时可以为空。 */
    private Integer promptTokens;

    /** completionTokens（输出 Token 数）表示模型回答消耗的 Token（词元）数量，未知时可以为空。 */
    private Integer completionTokens;

    /** totalTokens（总 Token 数）表示输入和输出合计 Token（词元）数量，未知时可以为空。 */
    private Integer totalTokens;

    /** finishReason（结束原因）表示模型停止生成的原因，例如 stop（正常结束）、length（长度限制）或 unknown（未知）。 */
    private String finishReason;

    /** responseLength（响应字符数）表示模型文本输出的字符长度。 */
    private Integer responseLength;

    /** truncatedSuspected（是否疑似截断）表示本地规则是否认为输出可能未完整返回。 */
    private Boolean truncatedSuspected;

    /** rawErrorMessage（原始错误消息）表示供应商或输出治理过程中附带的错误说明。 */
    private String rawErrorMessage;
}
