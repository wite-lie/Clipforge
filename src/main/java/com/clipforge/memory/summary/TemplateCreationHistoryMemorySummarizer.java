package com.clipforge.memory.summary;

import com.clipforge.agent.CreationPlanResult;
import com.clipforge.memory.write.MemoryWriteRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * TemplateCreationHistoryMemorySummarizer（模板创作历史记忆摘要器）。
 *
 * <p>第一版使用模板摘要高分方案，避免每次写记忆都额外调用 LLM（大语言模型）。
 */
@Component
@Profile("db")
public class TemplateCreationHistoryMemorySummarizer implements CreationHistoryMemorySummarizer {

    /**
     * summarize（摘要）用于把最终创作结果压缩成可复用经验。
     *
     * @param request 记忆写入请求
     * @return 创作历史记忆摘要
     */
    @Override
    public CreationHistoryMemorySummary summarize(MemoryWriteRequest request) {
        // 读取最终创作方案结果。
        CreationPlanResult result = request == null ? null : request.getFinalResult();
        // 如果没有最终结果，则返回空摘要。
        if (result == null) {
            // 返回空摘要对象。
            return CreationHistoryMemorySummary.builder().summary("").tags(List.of()).build();
        }
        // 组装高分方案摘要。
        String summary = "本次高分方案围绕“" + safe(result.getTheme()) + "”展开，适配 " + safe(request.getPlatform())
                + " 平台，使用“" + safe(result.getHook()) + "”作为开头钩子，目标受众为“"
                + safe(result.getTargetAudience()) + "”，综合评分 " + result.getScore() + "。";
        // 返回摘要和标签。
        return CreationHistoryMemorySummary.builder()
                .summary(summary)
                .tags(buildTags(request, result))
                .build();
    }

    /**
     * buildTags（构建标签）用于从平台、主题和话题中生成可检索标签。
     *
     * @param request 记忆写入请求
     * @param result 创作方案结果
     * @return 标签列表
     */
    private List<String> buildTags(MemoryWriteRequest request, CreationPlanResult result) {
        // 创建标签列表。
        List<String> tags = new ArrayList<>();
        // 添加目标平台标签。
        addIfPresent(tags, request == null ? null : request.getPlatform());
        // 添加高分方案标签。
        tags.add("高分方案");
        // 添加主题标签。
        addIfPresent(tags, result.getTheme());
        // 添加第一个话题标签。
        if (result.getHashtags() != null && !result.getHashtags().isEmpty()) {
            // 去掉话题开头的井号，便于后续统一展示。
            addIfPresent(tags, result.getHashtags().get(0).replace("#", ""));
        }
        // 返回标签列表。
        return tags;
    }

    /**
     * addIfPresent（按需添加）用于添加非空标签。
     *
     * @param tags 标签列表
     * @param value 候选标签
     */
    private void addIfPresent(List<String> tags, String value) {
        // 如果候选标签为空，则跳过。
        if (value == null || value.isBlank()) {
            // 直接返回。
            return;
        }
        // 添加裁剪后的标签。
        tags.add(value.trim().length() > 40 ? value.trim().substring(0, 40) : value.trim());
    }

    /**
     * safe（安全文本）用于避免摘要中出现 null。
     *
     * @param value 原始文本
     * @return 安全文本
     */
    private String safe(String value) {
        // 空文本使用“未提供”占位。
        return value == null || value.isBlank() ? "未提供" : value.trim();
    }
}
