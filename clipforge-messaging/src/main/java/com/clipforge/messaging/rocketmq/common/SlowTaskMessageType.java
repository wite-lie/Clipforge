package com.clipforge.messaging.rocketmq.common;

/**
 * SlowTaskMessageType（慢任务消息类型）用于描述 RocketMQ（消息队列）中具体要执行的任务。
 */
public enum SlowTaskMessageType {

    /** IMAGE_UNDERSTANDING（图片理解）表示单张图片素材理解任务。 */
    IMAGE_UNDERSTANDING,

    /** VIDEO_PROCESSING（视频处理）表示单个视频素材完整处理任务。 */
    VIDEO_PROCESSING,

    /** KNOWLEDGE_DOCUMENT_PROCESS（知识文档处理）表示知识库文档解析、切片、向量化和入库任务。 */
    KNOWLEDGE_DOCUMENT_PROCESS,

    /** RAG_URL_SYNC（RAG URL 同步）表示知识库 URL 来源文档的定时同步和变更检测任务。 */
    RAG_URL_SYNC
}
