package com.clipforge.agent.material;

import com.clipforge.api.dto.MaterialResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.material.MaterialSummary;
import com.clipforge.material.MaterialUnderstandingResult;
import com.clipforge.material.understanding.SavedImageUnderstandingReader;
import com.clipforge.model.vision.ImageUnderstandingResult;
import com.clipforge.tool.MaterialSummaryTool;
import com.clipforge.trace.TraceRecorder;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DefaultMaterialUnderstandingAgent（默认素材理解智能体）。
 *
 * <p>该实现用于创作 Workflow（工作流）中的素材理解节点。Phase 10（阶段 10）优化后，它只读取已经保存的图片理解结果，不在创作期同步调用 VLM（视觉语言模型）。
 */
@Component
@RequiredArgsConstructor
public class DefaultMaterialUnderstandingAgent implements MaterialUnderstandingAgent {

    /** IMAGE_MATERIAL_TYPE（图片素材类型）用于筛选当前阶段支持注入创作链路的图片素材。 */
    private static final String IMAGE_MATERIAL_TYPE = "IMAGE";

    /** savedImageUnderstandingReader（已保存图片理解结果读取器）用于读取处理期落库的图片理解结果。 */
    private final SavedImageUnderstandingReader savedImageUnderstandingReader;

    /** materialSummaryTool（素材摘要工具）负责把多张图片理解结果合并为素材摘要。 */
    private final MaterialSummaryTool materialSummaryTool;

    /** traceRecorder（执行链路记录器）用于记录局部 Agent（智能体）级别的执行事件。 */
    private final TraceRecorder traceRecorder;

    /**
     * understand（理解素材）用于读取已保存的图片理解结果，并合并生成素材摘要。
     *
     * @param runId 运行编号
     * @param materials 素材列表
     * @param instruction 理解指令，当前只作为链路语义保留，不再触发视觉模型调用
     * @return 素材理解结果
     */
    @Override
    public MaterialUnderstandingResult understand(String runId, List<MaterialResponse> materials, String instruction) {
        // 统计输入素材数量。
        int inputMaterialCount = materials == null ? 0 : materials.size();
        // 记录素材理解智能体开始事件。
        traceRecorder.record(runId, "MATERIAL_UNDERSTANDING_AGENT_STARTED", "素材理解智能体开始执行", "输入素材数量：" + inputMaterialCount);
        // 创建警告列表，用于记录非致命问题。
        List<String> warnings = new ArrayList<>();
        // 过滤当前链路需要的图片素材。
        List<MaterialResponse> imageMaterials = filterImageMaterials(materials, warnings);
        // 计算跳过素材数量。
        int skippedMaterialCount = inputMaterialCount - imageMaterials.size();
        // 生成策略说明。
        String strategy = decideStrategy(imageMaterials, skippedMaterialCount);
        // 记录策略决策事件。
        traceRecorder.record(runId, "MATERIAL_UNDERSTANDING_AGENT_STRATEGY_DECIDED", "素材理解策略已确定", strategy);
        // 没有图片素材时返回空素材摘要，保持后续链路稳定。
        if (imageMaterials.isEmpty()) {
            MaterialSummary emptySummary = materialSummaryTool.summarize(runId, List.of());
            MaterialUnderstandingResult emptyResult = buildResult(runId, inputMaterialCount, imageMaterials, skippedMaterialCount, strategy, List.of(), emptySummary, warnings);
            traceRecorder.record(runId, "MATERIAL_UNDERSTANDING_AGENT_FINISHED", "素材理解智能体执行完成", "图片素材数量：0，读取结果数量：0");
            return emptyResult;
        }
        // 从数据库读取每张图片已经完成的理解结果。
        List<ImageUnderstandingResult> imageResults = loadSavedImageResults(imageMaterials);
        // 把多张图片理解结果合并为创作规划可使用的素材摘要。
        MaterialSummary materialSummary = materialSummaryTool.summarize(runId, imageResults);
        // 构建素材理解结果。
        MaterialUnderstandingResult result = buildResult(runId, inputMaterialCount, imageMaterials, skippedMaterialCount, strategy, imageResults, materialSummary, warnings);
        // 记录素材理解完成事件。
        traceRecorder.record(runId, "MATERIAL_UNDERSTANDING_AGENT_FINISHED", "素材理解智能体执行完成", "图片素材数量：" + imageMaterials.size() + "，读取结果数量：" + imageResults.size());
        // 返回完整素材理解结果。
        return result;
    }

