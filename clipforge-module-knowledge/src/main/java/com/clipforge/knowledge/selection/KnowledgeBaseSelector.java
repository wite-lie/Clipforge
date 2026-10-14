package com.clipforge.knowledge.selection;

/**
 * KnowledgeBaseSelector（知识库选择器）。
 *
 * <p>该接口屏蔽“显式选择、项目默认、平台默认、全库兜底”等规则细节，让创作应用服务只依赖抽象。
 */
public interface KnowledgeBaseSelector {

    /**
     * select（选择）用于根据请求上下文解析本次应使用的知识库范围。
     *
     * @param request 知识库选择请求
     * @return 知识库选择结果
     */
    KnowledgeBaseSelectionResult select(KnowledgeBaseSelectionRequest request);
}
