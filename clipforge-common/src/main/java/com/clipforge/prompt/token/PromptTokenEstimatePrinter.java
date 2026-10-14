package com.clipforge.prompt.token;

import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * PromptTokenEstimatePrinter（提示词词元估算打印器）。
 *
 * <p>该组件用于在本地调试和真实模型测试中，把 PromptTokenEstimateResult（提示词词元估算结果）打印成易读文本。
 */
@Component
public class PromptTokenEstimatePrinter {

    /**
     * print（打印）用于把估算结果输出到控制台。
     *
     * @param title 标题
     * @param result 估算结果
     */
    public void print(String title, PromptTokenEstimateResult result) {
        // 打印标题。
        System.out.println("===== " + title + " =====");
        // 如果结果为空，则打印空结果。
        if (result == null) {
            // 打印空提示。
            System.out.println("估算结果为空");
            // 直接返回。
            return;
        }
        // 打印总估算 Token（词元）。
        System.out.println("总估算 Token（词元）：" + result.getTotalEstimatedTokens());
        // 打印总字符数。
        System.out.println("总字符数：" + result.getTotalCharacters());
        // 打印风险等级。
        System.out.println("风险等级：" + result.getRiskLevel());
        // 打印阈值。
        System.out.println("阈值：MEDIUM >= " + result.getWarningThreshold() + "，HIGH >= " + result.getHighRiskThreshold());
        // 打印最大片段。
        System.out.println("最大真实片段：" + result.getLargestSectionType() + " / " + result.getLargestSectionName());
        // 打印建议裁剪对象。
        System.out.println("建议后续优先裁剪：" + result.getRecommendedTrimSectionTypes());
        // 打印明细标题。
        System.out.println("----- 分段估算明细 -----");
        // 遍历片段明细。
        for (PromptTokenSectionEstimate section : result.getSections()) {
            // 打印单个片段。
            System.out.println(formatSection(section));
        }
    }

    /**
     * formatSection（格式化片段）用于输出单行片段估算结果。
     *
     * @param section 片段估算
     * @return 格式化文本
     */
    private String formatSection(PromptTokenSectionEstimate section) {
        // 创建百分比格式化器。
        NumberFormat percentFormat = NumberFormat.getPercentInstance(Locale.CHINA);
        // 设置最多两位小数。
        percentFormat.setMaximumFractionDigits(2);
        // 格式化占比。
        String ratioText = section.getRatio() == null ? "0%" : percentFormat.format(section.getRatio());
        // 返回单行文本。
        return section.getType()
                // 拼接名称。
                + " | " + section.getName()
                // 拼接是否计入总数。
                + " | 计入总数：" + section.getIncludedInTotal()
                // 拼接字符数。
                + " | 字符数：" + section.getCharacterCount()
                // 拼接 Token（词元）数。
                + " | 估算 Token：" + section.getEstimatedTokens()
                // 拼接占比。
                + " | 占比：" + ratioText;
    }
}
