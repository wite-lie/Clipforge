package com.clipforge.material;

import com.clipforge.material.application.MaterialUploadCommand;
import org.springframework.web.multipart.MultipartFile;

/**
 * FileStorage（文件存储）。
 *
 * <p>该接口屏蔽本地磁盘、MinIO（对象存储）等不同存储实现，业务层只依赖抽象。
 */
public interface FileStorage {

    /**
     * store（存储）用于保存上传文件并返回存储结果。
     *
     * @param file 上传文件
     * @return 文件存储结果
     */
    StoredFile store(MultipartFile file);

    /**
     * store（存储）用于保存素材上传命令中的文件内容。
     *
     * @param command 素材上传命令
     * @return 文件存储结果
     */
    StoredFile store(MaterialUploadCommand command);
}
