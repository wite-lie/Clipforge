package com.clipforge.mq.common;

/**
 * SlowTaskTriggerType（慢任务触发类型）用于说明异步任务由谁、在什么场景下触发。
 */
public enum SlowTaskTriggerType {

    /** UPLOAD_AUTO（上传后自动触发）表示素材或文档上传成功后自动投递处理任务。 */
    UPLOAD_AUTO,

    /** MANUAL（手动触发）表示用户或接口主动触发处理任务。 */
    MANUAL,

    /** MANUAL_RETRY（手动重试）表示用户对失败任务主动重试。 */
    MANUAL_RETRY,

    /** MQ_RETRY（消息重试）表示由消息队列重投或系统补偿触发。 */
    MQ_RETRY,

    /** SCHEDULE（定时触发）表示由定时任务触发。 */
    SCHEDULE,

    /** SYSTEM（系统触发）表示系统初始化、修复或后台补偿触发。 */
    SYSTEM
}
