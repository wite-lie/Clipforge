package com.clipforge.messaging.rocketmq.common;

/**
 * SlowTaskBizType（慢任务业务类型）用于区分 RocketMQ（消息队列）慢任务所属业务域。
 */
public enum SlowTaskBizType {

    /** MATERIAL（素材）表示图片、视频等素材处理任务。 */
    MATERIAL,

    /** RAG（检索增强生成）表示知识库文档处理任务。 */
    RAG,

    /** EVALUATION（评估）表示后续可能异步化的方案评估或质量复盘任务。 */
    EVALUATION
}
