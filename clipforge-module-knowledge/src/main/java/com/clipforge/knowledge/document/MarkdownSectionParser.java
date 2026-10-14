package com.clipforge.knowledge.document;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * MarkdownSectionParser（Markdown 章节解析器）。
 *
 * <p>该组件负责把 Markdown（标记文档）先按标题结构拆成章节。章节内部仍交给 LangChain4j（LangChain 的 Java 封装）DocumentSplitter（文档切分器）处理。
 */
@Component
public class MarkdownSectionParser {

    /**
     * parse（解析）用于把 Markdown（标记文档）文本拆成带标题路径的章节列表。
     *
     * @param markdown Markdown（标记文档）原文
     * @return Markdown 章节列表
     */
    public List<MarkdownSection> parse(String markdown) {
        // 如果原文为空，则返回空列表。
        if (markdown == null || markdown.isBlank()) {
            // 返回空列表。
            return List.of();
        }
        // 创建解析结果列表。
        List<MarkdownSection> sections = new ArrayList<>();
        // 创建最多 6 级 Markdown（标记文档）标题栈。
        List<String> headingStack = new ArrayList<>(List.of("", "", "", "", "", ""));
        // 创建当前章节内容缓冲区。
        StringBuilder currentContent = new StringBuilder();
        // 当前标题路径。
        String currentTitlePath = "";
        // 按行遍历，保留跨平台换行兼容。
        for (String line : markdown.split("\\R", -1)) {
            // 计算当前行标题层级。
            int headingLevel = headingLevel(line);
            // 如果当前行是合法 Markdown（标记文档）标题，则开启新章节。
            if (headingLevel > 0) {
                // 把前一个章节写入结果。
                flushSection(sections, currentTitlePath, currentContent);
                // 提取标题文本。
                String headingText = line.strip().substring(headingLevel).strip();
                // 更新标题栈。
                updateHeadingStack(headingStack, headingLevel, headingText);
                // 更新当前标题路径。
                currentTitlePath = joinTitlePath(headingStack);
                // 清空章节内容缓冲区。
                currentContent.setLength(0);
                // 把标题行也放入章节内容，保证检索命中时保留局部上下文。
                currentContent.append(line).append('\n');
                // 继续下一行。
                continue;
            }
            // 普通内容行直接加入当前章节。
            currentContent.append(line).append('\n');
        }
        // 循环结束后写入最后一个章节。
        flushSection(sections, currentTitlePath, currentContent);
        // 返回章节列表。
        return sections;
    }

    /**
     * flushSection（写入章节）用于把当前缓冲区中的章节内容转为 MarkdownSection（Markdown 章节）。
     *
     * @param sections 章节列表
     * @param titlePath 标题路径
     * @param contentBuilder 内容缓冲区
     */
    private void flushSection(List<MarkdownSection> sections, String titlePath, StringBuilder contentBuilder) {
        // 读取章节内容并去掉首尾空白。
        String content = contentBuilder.toString().strip();
        // 如果章节内容为空，则不写入。
        if (content.isBlank()) {
            // 清空缓冲区。
            contentBuilder.setLength(0);
            // 返回。
            return;
        }
        // 如果章节只有标题没有正文，则跳过，避免生成噪声切片。
        if (!hasNonHeadingText(content)) {
            // 清空缓冲区。
            contentBuilder.setLength(0);
            // 返回。
            return;
        }
        // 写入章节对象。
        sections.add(MarkdownSection.builder()
                // 设置章节序号。
                .sectionIndex(sections.size())
                // 设置标题路径，空白路径转为空字符串。
                .titlePath(titlePath == null ? "" : titlePath)
                // 设置章节内容。
                .content(content)
                // 构建对象。
                .build());
        // 清空缓冲区。
        contentBuilder.setLength(0);
    }

    /**
     * hasNonHeadingText（是否有非标题文本）用于过滤只有标题行的章节。
     *
     * @param content 章节内容
     * @return 是否包含正文
     */
    private boolean hasNonHeadingText(String content) {
        // 按行遍历章节内容。
        for (String line : content.split("\\R")) {
            // 去掉首尾空白。
            String trimmedLine = line.strip();
            // 空行不算正文。
            if (trimmedLine.isBlank()) {
                // 继续下一行。
                continue;
            }
            // 非标题行视为正文。
            if (headingLevel(trimmedLine) <= 0) {
                // 返回包含正文。
                return true;
            }
        }
        // 没有正文。
        return false;
    }

    /**
     * updateHeadingStack（更新标题栈）用于记录当前 Markdown（标记文档）标题层级。
     *
     * @param headingStack 标题栈
     * @param headingLevel 标题层级
     * @param headingText 标题文本
     */
    private void updateHeadingStack(List<String> headingStack, int headingLevel, String headingText) {
        // 设置当前层级标题。
        headingStack.set(headingLevel - 1, headingText);
        // 清空更深层级标题，避免旧小标题串到新章节下。
        for (int index = headingLevel; index < headingStack.size(); index++) {
            // 清空更深层级。
            headingStack.set(index, "");
        }
    }

    /**
     * joinTitlePath（拼接标题路径）用于把标题栈转为可读路径。
     *
     * @param headingStack 标题栈
     * @return 标题路径
     */
    private String joinTitlePath(List<String> headingStack) {
        // 拼接非空标题。
        return headingStack.stream()
                // 过滤空标题。
                .filter(value -> value != null && !value.isBlank())
                // 用斜杠拼接。
                .reduce((left, right) -> left + " / " + right)
                // 没有标题时返回空字符串。
                .orElse("");
    }

    /**
     * headingLevel（标题层级）用于识别 Markdown（标记文档）标题行。
     *
     * @param line 文本行
     * @return 标题层级，不是标题时返回 -1
     */
    private int headingLevel(String line) {
        // 如果行为空，则不是标题。
        if (line == null || line.isBlank()) {
            // 返回不是标题。
            return -1;
        }
        // 去掉首尾空白。
        String trimmedLine = line.strip();
        // 标题必须以井号开头。
        if (!trimmedLine.startsWith("#")) {
            // 返回不是标题。
            return -1;
        }
        // 统计井号数量。
        int level = 0;
        // 遍历井号。
        while (level < trimmedLine.length() && trimmedLine.charAt(level) == '#') {
            // 增加标题层级。
            level++;
        }
        // 只支持 1 到 6 级标题。
        if (level < 1 || level > 6) {
            // 返回不是标题。
            return -1;
        }
        // 井号后必须有空白，避免误判普通井号文本。
        if (level >= trimmedLine.length() || !Character.isWhitespace(trimmedLine.charAt(level))) {
            // 返回不是标题。
            return -1;
        }
        // 返回标题层级。
        return level;
    }
}
