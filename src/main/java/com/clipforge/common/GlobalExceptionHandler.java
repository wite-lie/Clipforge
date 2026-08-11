package com.clipforge.common;

import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * GlobalExceptionHandler（全局异常处理器）。
 *
 * <p>该组件统一拦截 Controller（控制器）层抛出的异常，并转换为 ApiResponse（统一接口响应）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * handleValidationException（处理参数校验异常）用于处理 `@Valid` 触发的请求参数错误。
     *
     * @param exception 参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        // 提取所有字段错误，并拼接成可读提示。
        String message = exception.getBindingResult().getFieldErrors().stream()
                // 把字段名和错误信息拼接起来。
                .map(this::formatFieldError)
                // 使用分号连接多个字段错误。
                .collect(Collectors.joining("；"));
        // 返回 400（错误请求）状态码和统一失败响应。
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(ErrorCode.BAD_REQUEST, message));
    }

    /**
     * handleBusinessException（处理业务异常）用于处理可预期的业务错误。
     *
     * @param exception 业务异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        // 根据业务异常中的错误码构造失败响应。
        ApiResponse<Void> response = ApiResponse.fail(exception.getErrorCode(), exception.getMessage());
        // 业务异常通常表示请求无法完成，这里统一返回 400（错误请求）。
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * handleMaxUploadSizeExceededException（处理上传大小超限异常）用于返回统一上传限制提示。
     *
     * @param exception 上传大小超限异常
     * @param request HTTP（超文本传输协议）请求
     * @return 统一失败响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        // 根据请求路径判断是知识文件上传还是素材上传。
        boolean knowledgeUpload = request.getRequestURI().startsWith("/api/knowledge-bases/") && request.getRequestURI().endsWith("/documents");
        // 知识文件上传超限时返回知识上传错误码。
        ErrorCode errorCode = knowledgeUpload ? ErrorCode.KNOWLEDGE_UPLOAD_LIMITED : ErrorCode.MATERIAL_UPLOAD_LIMITED;
        // 返回 400（错误请求）状态码和统一失败响应。
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                // 说明当前请求超过 multipart（多部分表单）上传大小限制。
                .body(ApiResponse.fail(errorCode, "上传文件超过大小限制，请压缩文件或减少单次上传数量后重试"));
    }

    /**
     * handleUnknownException（处理未知异常）用于兜底处理未预期异常。
     *
     * @param exception 未知异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception exception) {
        // 返回 500（服务器内部错误）状态码和统一失败响应。
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                // 不把异常堆栈直接暴露给调用方，避免泄露内部实现。
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    /**
     * formatFieldError（格式化字段错误）用于把单个字段校验错误转为中文提示。
     *
     * @param fieldError 字段错误
     * @return 格式化后的字段错误
     */
    private String formatFieldError(FieldError fieldError) {
        // 拼接字段名和默认错误消息。
        return fieldError.getField() + "：" + fieldError.getDefaultMessage();
    }
}
