package com.clipforge.creation.export;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * CreationMarkdownFileStorage（创作 Markdown 文件存储）。
 *
 * <p>该组件负责把渲染后的 Markdown 内容写入本地文件系统。
 */
@Component
public class CreationMarkdownFileStorage {

    /** baseDirectory（基础目录）表示创作导出文件统一保存位置。 */
    private final Path baseDirectory = Path.of("data", "exports", "creation").toAbsolutePath().normalize();

    /**
     * save（保存）用于把 Markdown 文档写入本地导出目录。
     *
     * @param document Markdown 文档
     * @return 已保存文件路径
     */
    public Path save(CreationMarkdownDocument document) {
        try {
            Files.createDirectories(baseDirectory);
            Path targetPath = baseDirectory.resolve(document.getFileName()).normalize();
            if (!targetPath.startsWith(baseDirectory)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "导出文件名不合法");
            }
            Files.writeString(targetPath, document.getContent(), StandardCharsets.UTF_8);
            return targetPath;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Markdown 导出文件写入失败", exception);
        }
    }
}
