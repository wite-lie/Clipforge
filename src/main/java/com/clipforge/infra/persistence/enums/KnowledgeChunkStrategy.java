package com.clipforge.infra.persistence.enums;

/**
 * KnowledgeChunkStrategy（知识切片策略）。
 *
 * <p>该枚举用于统一知识库和知识文档中的 chunkStrategy（切片策略）字段取值。
 */
public enum KnowledgeChunkStrategy {

    /** RECURSIVE（递归切片）表示优先按自然边界递归拆分文本，是当前默认策略。 */
    RECURSIVE,

    /** PARAGRAPH（段落切片）表示优先按段落边界切分文本。 */
    PARAGRAPH,

    /** LINE（行切片）表示优先按换行边界切分文本。 */
    LINE,

    /** SENTENCE（句子切片）表示优先按句子边界切分文本。 */
    SENTENCE,

    /** CHARACTER（字符切片）表示按字符长度稳定切分文本。 */
    CHARACTER,

    /** REGEX（正则切片）表示按用户配置的正则表达式切分文本。 */
    REGEX,

    MARKDOWN_SECTION;

    /**
     * fromValue（按值解析）用于把数据库或请求中的字符串转换为切片策略枚举。
     *
     * @param value 原始策略值
     * @return 切片策略枚举
     */
    public static KnowledgeChunkStrategy fromValue(String value) {
        // 如果策略为空，则返回默认策略。
        if (value == null || value.isBlank()) {
            // 返回默认策略。
            return RECURSIVE;
        }
        // 把策略值标准化为大写。
        String normalizedValue = value.strip().toUpperCase();
        // 遍历所有枚举值。
        for (KnowledgeChunkStrategy strategy : values()) {
            // 如果枚举名称匹配，则返回该枚举。
            if (strategy.name().equals(normalizedValue)) {
                // 返回匹配策略。
                return strategy;
            }
        }
        // 未匹配时返回空引用，由调用方决定错误处理方式。
        return null;
    }

    /**
     * value（数据库值）用于返回当前枚举写入数据库的字符串。
     *
     * @return 数据库切片策略值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }

    /**
     * defaultValue（默认值）用于返回当前项目推荐的默认切片策略。
     *
     * @return 默认切片策略值
     */
    public static String defaultValue() {
        // 第一版默认使用递归切片。
        return RECURSIVE.value();
    }
}
