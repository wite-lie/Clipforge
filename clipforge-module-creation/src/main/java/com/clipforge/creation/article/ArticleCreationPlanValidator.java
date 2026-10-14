package com.clipforge.creation.article;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.contract.creation.ArticleCreationPlan;
import com.clipforge.contract.creation.ArticleSection;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ArticleCreationPlanValidator（图文方案校验器）。
 */
@Component
public class ArticleCreationPlanValidator {

    /**
     * validate（校验）用于检查图文方案是否满足最低结构要求。
     *
     * @param plan 图文方案
     */
    public void validate(ArticleCreationPlan plan) {
        if (plan == null) {
            throw invalid("图文方案不能为空");
        }
        requireText(plan.getTheme(), "theme 不能为空");
        requireText(plan.getTargetAudience(), "targetAudience 不能为空");
        requireText(plan.getSummary(), "summary 不能为空");
        requireText(plan.getLead(), "lead 不能为空");
        requireText(plan.getConclusion(), "conclusion 不能为空");
        requireList(plan.getTitleCandidates(), "titleCandidates 不能为空");
        requireList(plan.getSections(), "sections 不能为空");
        for (ArticleSection section : plan.getSections()) {
            if (section == null) {
                throw invalid("sections 不能包含空段落");
            }
            requireText(section.getSectionTitle(), "sectionTitle 不能为空");
            requireText(section.getBody(), "section body 不能为空");
        }
        if (plan.getScore() < 0 || plan.getScore() > 100) {
            throw invalid("score 必须在 0 到 100 之间");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw invalid(message);
        }
    }

    private void requireList(List<?> values, String message) {
        if (values == null || values.isEmpty()) {
            throw invalid(message);
        }
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID, message);
    }
}
