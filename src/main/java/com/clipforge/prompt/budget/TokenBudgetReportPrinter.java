package com.clipforge.prompt.budget;

import org.springframework.stereotype.Component;

/**
 * TokenBudgetReportPrinter（词元预算报告打印器）。
 *
 * <p>该组件用于在本地真实模型测试中美化打印 TokenBudgetDecision（词元预算决策）。
 */
@Component
public class TokenBudgetReportPrinter {

    /**
     * print（打印）用于输出预算决策报告。
     *
     * @param title 标题
     * @param decision 预算决策
     */
    public void print(String title, TokenBudgetDecision decision) {
        // 打印标题。
        System.out.println("===== " + title + " =====");
        // 如果决策为空，则打印空提示。
        if (decision == null) {
            // 打印空结果。
            System.out.println("预算决策为空");
            // 直接返回。
            return;
        }
        // 打印调用用途。
        System.out.println("调用用途：" + decision.getPurpose());
        // 打印预算档案。
        System.out.println("预算档案：" + decision.getProfileName());
        // 打印上下文窗口。
        System.out.println("上下文窗口：" + decision.getModelContextWindow());
        // 打印输出预留。
        System.out.println("输出预留：" + decision.getReservedOutputTokens());
        // 打印安全余量。
        System.out.println("安全余量：" + decision.getReservedSafetyTokens());
        // 打印输入预算。
        System.out.println("输入预算：" + decision.getInputBudgetTokens());
        // 打印估算输入。
        System.out.println("估算输入：" + decision.getEstimatedInputTokens());
        // 打印是否超预算。
        System.out.println("是否超预算：" + decision.getOverBudget());
        // 打印超预算数量。
        System.out.println("超预算 Token（词元）：" + decision.getOverBudgetTokens());
        // 打印风险等级。
        System.out.println("风险等级：" + decision.getRiskLevel());
        // 打印明细标题。
        System.out.println("----- 片段预算明细 -----");
        // 遍历片段预算。
        if (decision.getSectionBudgets() != null) {
            // 打印每个片段。
            decision.getSectionBudgets().forEach(section -> System.out.println(formatSection(section)));
        }
        // 打印建议标题。
        System.out.println("----- 建议动作 -----");
        // 遍历建议动作。
        if (decision.getRecommendedActions() != null) {
            // 打印每条建议。
            decision.getRecommendedActions().forEach(action -> System.out.println("- " + action));
        }
    }

    /**
     * formatSection（格式化片段）用于输出单行片段预算明细。
     *
     * @param section 片段预算
     * @return 单行文本
     */
    private String formatSection(TokenBudgetSectionBudget section) {
        // 返回格式化文本。
        return section.getSectionType()
                // 拼接名称。
                + " | " + section.getSectionName()
                // 拼接估算 Token（词元）。
                + " | 估算：" + section.getEstimatedTokens()
                // 拼接限制。
                + " | 限制：" + (section.getLimitTokens() == null ? "未配置" : section.getLimitTokens())
                // 拼接状态。
                + " | 状态：" + section.getStatus()
                // 拼接超出数量。
                + " | 超出：" + section.getOverLimitTokens()
                // 拼接是否计入总数。
                + " | 计入总数：" + section.getIncludedInTotal();
    }
}
