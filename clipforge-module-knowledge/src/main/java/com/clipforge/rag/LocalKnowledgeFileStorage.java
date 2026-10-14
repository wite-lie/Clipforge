package com.clipforge.rag;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.application.api.KnowledgeDocumentUploadCommand;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * LocalKnowledgeFileStorage（本地知识文件存储）。
 *
 * <p>该实现把上传的知识文件保存到本地磁盘目录，并计算 SHA-256（安全哈希算法）文件哈希，后续可替换为 MinIO（对象存储）。
 */
@Component
public class LocalKnowledgeFileStorage implements KnowledgeFileStorage {

    /** knowledgeUploadProperties（知识上传配置）提供本地保存根路径。 */
    private final KnowledgeUploadProperties knowledgeUploadProperties;

    /**
     * 构造函数用于注入知识上传配置。
     *
     * @param knowledgeUploadProperties 知识上传配置
     */
    public LocalKnowledgeFileStorage(KnowledgeUploadProperties knowledgeUploadProperties) {
        // 保存知识上传配置。
        this.knowledgeUploadProperties = knowledgeUploadProperties;
    }

    /**
     * store（存储）用于把上传命令中的文件流保存到本地磁盘并计算文件哈希。
     *
     * @param command 上传命令
     * @return 知识文件存储结果
     */
    @Override
    public KnowledgeStoredFile store(KnowledgeDocumentUploadCommand command) {
        // 读取原始文件名。
        String originalFilename = command.getOriginalFilename();
        // 提取安全扩展名。
        String extension = extractExtension(originalFilename);
        // 生成系统存储文件名。
        String storedFilename = "knowledge_document_" + UUID.randomUUID().toString().replace("-", "") + extension;
        // 按日期分目录，避免单目录文件过多。
        String datePath = LocalDate.now().toString();
        // 构建目标目录路径。
        Path targetDirectory = Path.of(knowledgeUploadProperties.getRootPath(), datePath);
        // 构建目标文件路径。
        Path targetFile = targetDirectory.resolve(storedFilename);
        try {
            // 创建目标目录。
            Files.createDirectories(targetDirectory);
            // 使用输入流复制，避免一次性读取全部字节造成内存占用。
            try (InputStream inputStream = command.getContent().openStream()) {
                Files.copy(inputStream, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            // 计算已落盘文件的 SHA-256（安全哈希算法）哈希。
            String fileHash = sha256(targetFile);
            // 返回存储结果。
            return KnowledgeStoredFile.builder()
                    // 设置存储文件名。
                    .storedFilename(storedFilename)
                    // 设置文件路径。
                    .filePath(targetFile.toString())
                    // 设置文件哈希。
                    .fileHash(fileHash)
                    // 构建结果。
                    .build();
        } catch (IOException exception) {
            // 文件写入失败时抛出业务异常。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "知识文件保存失败", exception);
        }
    }

    /**
     * sha256（计算 SHA-256）用于计算本地文件内容哈希。
     *
     * @param filePath 文件路径
     * @return 十六进制哈希字符串
     */
    private String sha256(Path filePath) {
        try {
            // 创建 SHA-256（安全哈希算法）摘要器。
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            // 打开文件输入流。
            try (InputStream inputStream = Files.newInputStream(filePath);
                 // 用 DigestInputStream（摘要输入流）边读取边更新哈希。
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest)) {
                // 创建缓冲区，避免一次性读取大文件。
                byte[] buffer = new byte[8192];
                // 循环读取文件直到结束。
                while (digestInputStream.read(buffer) != -1) {
                    // DigestInputStream（摘要输入流）会自动把读取到的字节加入摘要计算。
                }
            }
            // 把哈希字节转为十六进制字符串。
            return HexFormat.of().formatHex(messageDigest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            // 哈希计算失败时抛出知识文件错误。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "知识文件哈希计算失败", exception);
        }
    }

    /**
     * extractExtension（提取扩展名）用于从原始文件名中获取后缀。
     *
     * @param originalFilename 原始文件名
     * @return 扩展名
     */
    private String extractExtension(String originalFilename) {
        // 如果文件名为空，则使用空后缀。
        if (originalFilename == null || originalFilename.isBlank()) {
            // 返回空后缀。
            return "";
        }
        // 找到最后一个点号。
        int index = originalFilename.lastIndexOf('.');
        // 如果没有扩展名，则返回空后缀。
        if (index < 0 || index == originalFilename.length() - 1) {
            // 返回空后缀。
            return "";
        }
        // 返回小写扩展名。
        return originalFilename.substring(index).toLowerCase();
    }
}
