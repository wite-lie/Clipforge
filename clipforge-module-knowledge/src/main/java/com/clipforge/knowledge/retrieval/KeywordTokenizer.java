package com.clipforge.knowledge.retrieval;

import java.util.List;

/**
 * KeywordTokenizer（关键词分词器）。
 *
 * <p>该接口用于把用户查询文本拆成可供 BM25（最佳匹配 25）关键词检索使用的词项列表。后续可以无缝替换为 HanLP（中文自然语言处理工具）、jieba（中文分词工具）等更强分词器。
 */
public interface KeywordTokenizer {

    /**
     * tokenize（分词）用于把查询文本拆成有序去重的关键词列表。
     *
     * @param text 原始查询文本
     * @return 关键词列表
     */
    List<String> tokenize(String text);
}
