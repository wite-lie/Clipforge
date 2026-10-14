package com.clipforge.knowledge.sync;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DefaultUrlContentFetcher（默认 URL 内容抓取器）。
 *
 * <p>该实现基于 Java（爪哇语言）标准 HttpClient（HTTP 客户端）完成抓取，并支持简单重定向、大小限制和安全校验。
 */
@Component
@RequiredArgsConstructor
public class DefaultUrlContentFetcher implements UrlContentFetcher {

    /** properties（同步配置）控制抓取超时、重定向和大小限制。 */
    private final KnowledgeDocumentUrlSyncProperties properties;

    /** validator（安全校验器）用于拦截危险 URL。 */
    private final UrlSafetyValidator validator;

    /** fingerprintCalculator（指纹计算器）用于计算响应内容哈希。 */
    private final UrlContentFingerprintCalculator fingerprintCalculator;

    @Override
    public UrlContentSnapshot fetch(String sourceUrl) {
        return fetchIfChanged(sourceUrl, null, null);
    }

    @Override
    public UrlContentSnapshot fetchIfChanged(String sourceUrl, String previousEtag, String previousLastModified) {
        long start = System.currentTimeMillis();
        validator.validate(sourceUrl, properties.getAllowedHosts(), properties.isBlockPrivateNetwork());
        try {
            URI currentUri = URI.create(sourceUrl);
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
                    .build();
            int redirectCount = 0;
            while (true) {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(currentUri)
                        .timeout(Duration.ofMillis(properties.getRequestTimeoutMillis()))
                        .GET();
                if (hasText(previousEtag)) {
                    requestBuilder.header("If-None-Match", previousEtag);
                }
                if (hasText(previousLastModified)) {
                    requestBuilder.header("If-Modified-Since", previousLastModified);
                }
                HttpRequest request = requestBuilder.build();
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int statusCode = response.statusCode();
                if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    closeQuietly(response.body());
                    return UrlContentSnapshot.builder()
                            .sourceUrl(sourceUrl)
                            .resolvedUrl(currentUri.toString())
                            .etag(response.headers().firstValue("ETag").orElse(previousEtag))
                            .lastModified(response.headers().firstValue("Last-Modified").orElse(previousLastModified))
                            .notModified(true)
                            .httpStatus(statusCode)
                            .fetchedAt(LocalDateTime.now())
                            .fetchDurationMs(System.currentTimeMillis() - start)
                            .build();
                }
                if (isRedirect(statusCode)) {
                    redirectCount++;
                    if (redirectCount > properties.getMaxRedirects()) {
                        closeQuietly(response.body());
                        throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 重定向次数超限");
                    }
                    String location = response.headers().firstValue("Location").orElse(null);
                    if (!hasText(location)) {
                        closeQuietly(response.body());
                        throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 重定向缺少 Location 头");
                    }
                    closeQuietly(response.body());
                    currentUri = currentUri.resolve(location);
                    validator.validate(currentUri.toString(), properties.getAllowedHosts(), properties.isBlockPrivateNetwork());
                    continue;
                }
                if (statusCode < 200 || statusCode >= 300) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 抓取失败，HTTP 状态码：" + statusCode);
                }
                String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
                String etag = response.headers().firstValue("ETag").orElse(null);
                String lastModified = response.headers().firstValue("Last-Modified").orElse(null);
                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                byte[] bodyBytes = readBody(response.body(), properties.getMaxContentBytes());
                if (contentLength > properties.getMaxContentBytes() || bodyBytes.length > properties.getMaxContentBytes()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 内容超过最大限制");
                }
                return UrlContentSnapshot.builder()
                        .sourceUrl(sourceUrl)
                        .resolvedUrl(currentUri.toString())
                        .suggestedFilename(resolveSuggestedFilename(currentUri, contentType))
                        .contentType(contentType)
                        .contentLength((long) bodyBytes.length)
                        .etag(etag)
                        .lastModified(lastModified)
                        .contentBytes(bodyBytes)
                        .notModified(false)
                        .httpStatus(statusCode)
                        .contentHash(fingerprintCalculator.sha256(bodyBytes))
                        .fetchedAt(LocalDateTime.now())
                        .fetchDurationMs(System.currentTimeMillis() - start)
                        .build();
            }
        } catch (HttpTimeoutException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "URL 抓取超时", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "URL 抓取被中断", exception);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "URL 抓取失败", exception);
        }
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == HttpURLConnection.HTTP_MOVED_PERM
                || statusCode == HttpURLConnection.HTTP_MOVED_TEMP
                || statusCode == HttpURLConnection.HTTP_SEE_OTHER
                || statusCode == 307
                || statusCode == 308;
    }

    private void closeQuietly(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }
        try {
            inputStream.close();
        } catch (IOException ignored) {
            // 关闭 304 响应流失败不影响变更检测结果。
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private byte[] readBody(InputStream inputStream, long maxBytes) throws IOException {
        try (InputStream stream = inputStream;
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = stream.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 内容超过最大限制");
                }
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    private String resolveSuggestedFilename(URI uri, String contentType) {
        String path = uri.getPath();
        if (path != null && !path.isBlank()) {
            int index = path.lastIndexOf('/');
            String candidate = index >= 0 ? path.substring(index + 1) : path;
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        String extension = resolveExtension(contentType);
        return "remote_document" + extension;
    }

    private String resolveExtension(String contentType) {
        if (contentType == null) {
            return ".txt";
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (normalized.contains("markdown")) {
            return ".md";
        }
        if (normalized.contains("html")) {
            return ".html";
        }
        if (normalized.contains("plain")) {
            return ".txt";
        }
        if (normalized.contains("pdf")) {
            return ".pdf";
        }
        if (normalized.contains("wordprocessingml.document")) {
            return ".docx";
        }
        if (normalized.contains("msword")) {
            return ".doc";
        }
        return ".txt";
    }
}
