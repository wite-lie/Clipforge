package com.clipforge.material;

import com.clipforge.api.dto.MaterialResponse;
import com.clipforge.model.vision.ImageUnderstandingResult;
import com.clipforge.model.vision.VisionModelResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MaterialUnderstandingResult（素材理解结果）。
 *
 * <p>该对象承载 MaterialUnderstandingAgent（素材理解智能体）第一版的完整输出，包括输入素材统计、图片理解结果、素材摘要、策略说明和警告信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialUnderstandingResult {

    /** runId（运行编号）表示本次素材理解所属的 Agent Run（智能体运行）。 */
    private String runId;

    /** inputMaterialCount（输入素材数量）表示调用方传入了多少个素材。 */
    private Integer inputMaterialCount;

    /** imageMaterialCount（图片素材数量）表示本次真正进入图片理解链路的素材数量。 */
    private Integer imageMaterialCount;

    /** skippedMaterialCount（跳过素材数量）表示因为类型不匹配或数据为空而没有进入图片理解链路的素材数量。 */
    private Integer skippedMaterialCount;

    /** strategy（理解策略）表示本次局部 Agent（智能体）采用的处理方式。 */
    private String strategy;

    /** imageMaterials（图片素材列表）表示过滤后参与图片理解的素材元数据。 */
    private List<MaterialResponse> imageMaterials;

    /** imageResults（图片理解结果列表）表示 VLM（视觉语言模型）或 Mock（模拟）模型返回的结构化理解结果。 */
    private List<ImageUnderstandingResult> imageResults;

    /** materialSummary（素材摘要）表示多张图片合并后的创作可用摘要。 */
    private MaterialSummary materialSummary;

    /** visionModelResponse（视觉模型响应）用于保留供应商、模型、原始响应和 Token（词元）用量等信息。 */
    private VisionModelResponse visionModelResponse;

    /** warnings（警告列表）表示素材为空、非图片素材被跳过等非致命问题。 */
    private List<String> warnings;
}
