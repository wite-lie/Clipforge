package com.clipforge.mq.rag.urlsync;

/**
 * KnowledgeUrlSyncProducer（知识库 URL 同步生产者）。
 *
 * <p>该接口屏蔽 RocketMQ（消息队列）实现，让应用服务只依赖 URL 同步投递抽象。
 */
public interface KnowledgeUrlSyncProducer {

    /**
     * send（发送）用于投递 URL 同步消息。
     *
     * @param message URL 同步消息
     */
    void send(KnowledgeUrlSyncMessage message);
}