    /**
     * loadSavedImageResults（读取已保存图片结果）用于保证每张图片都已经在处理期完成理解。
     *
     * @param imageMaterials 图片素材列表
     * @return 图片理解结果列表
     */
    private List<ImageUnderstandingResult> loadSavedImageResults(List<MaterialResponse> imageMaterials) {
        // 创建图片理解结果列表。
        List<ImageUnderstandingResult> results = new ArrayList<>();
        // 遍历图片素材。
        for (MaterialResponse material : imageMaterials) {
            // 读取最新成功的图片理解结果。
            ImageUnderstandingResult result = savedImageUnderstandingReader.findLatestSucceeded(material.getMaterialId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MATERIAL_NOT_READY, "图片素材尚未完成理解，请等待素材处理任务成功后再创作，materialId=" + material.getMaterialId()));
            // 加入结果列表。
            results.add(result);
        }
        // 返回全部图片理解结果。
        return results;
    }

    /**
     * filterImageMaterials（过滤图片素材）用于保留当前阶段能够注入创作链路的图片素材。
     *
     * @param materials 原始素材列表
     * @param warnings 警告列表
     * @return 图片素材列表
     */
    private List<MaterialResponse> filterImageMaterials(List<MaterialResponse> materials, List<String> warnings) {
        // 空素材列表直接返回空列表。
        if (materials == null || materials.isEmpty()) {
            warnings.add("调用方没有传入素材，素材理解链路将生成空摘要。");
            return List.of();
        }
        // 创建图片素材列表。
        List<MaterialResponse> imageMaterials = new ArrayList<>();
        // 遍历原始素材。
        for (MaterialResponse material : materials) {
            // 跳过空素材对象。
            if (material == null) {
                warnings.add("存在空素材对象，已跳过。");
                continue;
            }
            // 跳过非图片素材，视频摘要由视频理解链路单独注入。
            if (!IMAGE_MATERIAL_TYPE.equalsIgnoreCase(material.getMaterialType())) {
                warnings.add("素材 " + material.getMaterialId() + " 的类型不是 IMAGE（图片），当前图片素材节点已跳过。");
                continue;
            }
            // 素材编号为空时无法读取理解结果。
            if (material.getMaterialId() == null || material.getMaterialId().isBlank()) {
                warnings.add("存在缺少 materialId（素材编号）的图片素材，已跳过。");
                continue;
            }
            // 加入图片素材列表。
            imageMaterials.add(material);
        }
        // 返回图片素材列表。
        return imageMaterials;
    }

    /**
     * decideStrategy（决策策略）用于描述本次素材理解采用的处理方式。
     *
     * @param imageMaterials 图片素材列表
     * @param skippedMaterialCount 跳过素材数量
     * @return 策略说明
     */
    private String decideStrategy(List<MaterialResponse> imageMaterials, int skippedMaterialCount) {
        // 没有图片素材时进入空摘要策略。
        if (imageMaterials == null || imageMaterials.isEmpty()) {
            return "当前没有可读取的 IMAGE（图片）素材，采用空素材摘要策略。";
        }
        // 返回读取已保存结果的策略说明。
        return "读取 " + imageMaterials.size() + " 张 IMAGE（图片）素材的已保存 IMAGE_VISION（图片视觉理解）结果，并跳过 " + skippedMaterialCount + " 个不适合当前节点处理的素材。";
    }

    /**
     * buildResult（构建结果）用于统一组装素材理解结果对象。
     *
     * @param runId 运行编号
     * @param inputMaterialCount 输入素材数量
     * @param imageMaterials 图片素材列表
     * @param skippedMaterialCount 跳过素材数量
     * @param strategy 策略说明
     * @param imageResults 图片理解结果列表
     * @param materialSummary 素材摘要
     * @param warnings 警告列表
     * @return 素材理解结果
     */
    private MaterialUnderstandingResult buildResult(String runId, int inputMaterialCount, List<MaterialResponse> imageMaterials, int skippedMaterialCount, String strategy, List<ImageUnderstandingResult> imageResults, MaterialSummary materialSummary, List<String> warnings) {
        // 构建素材理解结果。
        return MaterialUnderstandingResult.builder()
                .runId(runId)
                .inputMaterialCount(inputMaterialCount)
                .imageMaterialCount(imageMaterials == null ? 0 : imageMaterials.size())
                .skippedMaterialCount(skippedMaterialCount)
                .strategy(strategy)
                .imageMaterials(imageMaterials == null ? List.of() : imageMaterials)
                .imageResults(imageResults == null ? List.of() : imageResults)
                .materialSummary(materialSummary)
                .visionModelResponse(null)
                .warnings(warnings == null ? List.of() : List.copyOf(warnings))
                .build();
    }
}
