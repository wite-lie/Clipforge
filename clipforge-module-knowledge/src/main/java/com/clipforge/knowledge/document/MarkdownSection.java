package com.clipforge.knowledge.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MarkdownSection（Markdown 章节）。
 *
 * <p>该对象表示从 Markdown（标记文档）中按标题结构解析出来的一个章节片段。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkdownSection {

    /** sectionIndex（章节序号）表示章节在原文中的顺序。 */
    private int sectionIndex;

    /** titlePath（标题路径）表示当前章节所属的标题层级，例如 一级标题 / 二级标题。 */
    private String titlePath;

    /** content（章节内容）表示当前标题下直到下一个标题前的文本。 */
    private String content;
}
