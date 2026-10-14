package com.clipforge.prompt.compress;

import org.springframework.stereotype.Component;

/**
 * PromptCompressionReportPrinter（提示词压缩报告打印器）。
 *
 * <p>该组件用于真实模型集成测试时美化输出压缩报告，帮助观察哪些上下文片段被裁剪、裁剪前后大致节省多少 Token（词元）。
 */
@Component
public class PromptCompressionReportPrinter {

    /**
     * print（打印）输出一次 Prompt Compression（提示词压缩）报告。
     *
     * @param title 打印标题
     * @param report 压缩报告
     */
    public void print(String title, PromptCompressionReport report) {
        System.out.println("===== " + title + " =====");
        if (report == null) {
            System.out.println("暂无 Prompt Compression（提示词压缩）报告。");
            return;
        }
        System.out.println("是否压缩：" + report.getCompressed());
        System.out.println("原因：" + report.getReason());
        System.out.println("压缩前估算 Token（词元）：" + report.getBeforeEstimatedTokens());
        System.out.println("压缩后估算 Token（词元）：" + report.getAfterEstimatedTokens());
        if (report.getSections() == null || report.getSections().isEmpty()) {
            System.out.println("片段明细：暂无。");
            return;
        }
        System.out.println("片段明细：");
        for (PromptSectionCompressionResult section : report.getSections()) {
            System.out.println("- " + section.getSectionType()
                    + "，动作：" + section.getAction()
                    + "，字符：" + section.getBeforeCharacters() + " -> " + section.getAfterCharacters()
                    + "，估算 Token（词元）：" + section.getBeforeEstimatedTokens() + " -> " + section.getAfterEstimatedTokens()
                    + "，原因：" + section.getReason());
        }
    }
}
