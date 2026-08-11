package com.clipforge.agent.prompt;

import com.clipforge.memory.model.ConversationSummarySnapshot;
import com.clipforge.memory.model.CreationHistoryMemorySnapshot;
import com.clipforge.memory.model.MemoryContext;
import com.clipforge.memory.model.ProjectMemorySnapshot;
import com.clipforge.memory.model.UserPreferenceMemory;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * MemoryPromptRenderer（记忆提示词渲染器）。
 *
 * <p>该组件负责把 MemoryContext（记忆上下文）渲染为可注入创作 Prompt（提示词）的短文本片段。
 */
@Component
public class MemoryPromptRenderer {

    /** maxItemLength（单条记忆最大长度）用于避免单条记忆过长污染 Prompt（提示词）。 */
    private static final int MAX_ITEM_LENGTH = 300;

    /**
     * render（渲染）用于把记忆上下文转换为提示词片段。
     *
     * @param memoryContext 记忆上下文
     * @return 可注入提示词的记忆片段；无记忆时返回空字符串
     */
    public String render(MemoryContext memoryContext) {
        // 如果没有任何记忆，则返回空字符串，让上层模板不出现空记忆段落。
        if (memoryContext == null || !memoryContext.hasAnyMemory()) {
            // 返回空字符串。
            return "";
        }
        // 创建字符串构建器。
        StringBuilder builder = new StringBuilder();
        // 追加统一标题。
        builder.append("# 用户与项目记忆").append(System.lineSeparator()).append(System.lineSeparator());
        // 追加用户偏好。
        appendUserPreferences(builder, memoryContext.getUserPreferences());
        // 追加项目记忆。
        appendProjectMemory(builder, memoryContext.getProjectMemory());
        // 追加历史经验。
        appendHistoryMemories(builder, memoryContext.getHistoryMemories());
        // 追加会话摘要。
        appendConversationSummary(builder, memoryContext.getConversationSummary());
        // 追加使用约束，防止模型把记忆当成事实知识库。
        builder.append("请把以上记忆作为用户偏好和项目背景使用；如果与用户本轮明确要求冲突，以用户本轮要求为准。");
        // 返回去掉末尾多余空白的文本。
        return builder.toString().stripTrailing();
    }

    /**
     * appendUserPreferences（追加用户偏好）用于渲染用户级长期偏好。
     *
     * @param builder 字符串构建器
     * @param preferences 用户偏好列表
     */
    private void appendUserPreferences(StringBuilder builder, List<UserPreferenceMemory> preferences) {
        // 如果用户偏好为空，则不渲染该小节。
        if (preferences == null || preferences.isEmpty()) {
            // 直接返回。
            return;
        }
        // 追加小节标题。
        builder.append("## 用户长期偏好").append(System.lineSeparator());
        // 遍历用户偏好。
        for (int index = 0; index < preferences.size(); index++) {
            // 读取当前偏好。
            UserPreferenceMemory preference = preferences.get(index);
            // 追加编号、类型、键和值。
            builder.append("- [").append(index + 1).append("] ");
            builder.append(sanitize(preference.getMemoryType())).append(" / ");
            builder.append(sanitize(preference.getMemoryKey())).append("：");
            builder.append(sanitize(preference.getMemoryValue()));
            builder.append(System.lineSeparator());
        }
        // 追加空行。
        builder.append(System.lineSeparator());
    }

    /**
     * appendProjectMemory（追加项目记忆）用于渲染项目级长期画像。
     *
     * @param builder 字符串构建器
     * @param projectMemory 项目记忆
     */
    private void appendProjectMemory(StringBuilder builder, ProjectMemorySnapshot projectMemory) {
        // 如果项目记忆为空，则不渲染该小节。
        if (projectMemory == null) {
            // 直接返回。
            return;
        }
        // 追加小节标题。
        builder.append("## 当前项目记忆").append(System.lineSeparator());
        // 追加项目摘要。
        appendLineIfPresent(builder, "项目定位", projectMemory.getSummary());
        // 追加目标平台。
        appendLineIfPresent(builder, "目标平台", projectMemory.getTargetPlatform());
        // 追加目标受众。
        appendLineIfPresent(builder, "目标受众", projectMemory.getTargetAudience());
        // 追加内容风格。
        appendLineIfPresent(builder, "内容风格", projectMemory.getContentStyle());
        // 追加约束。
        appendLineIfPresent(builder, "项目约束", projectMemory.getConstraintsJson());
        // 追加空行。
        builder.append(System.lineSeparator());
    }

