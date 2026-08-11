package com.clipforge.common;

/**
 * BusinessException（业务异常）。
 *
 * <p>该异常用于表达可预期的业务错误，例如知识文件缺失或 Agent（智能体）输出不完整。
 */
public class BusinessException extends RuntimeException {

    /** errorCode（错误码）表示当前业务异常对应的统一错误类型。 */
    private final ErrorCode errorCode;

    /**
     * 构造函数用于创建业务异常。
     *
     * @param errorCode 错误码
     * @param message 错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数用于创建带原始异常的业务异常。
     *
     * @param errorCode 错误码
     * @param message 错误消息
     * @param cause 原始异常
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
