package com.clipforge.rag.recovery;

/**
 * KnowledgeDocumentProcessErrorType（知识文档处理错误类型）。
 *
 * <p>该枚举用于把 RAG（检索增强生成）文档处理失败原因结构化，方便状态查询、人工恢复和后续质量复盘。
 */
public enum KnowledgeDocumentProcessErrorType {

    /** FILE_NOT_FOUND（文件不存在）表示数据库记录存在，但本地文件路径不可读。 */
    FILE_NOT_FOUND,

    /** FILE_DAMAGED（文件损坏）表示文件存在但解析器无法读取有效内容。 */
    FILE_DAMAGED,

    /** DOCUMENT_PARSE_FAILED（文档解析失败）表示 Tika（通用文档解析工具）或文本解析阶段失败。 */
    DOCUMENT_PARSE_FAILED,

    /** CHUNK_FAILED（切片失败）表示文档内容无法正常切成知识片段。 */
    CHUNK_FAILED,

    /** EMBEDDING_MODEL_FAILED（嵌入模型失败）表示 Embedding（嵌入）模型调用失败。 */
    EMBEDDING_MODEL_FAILED,

    /** VECTOR_STORE_FAILED（向量入库失败）表示 pgvector（PostgreSQL 向量扩展）写入失败。 */
    VECTOR_STORE_FAILED,

    /** MQ_SEND_FAILED（消息发送失败）表示 RocketMQ（消息队列）生产者发送失败。 */
    MQ_SEND_FAILED,

    /** MQ_CONSUME_FAILED（消息消费失败）表示 RocketMQ（消息队列）消费者处理失败。 */
    MQ_CONSUME_FAILED,

    /** TASK_TIMEOUT（任务超时）表示任务长时间停留在运行中，需要恢复处理。 */
    TASK_TIMEOUT,

    /** RETRY_LIMIT_EXCEEDED（重试次数超限）表示文档已经达到最大重试次数。 */
    RETRY_LIMIT_EXCEEDED,

    /** UNKNOWN（未知错误）表示暂时无法精确归类的异常。 */
    UNKNOWN;

    /**
     * value（文本值）用于写入日志或接口响应。
     *
     * @return 错误类型文本
     */
    public String value() {
        return name();
    }
}
