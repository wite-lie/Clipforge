package com.clipforge.knowledge.application.api;

import java.io.IOException;
import java.io.InputStream;

/**
 * KnowledgeDocumentUploadContent（知识文档上传内容）提供可打开的文件输入流。
 *
 * <p>该接口允许 Controller 将 MultipartFile 转换为稳定的 Java 输入流边界，应用服务和存储层不再依赖 Web 层类型。
 */
@FunctionalInterface
public interface KnowledgeDocumentUploadContent {

    /**
     * openStream（打开输入流）用于读取上传文件内容。
     *
     * @return 上传文件输入流
     * @throws IOException 文件流打开失败
     */
    InputStream openStream() throws IOException;
}
