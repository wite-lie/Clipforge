package com.clipforge.mq.rag.document;

/**
 * KnowledgeDocumentProcessProducer（知识文档处理消息生产者）。
 *
 * <p>该接口屏蔽 RocketMQ（消息队列）实现细节，应用层只负责提交“要处理哪个文档”的业务消息。
 */
public interface KnowledgeDocumentProcessProducer {

    /**
     * send（发送）用于把知识文档处理消息投递到消息队列。
     *
     * @param message 知识文档处理消息
     */
    void send(KnowledgeDocumentProcessMessage message);
}
