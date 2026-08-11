package com.clipforge.material;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * LocalFileStorage（本地文件存储）。
 *
 * <p>该实现把上传素材保存到本地磁盘目录，后续可以替换为 MinIO（对象存储）实现。
 */
@Component
public class LocalFileStorage implements FileStorage {

    /** materialUploadProperties（素材上传配置）提供本地保存根路径。 */
    private final MaterialUploadProperties materialUploadProperties;

    /**
     * 构造函数用于注入素材上传配置。
     *
     * @param materialUploadProperties 素材上传配置
     */
    public LocalFileStorage(MaterialUploadProperties materialUploadProperties) {
        // 保存素材上传配置。
        this.materialUploadProperties = materialUploadProperties;
    }

    /**
     * store（存储）用于把 MultipartFile（多部分上传文件）保存到本地磁盘。
     *
     * @param file 上传文件
     * @return 文件存储结果
     */
    @Override
    public StoredFile store(MultipartFile file) {
        // 读取原始文件名。
        String originalFilename = file.getOriginalFilename();
        // 提取安全扩展名。
        String extension = extractExtension(originalFilename);
        // 生成系统存储文件名。
        String storedFilename = "material_" + UUID.randomUUID().toString().replace("-", "") + extension;
        // 按日期分目录，避免单目录文件过多。
        String datePath = LocalDate.now().toString();
        // 构建目标目录路径。
        Path targetDirectory = Path.of(materialUploadProperties.getRootPath(), datePath);
        // 构建目标文件路径。
        Path targetFile = targetDirectory.resolve(storedFilename);
        try {
            // 创建目标目录。
            Files.createDirectories(targetDirectory);
            // 使用 transferTo（传输到）写入文件，避免 getBytes（读取全部字节）造成内存占用。
            file.transferTo(targetFile);
            // 返回存储结果。
            return StoredFile.builder()
                    // 设置存储文件名。
                    .storedFilename(storedFilename)
                    // 设置文件路径。
                    .filePath(targetFile.toString())
                    // 构建结果。
                    .build();
        } catch (IOException exception) {
            // 文件写入失败时抛出业务异常。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "素材文件保存失败", exception);
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
