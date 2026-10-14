package com.clipforge.server.filter;

import com.clipforge.common.ApiResponse;
import com.clipforge.common.ErrorCode;
import com.clipforge.rag.KnowledgeUploadProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * KnowledgeUploadRateLimitFilter（知识文件上传限流过滤器）。
 *
 * <p>该过滤器位于 DispatcherServlet（请求分发器）之前，用本地 Semaphore（信号量）在 multipart（多部分表单）解析前控制知识文件上传并发。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class KnowledgeUploadRateLimitFilter extends OncePerRequestFilter {

    /** knowledgeUploadProperties（知识上传配置）提供并发数和等待时间。 */
    private final KnowledgeUploadProperties knowledgeUploadProperties;

    /** objectMapper（对象映射器）用于输出统一 JSON（对象表示格式）响应。 */
    private final ObjectMapper objectMapper;

    /** semaphore（信号量）用于限制当前单实例同时处理的知识文件上传请求数。 */
    private volatile Semaphore semaphore;

    /** semaphorePermits（信号量许可数）用于在配置变化或初始化时识别是否需要重建信号量。 */
    private volatile int semaphorePermits;

    /**
     * 构造函数用于注入上传配置和 JSON（对象表示格式）工具。
     *
     * @param knowledgeUploadProperties 知识上传配置
     * @param objectMapper 对象映射器
     */
    public KnowledgeUploadRateLimitFilter(KnowledgeUploadProperties knowledgeUploadProperties, ObjectMapper objectMapper) {
        // 保存知识上传配置。
        this.knowledgeUploadProperties = knowledgeUploadProperties;
        // 保存对象映射器。
        this.objectMapper = objectMapper;
        // 初始化信号量。
        rebuildSemaphoreIfNeeded();
    }

    /**
     * doFilterInternal（执行过滤）用于在知识文件上传接口进入 multipart（多部分表单）解析前获取上传许可。
     *
     * @param request HTTP（超文本传输协议）请求
     * @param response HTTP（超文本传输协议）响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet（服务器小程序）异常
     * @throws IOException 输入输出异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 如果不是知识文档上传接口，则直接放行。
        if (!isKnowledgeDocumentUploadRequest(request)) {
            // 继续执行后续过滤器。
            filterChain.doFilter(request, response);
            // 返回。
            return;
        }
        // 按当前配置确保信号量已经初始化。
        rebuildSemaphoreIfNeeded();
        // 尝试获取上传许可。
        boolean acquired = acquirePermit();
        // 如果没有拿到许可，则直接返回 429（请求过多）。
        if (!acquired) {
            // 写入上传受限响应。
            writeLimitedResponse(response);
            // 返回，避免进入 multipart（多部分表单）解析。
            return;
        }
        try {
            // 拿到许可后才允许进入后续 Filter（过滤器）和 DispatcherServlet（请求分发器）。
            filterChain.doFilter(request, response);
        } finally {
            // 请求完成后释放许可，避免许可泄漏。
            semaphore.release();
        }
    }

    /**
     * isKnowledgeDocumentUploadRequest（是否知识文档上传请求）用于识别需要上传限流的接口。
     *
     * @param request HTTP（超文本传输协议）请求
     * @return 是否知识文档上传请求
     */
    private boolean isKnowledgeDocumentUploadRequest(HttpServletRequest request) {
        // 只拦截 POST（提交）方法。
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            // 非 POST 请求不拦截。
            return false;
        }
        // 读取请求路径。
        String uri = request.getRequestURI();
        // 只拦截 /api/knowledge-bases/{knowledgeBaseId}/documents 上传接口。
        return uri.startsWith("/api/knowledge-bases/") && uri.endsWith("/documents");
    }

    /**
     * acquirePermit（获取许可）用于根据配置决定立即失败还是等待一小段时间。
     *
     * @return 是否获取成功
     */
    private boolean acquirePermit() {
        try {
            // 如果最大等待时间小于等于 0，则立即尝试获取许可。
            if (knowledgeUploadProperties.getMaxWaitMillis() <= 0) {
                // 立即获取许可。
                return semaphore.tryAcquire();
            }
            // 在配置等待时间内尝试获取许可。
            return semaphore.tryAcquire(knowledgeUploadProperties.getMaxWaitMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            // 恢复线程中断状态。
            Thread.currentThread().interrupt();
            // 视为获取失败。
            return false;
        }
    }

    /**
     * writeLimitedResponse（写入限流响应）用于返回统一 JSON（对象表示格式）错误。
     *
     * @param response HTTP（超文本传输协议）响应
     * @throws IOException 输入输出异常
     */
    private void writeLimitedResponse(HttpServletResponse response) throws IOException {
        // 设置 HTTP（超文本传输协议）状态码为 429。
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        // 设置响应内容类型为 JSON（对象表示格式）。
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // 设置响应字符编码。
        response.setCharacterEncoding("UTF-8");
        // 写入统一失败响应。
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(ErrorCode.KNOWLEDGE_UPLOAD_LIMITED, "当前知识文件上传并发已满，请稍后再试"));
    }

    /**
     * rebuildSemaphoreIfNeeded（按需重建信号量）用于根据配置初始化本地上传并发控制。
     */
    private synchronized void rebuildSemaphoreIfNeeded() {
        // 读取配置的并发数，最小为 1。
        int configuredPermits = Math.max(1, knowledgeUploadProperties.getMaxConcurrent());
        // 如果信号量已存在且许可数没有变化，则无需重建。
        if (semaphore != null && semaphorePermits == configuredPermits) {
            // 直接返回。
            return;
        }
        // 创建公平信号量，避免高并发下后来的请求长期插队。
        semaphore = new Semaphore(configuredPermits, true);
        // 记录当前许可数。
        semaphorePermits = configuredPermits;
    }
}
