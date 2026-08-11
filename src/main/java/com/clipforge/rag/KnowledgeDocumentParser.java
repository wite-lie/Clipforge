package com.clipforge.rag;

import com.clipforge.infra.persistence.entity.KnowledgeDocumentEntity;

/**
 * KnowledgeDocumentParser（知识文档解析器）。
 *
 * <p>该接口屏蔽 TextDocumentParser（文本解析器）和 Apache Tika（通用文档解析工具）的差异，让应用服务只依赖统一解析结果。
 */
public interface KnowledgeDocumentParser {

    /**
     * parse（解析）用于把本地知识文档解析为统一 DocumentParseResult（文档解析结果）。
     *
     * @param documentEntity 知识文档实体
     * @return 文档解析结果
     */
    DocumentParseResult parse(KnowledgeDocumentEntity documentEntity);
}
