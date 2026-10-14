package com.clipforge.material.application;

import java.io.IOException;
import java.io.InputStream;

/**
 * MaterialUploadContent（素材上传内容）。
 *
 * <p>该接口用 JDK（Java 开发工具包）输入流表达上传文件内容，避免 Material 模块边界依赖 Web 层的 MultipartFile。
 */
@FunctionalInterface
public interface MaterialUploadContent {

    /**
     * openStream（打开输入流）用于读取上传文件内容。
     *
     * @return 文件输入流
     * @throws IOException 读取文件内容失败
     */
    InputStream openStream() throws IOException;
}
