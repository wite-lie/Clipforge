package com.clipforge.knowledge.sync;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * UrlContentFingerprintCalculator（URL 内容指纹计算器）。
 *
 * <p>该组件负责把 ETag（资源版本标识）、Last-Modified（最近修改时间）和内容哈希组合成一个稳定指纹。
 */
@Component
public class UrlContentFingerprintCalculator {

    /**
     * buildFingerprint（构建指纹）用于把多个维度拼成一个统一指纹。
     *
     * @param etag ETag
     * @param lastModified Last-Modified
     * @param contentHash 内容哈希
     * @return 统一指纹
     */
    public String buildFingerprint(String etag, String lastModified, String contentHash) {
        return normalize(etag) + "|" + normalize(lastModified) + "|" + normalize(contentHash);
    }

    /**
     * sameContent（内容是否一致）用于比较历史快照与当前抓取结果。
     *
     * @param previousEtag 历史 ETag
     * @param previousLastModified 历史 Last-Modified
     * @param previousContentHash 历史内容哈希
     * @param snapshot 当前抓取结果
     * @return 是否一致
     */
    public boolean sameContent(String previousEtag, String previousLastModified, String previousContentHash, UrlContentSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (hasText(previousContentHash) && hasText(snapshot.getContentHash())) {
            return Objects.equals(normalize(previousContentHash), normalize(snapshot.getContentHash()));
        }
        if (hasText(previousEtag) && hasText(snapshot.getEtag()) && Objects.equals(normalize(previousEtag), normalize(snapshot.getEtag()))) {
            return true;
        }
        return hasText(previousLastModified)
                && hasText(snapshot.getLastModified())
                && Objects.equals(normalize(previousLastModified), normalize(snapshot.getLastModified()));
    }

    /**
     * sha256（安全哈希）用于计算字节数组哈希。
     *
     * @param content 内容字节
     * @return 十六进制哈希
     */
    public String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content == null ? new byte[0] : content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", exception);
        }
    }

    /**
     * normalize（标准化）用于把空值统一成空字符串。
     *
     * @param value 原始值
     * @return 标准化结果
     */
    public String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
