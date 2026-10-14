package com.clipforge.rag;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.sync.KnowledgeDocumentUrlSyncProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * KnowledgeUrlFileStorage（URL 文档本地存储器）。
 *
 * <p>该组件负责把远程抓取到的 URL（统一资源定位符）内容保存成本地文件，供后续解析、切片和向量化复用。
 */
@Component
@RequiredArgsConstructor
public class KnowledgeUrlFileStorage {

    /** properties（同步配置）提供本地落盘根路径。 */
    private final KnowledgeDocumentUrlSyncProperties properties;

    /**
     * store（保存）用于把远程抓取的内容写入本地文件。
     *
     * @param documentId 知识文档编号
     * @param suggestedFilename 建议文件名
     * @param contentType 内容类型
     * @param contentBytes 文件内容
     * @return 本地存储结果
     */
    public KnowledgeStoredFile store(String documentId, String suggestedFilename, String contentType, byte[] contentBytes) {
        String extension = resolveExtension(suggestedFilename, contentType);
        String storedFilename = "url_document_" + documentId + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetDirectory = Path.of(properties.getStorageRootPath(), LocalDate.now().toString());
        Path targetFile = targetDirectory.resolve(storedFilename);
        try {
            Files.createDirectories(targetDirectory);
            Files.write(targetFile, contentBytes);
            return KnowledgeStoredFile.builder()
                    .storedFilename(storedFilename)
                    .filePath(targetFile.toString())
                    .fileHash(sha256(contentBytes))
                    .build();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "URL 文档本地保存失败", exception);
        }
    }

    private String resolveExtension(String suggestedFilename, String contentType) {
        if (suggestedFilename != null && suggestedFilename.contains(".")) {
            int index = suggestedFilename.lastIndexOf('.');
            if (index > -1 && index < suggestedFilename.length() - 1) {
                return suggestedFilename.substring(index).toLowerCase(Locale.ROOT);
            }
        }
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

    private String sha256(byte[] contentBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(contentBytes == null ? new byte[0] : contentBytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "URL 文档哈希计算失败", exception);
        }
    }
}
