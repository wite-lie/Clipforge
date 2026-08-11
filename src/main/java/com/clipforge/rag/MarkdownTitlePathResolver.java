package com.clipforge.rag;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * MarkdownTitlePathResolver（Markdown 标题路径解析器）。
 *
 * <p>该组件为 LangChain4j（LangChain 的 Java 封装）切出来的 TextSegment（文本片段）补充简单标题路径，方便后续 RAG（检索增强生成）结果复盘。
 */
@Component
public class MarkdownTitlePathResolver {

    /**
     * resolveTitlePath（解析标题路径）用于根据切片在原文中的位置寻找最近 Markdown（标记文档）标题层级。
     *
     * @param documentText 原始文档文本
     * @param segmentText 切片文本
     * @param searchFrom 起始搜索位置
     * @return 标题解析结果
     */
    public TitlePathResolution resolveTitlePath(String documentText, String segmentText, int searchFrom) {
        // 在原文中从上次位置之后寻找切片文本位置。
        int segmentStart = documentText.indexOf(segmentText, Math.max(0, searchFrom));
        // 如果未找到，则从全文开头再尝试一次，兼容 LangChain4j 切片做过空白规整的情况。
        if (segmentStart < 0) {
            // 从全文开头查找切片文本。
            segmentStart = documentText.indexOf(segmentText);
        }
        // 如果仍未找到，则使用传入位置作为近似位置。
        int safeSegmentStart = segmentStart < 0 ? Math.max(0, searchFrom) : segmentStart;
        // 截取切片前的文本。
        String beforeSegment = documentText.substring(0, Math.min(safeSegmentStart, documentText.length()));
        // 从切片前文本中解析标题路径。
        String titlePath = parseTitlePath(beforeSegment);
        // 计算下次搜索起点。
        int nextSearchFrom = safeSegmentStart + Math.max(1, segmentText.length());
        // 返回标题路径和下次搜索起点。
        return new TitlePathResolution(titlePath, nextSearchFrom);
    }

    /**
     * parseTitlePath（解析标题路径）用于从 Markdown（标记文档）文本中提取最后生效的标题层级。
     *
     * @param text 切片前文本
     * @return 标题路径
     */
    private String parseTitlePath(String text) {
        // 创建标题层级列表，最多支持 6 级 Markdown（标记文档）标题。
        List<String> headings = new ArrayList<>(List.of("", "", "", "", "", ""));
        // 按行遍历文本。
        for (String line : text.split("\\R")) {
            // 去掉行首尾空白。
            String trimmedLine = line.strip();
            // 如果不是 Markdown（标记文档）标题，则跳过。
            if (!trimmedLine.startsWith("#")) {
                // 继续下一行。
                continue;
            }
            // 计算标题层级。
            int level = headingLevel(trimmedLine);
            // 如果层级不合法，则跳过。
            if (level < 1 || level > 6) {
                // 继续下一行。
                continue;
            }
            // 提取标题文本。
            String headingText = trimmedLine.substring(level).strip();
            // 如果标题文本为空，则跳过。
            if (headingText.isBlank()) {
                // 继续下一行。
                continue;
            }
            // 设置当前层级标题。
            headings.set(level - 1, headingText);
            // 清空更深层级标题，避免旧标题串到新章节下。
            for (int index = level; index < headings.size(); index++) {
                // 清空更深层级。
                headings.set(index, "");
            }
        }
        // 拼接非空标题路径。
        String titlePath = headings.stream()
                // 过滤空标题。
                .filter(value -> value != null && !value.isBlank())
                // 用斜杠连接标题层级。
                .reduce((left, right) -> left + " / " + right)
                // 没有标题时返回空字符串。
                .orElse("");
        // 返回标题路径。
        return titlePath;
    }

    /**
     * headingLevel（标题层级）用于统计 Markdown（标记文档）标题行开头的井号数量。
     *
     * @param line 文本行
     * @return 标题层级
     */
    private int headingLevel(String line) {
        // 初始化层级计数。
        int level = 0;
        // 遍历行字符。
        while (level < line.length() && line.charAt(level) == '#') {
            // 增加标题层级。
            level++;
        }
        // 标题井号后必须是空格，避免把普通井号文本误判为标题。
        if (level >= line.length() || !Character.isWhitespace(line.charAt(level))) {
            // 返回非法层级。
            return -1;
        }
        // 返回标题层级。
        return level;
    }

    /**
     * TitlePathResolution（标题路径解析结果）用于同时返回标题路径和下次搜索位置。
     *
     * @param titlePath 标题路径
     * @param nextSearchFrom 下次搜索起点
     */
    public record TitlePathResolution(String titlePath, int nextSearchFrom) {
    }
}
