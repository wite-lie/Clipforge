package com.clipforge.tool;

import com.clipforge.material.MaterialSummary;
import com.clipforge.model.vision.ImageUnderstandingResult;
import com.clipforge.trace.TraceRecorder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * MaterialSummaryTool（素材摘要工具）。
 *
 * <p>该工具负责把多张 ImageUnderstandingResult（图片理解结果）合并为 MaterialSummary（素材摘要），让后续创作方案生成不必直接处理一堆零散图片结果。
 */
@Component
@RequiredArgsConstructor
public class MaterialSummaryTool implements Tool {

    /** traceRecorder（执行链路记录器）用于记录素材摘要工具调用过程。 */
    private final TraceRecorder traceRecorder;

    /**
     * name（工具名称）用于标识素材摘要工具。
     *
     * @return 工具名称
     */
    @Override
    public String name() {
        // 返回工具名称。
        return "material_summary";
    }

    /**
     * description（工具描述）用于说明该工具负责合并图片理解结果。
     *
     * @return 工具描述
     */
    @Override
    public String description() {
        // 返回工具能力描述。
        return "把多张图片理解结果合并为适合创作方案生成、评估和修正使用的素材摘要";
    }

    /**
     * summarize（生成摘要）用于合并多张图片理解结果。
     *
     * @param runId 运行编号
     * @param imageResults 图片理解结果列表
     * @return 素材摘要
     */
    public MaterialSummary summarize(String runId, List<ImageUnderstandingResult> imageResults) {
        // 计算输入图片理解结果数量。
        int resultCount = imageResults == null ? 0 : imageResults.size();
        // 记录素材摘要开始事件。
        traceRecorder.record(runId, "MATERIAL_SUMMARY_TOOL_STARTED", "素材摘要工具开始执行", "图片理解结果数量：" + resultCount);
        // 过滤空结果，避免后续处理空指针。
        List<ImageUnderstandingResult> safeResults = safeResults(imageResults);
        // 构建素材摘要。
        MaterialSummary summary = MaterialSummary.builder()
                // 设置素材数量。
                .materialCount(safeResults.size())
                // 设置素材编号列表。
                .materialIds(distinctTexts(safeResults.stream().map(ImageUnderstandingResult::getMaterialId).toList()))
                // 设置主体列表。
                .mainSubjects(distinctTexts(safeResults.stream().map(ImageUnderstandingResult::getMainSubject).toList()))
                // 设置场景列表。
                .scenes(distinctTexts(safeResults.stream().map(ImageUnderstandingResult::getScene).toList()))
                // 设置视觉风格列表。
                .visualStyles(distinctTexts(safeResults.stream().map(ImageUnderstandingResult::getVisualStyle).toList()))
                // 设置关键元素列表。
                .keyElements(distinctNestedTexts(safeResults.stream().map(ImageUnderstandingResult::getKeyElements).toList()))
                // 设置文字元素列表。
                .textElements(distinctNestedTexts(safeResults.stream().map(ImageUnderstandingResult::getTextElements).toList()))
                // 设置创作用途列表。
                .creativeUsages(distinctTexts(safeResults.stream().map(ImageUnderstandingResult::getCreativeUsage).toList()))
                // 设置风险提示列表。
                .riskNotes(distinctNestedTexts(safeResults.stream().map(ImageUnderstandingResult::getRiskNotes).toList()))
                // 设置可直接注入 Prompt（提示词）的摘要文本。
                .summaryText(buildSummaryText(safeResults))
                // 构建素材摘要。
                .build();
        // 记录素材摘要完成事件。
        traceRecorder.record(runId, "MATERIAL_SUMMARY_TOOL_FINISHED", "素材摘要工具执行完成", "素材数量：" + summary.getMaterialCount() + "，主体数：" + summary.getMainSubjects().size() + "，关键元素数：" + summary.getKeyElements().size());
        // 返回素材摘要。
        return summary;
    }

    /**
     * safeResults（安全结果列表）用于过滤空引用。
     *
     * @param imageResults 原始图片理解结果列表
     * @return 非空图片理解结果列表
     */
    private List<ImageUnderstandingResult> safeResults(List<ImageUnderstandingResult> imageResults) {
        // 如果原始列表为空，则返回空列表。
        if (imageResults == null || imageResults.isEmpty()) {
            // 返回空列表。
            return List.of();
        }
        // 过滤列表中的空对象。
        return imageResults.stream()
                // 只保留非空图片理解结果。
                .filter(result -> result != null)
                // 收集为列表。
                .toList();
    }

