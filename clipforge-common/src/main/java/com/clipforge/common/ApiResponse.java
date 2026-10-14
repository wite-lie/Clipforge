package com.clipforge.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ApiResponse（统一接口响应）。
 *
 * <p>该对象用于统一 REST API（表述性状态转移接口）的返回格式，方便前端和测试稳定解析。
 *
 * @param <T> data（数据）字段承载的具体业务响应类型
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** success（是否成功）表示本次接口调用是否成功。 */
    private boolean success;

    /** code（响应码）表示机器可读的结果编码。 */
    private String code;

    /** message（响应消息）表示人类可读的结果说明。 */
    private String message;

    /** data（响应数据）表示接口成功时返回的业务数据。 */
    private T data;

    /**
     * ok（成功响应）用于快速构建成功接口响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        // 使用 Builder（建造者模式）构建统一成功响应。
        return ApiResponse.<T>builder()
                // 标记接口调用成功。
                .success(true)
                // 使用统一成功响应码。
                .code(ErrorCode.OK.getCode())
                // 使用统一成功响应消息。
                .message(ErrorCode.OK.getMessage())
                // 写入业务响应数据。
                .data(data)
                // 构建响应对象。
                .build();
    }

    /**
     * fail（失败响应）用于快速构建失败接口响应。
     *
     * @param errorCode 错误码
     * @param message 错误消息
     * @return 失败响应
     */
    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        // 使用 Builder（建造者模式）构建统一失败响应。
        return ApiResponse.<Void>builder()
                // 标记接口调用失败。
                .success(false)
                // 写入机器可读错误码。
                .code(errorCode.getCode())
                // 写入人类可读错误消息。
                .message(message)
                // 失败响应没有业务数据。
                .data(null)
                // 构建响应对象。
                .build();
    }
}