    /**
     * appendHistoryMemories（追加历史记忆）用于渲染历史高分创作经验。
     *
     * @param builder 字符串构建器
     * @param histories 历史记忆列表
     */
    private void appendHistoryMemories(StringBuilder builder, List<CreationHistoryMemorySnapshot> histories) {
        // 如果历史记忆为空，则不渲染该小节。
        if (histories == null || histories.isEmpty()) {
            // 直接返回。
            return;
        }
        // 追加小节标题。
        builder.append("## 可参考历史经验").append(System.lineSeparator());
        // 遍历历史记忆。
        for (int index = 0; index < histories.size(); index++) {
            // 读取当前历史记忆。
            CreationHistoryMemorySnapshot history = histories.get(index);
            // 追加编号、主题、评分和摘要。
            builder.append("- [").append(index + 1).append("] ");
            builder.append("主题：").append(sanitize(history.getTheme()));
            if (history.getScore() != null) {
                // 追加评分。
                builder.append("；评分：").append(history.getScore());
            }
            builder.append("；经验：").append(sanitize(history.getSummary()));
            builder.append(System.lineSeparator());
        }
        // 追加空行。
        builder.append(System.lineSeparator());
    }

    /**
     * appendConversationSummary（追加会话摘要）用于渲染短期记忆滚动摘要。
     *
     * @param builder 字符串构建器
     * @param summary 会话摘要
     */
    private void appendConversationSummary(StringBuilder builder, ConversationSummarySnapshot summary) {
        // 如果会话摘要为空，则不渲染该小节。
        if (summary == null || summary.getSummaryContent() == null || summary.getSummaryContent().isBlank()) {
            // 直接返回。
            return;
        }
        // 追加小节标题。
        builder.append("## 当前会话滚动摘要").append(System.lineSeparator());
        // 追加摘要内容。
        builder.append("- ").append(sanitize(summary.getSummaryContent())).append(System.lineSeparator());
        // 追加摘要边界，便于模型理解这是历史压缩结果而不是本轮新要求。
        if (summary.getSummarizedUntilMessageId() != null && !summary.getSummarizedUntilMessageId().isBlank()) {
            // 追加已摘要到的消息编号。
            builder.append("- 已摘要到消息：").append(sanitize(summary.getSummarizedUntilMessageId())).append(System.lineSeparator());
        }
        // 追加空行。
        builder.append(System.lineSeparator());
    }

    /**
     * appendLineIfPresent（按需追加一行）用于渲染非空字段。
     *
     * @param builder 字符串构建器
     * @param label 标签
     * @param value 原始值
     */
    private void appendLineIfPresent(StringBuilder builder, String label, String value) {
        // 如果值为空，则不追加。
        if (value == null || value.isBlank()) {
            // 直接返回。
            return;
        }
        // 追加一行键值文本。
        builder.append("- ").append(label).append("：").append(sanitize(value)).append(System.lineSeparator());
    }

    /**
     * sanitize（清洗文本）用于避免记忆内容破坏 Prompt（提示词）结构。
     *
     * @param text 原始文本
     * @return 清洗后的文本
     */
    private String sanitize(String text) {
        // 空文本统一转换为空字符串。
        String safeText = text == null ? "" : text;
        // 替换 Markdown（标记文档）分隔符，避免破坏提示词结构。
        safeText = safeText.replace("---", "___");
        // 如果文本超过单条长度限制，则截断。
        if (safeText.length() > MAX_ITEM_LENGTH) {
            // 返回截断后的文本。
            return safeText.substring(0, MAX_ITEM_LENGTH) + "...";
        }
        // 返回清洗后的文本。
        return safeText;
    }
}