    /**
     * distinctTexts（文本去重）用于按原始顺序去除空白和重复文本。
     *
     * @param values 原始文本列表
     * @return 去重后的文本列表
     */
    private List<String> distinctTexts(List<String> values) {
        // 创建有序集合，用于保留首次出现顺序。
        Set<String> orderedTexts = new LinkedHashSet<>();
        // 如果输入为空，直接返回空列表。
        if (values == null) {
            // 返回空列表。
            return List.of();
        }
        // 遍历原始文本。
        for (String value : values) {
            // 如果文本有实际内容，则加入有序集合。
            if (value != null && !value.isBlank()) {
                // 加入去除首尾空白后的文本。
                orderedTexts.add(value.strip());
            }
        }
        // 返回去重后的列表。
        return new ArrayList<>(orderedTexts);
    }

    /**
     * distinctNestedTexts（嵌套文本去重）用于合并多个字符串列表并去重。
     *
     * @param nestedValues 多个文本列表
     * @return 去重后的文本列表
     */
    private List<String> distinctNestedTexts(List<List<String>> nestedValues) {
        // 创建扁平化文本列表。
        List<String> flattenedValues = new ArrayList<>();
        // 如果输入为空，直接返回空列表。
        if (nestedValues == null) {
            // 返回空列表。
            return List.of();
        }
        // 遍历嵌套列表。
        for (List<String> values : nestedValues) {
            // 如果某个子列表为空，则跳过。
            if (values == null) {
                // 继续处理下一个子列表。
                continue;
            }
            // 添加子列表全部文本。
            flattenedValues.addAll(values);
        }
        // 对扁平化文本去重。
        return distinctTexts(flattenedValues);
    }

    /**
     * buildSummaryText（构建摘要文本）用于生成可直接放入创作 Prompt（提示词）的素材摘要。
     *
     * @param results 图片理解结果列表
     * @return 素材摘要文本
     */
    private String buildSummaryText(List<ImageUnderstandingResult> results) {
        // 如果没有图片理解结果，则返回明确说明。
        if (results == null || results.isEmpty()) {
            // 返回无素材摘要。
            return "当前没有可用的图片理解结果。";
        }
        // 创建摘要文本构建器。
        StringBuilder builder = new StringBuilder();
        // 添加总述。
        builder.append("本次共有 ").append(results.size()).append(" 张图片素材可用于创作规划。").append("\n");
        // 遍历每张图片理解结果。
        for (int index = 0; index < results.size(); index++) {
            // 读取当前图片理解结果。
            ImageUnderstandingResult result = results.get(index);
            // 添加图片序号和素材编号。
            builder.append(index + 1).append(". materialId（素材编号）：").append(defaultText(result.getMaterialId(), "未知")).append("\n");
            // 添加图片主体。
            builder.append("   主体：").append(defaultText(result.getMainSubject(), "未知")).append("\n");
            // 添加图片场景。
            builder.append("   场景：").append(defaultText(result.getScene(), "未知")).append("\n");
            // 添加视觉风格。
            builder.append("   视觉风格：").append(defaultText(result.getVisualStyle(), "未知")).append("\n");
            // 添加关键元素。
            builder.append("   关键元素：").append(joinTexts(result.getKeyElements())).append("\n");
            // 添加创作用途。
            builder.append("   创作用途：").append(defaultText(result.getCreativeUsage(), "未说明")).append("\n");
            // 添加单图摘要。
            builder.append("   单图摘要：").append(defaultText(result.getSummary(), "未生成摘要")).append("\n");
        }
        // 添加整体使用建议。
        builder.append("整体使用建议：优先围绕主体、场景和关键元素设计开头钩子、分镜画面和旁白解释，避免生成和图片无关的内容。");
        // 返回摘要文本。
        return builder.toString();
    }

    /**
     * joinTexts（拼接文本）用于把列表转换为适合展示的短文本。
     *
     * @param values 文本列表
     * @return 拼接后的文本
     */
    private String joinTexts(List<String> values) {
        // 对文本列表去重。
        List<String> texts = distinctTexts(values);
        // 如果去重后为空，则返回无。
        if (texts.isEmpty()) {
            // 返回无。
            return "无";
        }
        // 使用中文顿号拼接。
        return String.join("、", texts);
    }

    /**
     * defaultText（默认文本）用于处理空白文本。
     *
     * @param value 原始文本
     * @param defaultValue 默认文本
     * @return 最终文本
     */
    private String defaultText(String value, String defaultValue) {
        // 如果原始文本为空白，则返回默认文本。
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
