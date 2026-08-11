package com.clipforge.rag;

import org.springframework.web.multipart.MultipartFile;

/**
 * KnowledgeFileStorage（知识文件存储）。
 *
 * <p>该接口屏蔽本地磁盘、MinIO（对象存储）等不同存储实现，RAG（检索增强生成）业务层只依赖抽象。
 */
public interface KnowledgeFileStorage {

    /**
     * store（存储）用于保存上传的知识文件并返回存储结果。
     *
     * @param file 上传文件
     * @return 知识文件存储结果
     */
    KnowledgeStoredFile store(MultipartFile file);
}
