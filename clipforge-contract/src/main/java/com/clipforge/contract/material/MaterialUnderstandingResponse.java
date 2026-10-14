package com.clipforge.contract.material;

import com.clipforge.ai.api.vision.ImageUnderstandingResult;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MaterialUnderstandingResponse（素材理解结果响应）。
 *
 * <p>该对象用于把 cf_material_understanding（素材理解结果表）中的最新成功结果返回给前端或测试代码。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialUnderstandingResponse {

    /** understandingId（理解结果编号）表示本次素材理解结果的业务编号。 */
    private String understandingId;

    /** materialId（素材编号）表示该理解结果属于哪个素材。 */
    private String materialId;

    /** materialType（素材类型）表示 IMAGE（图片）等素材类型。 */
    private String materialType;

    /** understandingType（理解类型）表示 IMAGE_VISION（图片视觉理解）等理解能力。 */
    private String understandingType;

    /** status（状态）表示 SUCCEEDED（成功）等理解状态。 */
    private String status;

    /** modelProvider（模型供应商）表示生成该结果的模型平台。 */
    private String modelProvider;

    /** modelName（模型名称）表示生成该结果的模型编号。 */
    private String modelName;

    /** promptVersion（提示词版本）表示该结果使用的图片理解提示词版本。 */
    private String promptVersion;

    /** contentHash（内容哈希）表示素材文件内容指纹。 */
    private String contentHash;

    /** contentSummary（内容摘要）表示面向创作规划的单素材摘要。 */
    private String contentSummary;

    /** imageUnderstandingResult（图片理解结果）表示结构化图片理解内容。 */
    private ImageUnderstandingResult imageUnderstandingResult;

    /** inputTokenCount（输入 Token 数）表示本次理解消耗的输入 Token（词元）。 */
    private Integer inputTokenCount;

    /** outputTokenCount（输出 Token 数）表示本次理解消耗的输出 Token（词元）。 */
    private Integer outputTokenCount;

    /** totalTokenCount（总 Token 数）表示本次理解总消耗 Token（词元）。 */
    private Integer totalTokenCount;

    /** startedAt（开始时间）表示理解任务开始时间。 */
    private LocalDateTime startedAt;

    /** endedAt（结束时间）表示理解任务结束时间。 */
    private LocalDateTime endedAt;

    /** createTime（创建时间）表示理解结果入库时间。 */
    private LocalDateTime createTime;
}
