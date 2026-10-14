package com.clipforge.knowledge.selection;

/**
 * KnowledgeBaseSelectionSource（知识库选择来源）。
 *
 * <p>该枚举用于说明本次创作请求最终使用的知识库范围来自哪里，方便 Trace（执行链路追踪）和前端展示。
 */
public enum KnowledgeBaseSelectionSource {

    /** EXPLICIT_REQUEST（显式请求）表示用户或前端明确传入了 knowledgeBaseIds（知识库编号列表）。 */
    EXPLICIT_REQUEST,

    /** PROJECT_DEFAULT（项目默认）表示命中了项目默认知识库配置。 */
    PROJECT_DEFAULT,

    /** PLATFORM_DEFAULT（平台默认）表示命中了平台默认知识库配置。 */
    PLATFORM_DEFAULT,

    /** ALL_KNOWLEDGE_BASES（全部知识库）表示未命中具体知识库范围，改为全库检索。 */
    ALL_KNOWLEDGE_BASES,

    /** NONE（无选择）表示没有可用知识库范围，后续检索器可继续静态降级。 */
    NONE
}
