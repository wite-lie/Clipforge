package com.clipforge.infra.persistence.enums;

/**
 * KnowledgeBaseStatus（知识库状态）。
 *
 * <p>该枚举用于统一 cf_knowledge_base（知识库表）的 status（状态）字段取值。
 */
public enum KnowledgeBaseStatus {

    /** ACTIVE（启用中）表示知识库可以正常上传文档和参与检索。 */
    ACTIVE,

    /** ARCHIVED（已归档）表示知识库被归档，后续可用于管理接口禁用检索。 */
    ARCHIVED;

    /**
     * value（数据库值）用于返回当前枚举写入数据库的字符串。
     *
     * @return 数据库状态值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}
