package com.clipforge.agent.generator;

import com.clipforge.contract.creation.ArticleCreationPlan;
import com.clipforge.contract.creation.ArticleImageSlot;
import com.clipforge.contract.creation.ArticleSection;
import com.clipforge.contract.creation.ArticleSourceEvidence;
import com.clipforge.creation.article.ArticleCreationPlanGenerator;
import com.clipforge.creation.model.AgentContext;
import com.clipforge.creation.model.AgentPlan;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MockArticleCreationPlanGenerator（模拟图文创作方案生成器）。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.model", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockArticleCreationPlanGenerator implements ArticleCreationPlanGenerator {

    @Override
    public ArticleCreationPlan generate(AgentContext context, AgentPlan plan) {
        String goal = context == null ? "图文创作" : context.getGoal();
        String platform = context == null ? "toutiao" : context.getPlatform();
        return ArticleCreationPlan.builder()
                .theme("围绕“" + goal + "”生成一篇适合 " + platform + " 的图文内容")
                .targetAudience("对该主题感兴趣、希望快速获得清晰信息的读者")
                .titleCandidates(List.of("一篇看懂：" + goal, goal + "，这些重点别错过", "关于" + goal + "的实用梳理"))
                .summary("本文从背景、关键知识点和行动建议三个层面展开，帮助读者快速理解主题。")
                .lead("如果你想快速理解“" + goal + "”，可以先抓住几个最关键的问题。")
                .sections(List.of(
                        ArticleSection.builder()
                                .sectionTitle("为什么这个主题值得关注")
                                .body("先用读者熟悉的问题切入，说明主题的现实意义和内容价值。")
                                .keyPoints(List.of("建立兴趣", "说明价值", "避免夸张承诺"))
                                .suggestedMaterialIds(List.of())
                                .imageSlots(List.of(imageSlot("slot_1", "为什么这个主题值得关注", "导语后", "USER_UPLOAD", List.of(), "用一张能概括主题的真实素材作为开篇视觉。")))
                                .sourceEvidence(List.of(ArticleSourceEvidence.builder().sourceType("CREATIVE_KNOWLEDGE").quote("平台内容应清晰、真实、有信息密度。").build()))
                                .build(),
                        ArticleSection.builder()
                                .sectionTitle("核心知识点拆解")
                                .body("把复杂信息拆成 3 到 4 个小点，每个小点都用短句解释，降低阅读压力。")
                                .keyPoints(List.of("结构化表达", "段落短", "重点明确"))
                                .suggestedMaterialIds(List.of())
                                .imageSlots(List.of(imageSlot("slot_2", "核心知识点拆解", "段落内", "EXISTING_MATERIAL", List.of(), "配一张能对应核心知识点的已有图片素材。")))
                                .sourceEvidence(List.of())
                                .build(),
                        ArticleSection.builder()
                                .sectionTitle("读者可以带走什么")
                                .body("结尾给出可复述、可分享的总结，让图文内容更像一份有用笔记。")
                                .keyPoints(List.of("总结观点", "引导互动", "保留可信边界"))
                                .suggestedMaterialIds(List.of())
                                .imageSlots(List.of())
                                .sourceEvidence(List.of())
                                .build()))
                .imageSlots(List.of(
                        imageSlot("cover", "全文", "封面", "EXISTING_MATERIAL", List.of(), "选择一张主体清晰、信息辨识度高的图片作为封面。"),
                        imageSlot("slot_1", "为什么这个主题值得关注", "导语后", "USER_UPLOAD", List.of(), "用一张能概括主题的真实素材作为开篇视觉。"),
                        imageSlot("slot_2", "核心知识点拆解", "段落内", "EXISTING_MATERIAL", List.of(), "配一张能对应核心知识点的已有图片素材。")))
                .conclusion("好的图文不是把信息堆满，而是让读者看完能记住重点。")
                .interactionPrompt("你还想了解这个主题的哪一部分？可以在评论区继续补充。")
                .publishCopy("整理了一份关于“" + goal + "”的图文方案，适合收藏慢慢看。")
                .hashtags(List.of("#图文创作", "#知识科普", "#内容策划"))
                .sourceEvidence(List.of())
                .riskWarnings(List.of("当前为模拟生成，真实发布前需要结合知识库证据复核事实。"))
                .score(82)
                .improvementSuggestions(List.of("补充更具体的知识库依据", "增加真实图片或视频素材作为配图参考"))
                .build();
    }

    private ArticleImageSlot imageSlot(String slotId, String sectionTitle, String placement, String sourceType, List<String> materialIds, String reason) {
        return ArticleImageSlot.builder()
                .slotId(slotId)
                .sectionTitle(sectionTitle)
                .placement(placement)
                .sourceType(sourceType)
                .recommendedMaterialIds(materialIds)
                .boundMaterialId(null)
                .imagePrompt(reason)
                .reason(reason)
                .status("SUGGESTED")
                .build();
    }
}
