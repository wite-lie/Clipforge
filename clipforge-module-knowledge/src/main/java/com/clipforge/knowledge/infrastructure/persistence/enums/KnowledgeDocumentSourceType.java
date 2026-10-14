package com.clipforge.knowledge.infrastructure.persistence.enums;

/**
 * KnowledgeDocumentSourceType（知识文档来源类型）。
 *
 * <p>该枚举用于统一 cf_knowledge_document（知识文档表）的 source_type（来源类型）字段取值。
 */
public enum KnowledgeDocumentSourceType {

    /** LOCAL_UPLOAD（本地上传）表示用户通过接口直接上传知识文件。 */
    LOCAL_UPLOAD,

    /** URL（统一资源定位符）表示后续从远程地址抓取知识文件。 */
    URL,

    /** SYSTEM_SEED（系统种子）表示系统内置的初始化知识文件。 */
    SYSTEM_SEED;

    /**
     * value（数据库值）用于返回当前枚举写入数据库的字符串。
     *
     * @return 数据库来源类型值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}
