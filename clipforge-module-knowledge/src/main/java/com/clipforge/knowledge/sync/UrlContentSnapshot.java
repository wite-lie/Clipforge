package com.clipforge.knowledge.sync;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * UrlContentSnapshot（URL 内容快照）。
 *
 * <p>该对象表示一次远程抓取的结果，包括原始内容、响应头、哈希和建议文件名。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlContentSnapshot {

    /** sourceUrl（原始 URL）表示用户配置的原始地址。 */
    private String sourceUrl;

    /** resolvedUrl（解析后 URL）表示跟随重定向后的最终地址。 */
    private String resolvedUrl;

    /** suggestedFilename（建议文件名）表示落盘时使用的文件名。 */
    private String suggestedFilename;

    /** contentType（内容类型）表示远程返回的 MIME 类型。 */
    private String contentType;

    /** contentLength（内容长度）表示远程返回体的字节大小。 */
    private Long contentLength;

    /** etag（资源版本）表示远程资源的版本标识。 */
    private String etag;

    /** lastModified（最近修改时间）表示远程资源的修改时间。 */
    private String lastModified;

    /** contentBytes（内容字节）保存本次抓取到的完整内容。 */
    private byte[] contentBytes;

    /** notModified（未修改）表示远程服务通过 304 或强校验明确说明内容未变化。 */
    private Boolean notModified;

    /** httpStatus（HTTP 状态码）记录本次抓取的响应状态。 */
    private Integer httpStatus;

    /** contentHash（内容哈希）保存本次抓取内容的 SHA-256（安全哈希）。 */
    private String contentHash;

    /** fetchedAt（抓取时间）表示本次抓取完成时间。 */
    private LocalDateTime fetchedAt;

    /** fetchDurationMs（抓取耗时）表示远程抓取耗时。 */
    private Long fetchDurationMs;
}
