package com.clipforge.rag;

import dev.langchain4j.data.document.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DocumentParseResult（文档解析结果）。
 *
 * <p>该对象用于承载 KnowledgeDocumentParser（知识文档解析器）输出的正文、解析器名称和文档级解析元数据。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseResult {

    /** document（文档）是 LangChain4j（LangChain 的 Java 封装）的统一文档对象，后续直接进入切片链路。 */
    private Document document;

    /** parserName（解析器名称）表示本次实际使用的解析器，例如 TextDocumentParser 或 Apache Tika。 */
    private String parserName;

    /** fileExtension（文件扩展名）表示当前文档的扩展名，例如 .md、.pdf、.docx。 */
    private String fileExtension;

    /** extractedTextLength（抽取文本长度）表示解析后正文的字符数量。 */
    private Integer extractedTextLength;

    /** pageCount（页数）表示 PDF（便携式文档格式）或 Word（文字处理文档）解析得到的页数，无法获取时为空。 */
    private Integer pageCount;

    /** parserWarnings（解析警告）表示扫描版 PDF（便携式文档格式）无文本层等非致命提示。 */
    private String parserWarnings;
}
