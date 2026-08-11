package com.clipforge.model.vision;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VisionModelRequest（视觉模型请求）。
 *
 * <p>该对象封装一次 VLM（视觉语言模型）图片理解调用需要的文本提示、图片素材和业务追踪字段。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisionModelRequest {

    /** runId（运行编号）用于把图片理解调用和 Agent Run（智能体运行）关联起来。 */
    private String runId;

    /** purpose（调用用途）用于 Trace（执行链路追踪）和模型调用记录区分图片理解、图片摘要等场景。 */
    private String purpose;

    /** instruction（理解指令）表示希望视觉模型重点观察什么内容。 */
    private String instruction;

    /** materials（图片素材列表）表示本次要理解的一张或多张图片。 */
    private List<VisionModelRequestMaterial> materials;

    /** temperature（温度）用于控制模型输出随机性，图片理解第一版建议偏低。 */
    private Double temperature;

    /** maxTokens（最大输出 Token 数）用于限制模型生成长度。 */
    private Integer maxTokens;
}
