package com.clipforge.common;

/**
 * ErrorCode（错误码）。
 *
 * <p>该枚举集中管理接口和业务异常的错误编码，避免错误信息散落在各处。
 */
public enum ErrorCode {

    /** OK（成功）表示接口处理成功。 */
    OK("OK", "请求成功"),

    /** BAD_REQUEST（错误请求）表示请求参数不合法。 */
    BAD_REQUEST("BAD_REQUEST", "请求参数不合法"),

    /** KNOWLEDGE_FILE_ERROR（知识文件错误）表示静态知识库读取或解析失败。 */
    KNOWLEDGE_FILE_ERROR("KNOWLEDGE_FILE_ERROR", "知识文件读取失败"),

    /** AGENT_OUTPUT_INVALID（智能体输出不合法）表示生成结果未通过输出校验。 */
    AGENT_OUTPUT_INVALID("AGENT_OUTPUT_INVALID", "智能体输出不合法"),

    /** PERSISTENCE_DISABLED（持久化未启用）表示当前环境没有启用数据库持久化能力。 */
    PERSISTENCE_DISABLED("PERSISTENCE_DISABLED", "持久化能力未启用"),

    /** DATA_NOT_FOUND（数据不存在）表示按条件没有查询到业务数据。 */
    DATA_NOT_FOUND("DATA_NOT_FOUND", "数据不存在"),

    /** PROMPT_TEMPLATE_ERROR（提示词模板错误）表示提示词文件不存在或读取失败。 */
    PROMPT_TEMPLATE_ERROR("PROMPT_TEMPLATE_ERROR", "提示词模板错误"),

    /** MODEL_CONFIG_ERROR（模型配置错误）表示真实模型配置缺失或不合法。 */
    MODEL_CONFIG_ERROR("MODEL_CONFIG_ERROR", "模型配置错误"),

    /** MODEL_CALL_ERROR（模型调用错误）表示调用真实 LLM（大语言模型）失败。 */
    MODEL_CALL_ERROR("MODEL_CALL_ERROR", "模型调用失败"),

    /** MODEL_OUTPUT_PARSE_ERROR（模型输出解析错误）表示模型返回内容无法解析为系统需要的结构。 */
    MODEL_OUTPUT_PARSE_ERROR("MODEL_OUTPUT_PARSE_ERROR", "模型输出解析失败"),

    /** MODEL_OUTPUT_TRUNCATED（模型输出截断）表示模型返回的结构化内容疑似没有完整生成。 */
    MODEL_OUTPUT_TRUNCATED("MODEL_OUTPUT_TRUNCATED", "模型输出疑似被截断"),

    /** MATERIAL_UPLOAD_LIMITED（素材上传受限）表示素材上传并发已满或请求超过上传限制。 */
    MATERIAL_UPLOAD_LIMITED("MATERIAL_UPLOAD_LIMITED", "素材上传受限"),

    /** MATERIAL_NOT_READY（素材未就绪）表示素材理解结果尚未准备好。 */
    MATERIAL_NOT_READY("MATERIAL_NOT_READY", "素材未就绪"),

    /** KNOWLEDGE_UPLOAD_LIMITED（知识上传受限）表示知识文件上传并发已满或请求超过上传限制。 */
    KNOWLEDGE_UPLOAD_LIMITED("KNOWLEDGE_UPLOAD_LIMITED", "知识上传受限"),

    /** OCR_DISABLED（OCR 未启用）表示当前仅预留 OCR（光学字符识别）接口。 */
    OCR_DISABLED("OCR_DISABLED", "OCR 功能未启用"),

    /** ASYNC_PROCESS_UNAVAILABLE（异步处理不可用）表示消息队列异步处理能力不可用。 */
    ASYNC_PROCESS_UNAVAILABLE("ASYNC_PROCESS_UNAVAILABLE", "异步处理能力不可用"),

    /** INTERNAL_ERROR（内部错误）表示未预期的系统异常。 */
    INTERNAL_ERROR("INTERNAL_ERROR", "系统内部错误");

    /** code（错误编码）是机器可读的错误标识。 */
    private final String code;

    /** message（错误消息）是人类可读的错误说明。 */
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
