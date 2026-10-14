package com.clipforge.creation.export;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CreationMarkdownDocument（创作 Markdown 文档）。
 */
@Getter
@AllArgsConstructor
public class CreationMarkdownDocument {

    /** fileName（文件名）表示建议保存的 Markdown 文件名。 */
    private final String fileName;

    /** content（文件内容）表示完整 Markdown 文本。 */
    private final String content;
}
