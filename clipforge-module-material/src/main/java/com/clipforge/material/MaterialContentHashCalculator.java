package com.clipforge.material;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

/**
 * MaterialContentHashCalculator（素材内容哈希计算器）。
 *
 * <p>该组件用于根据素材文件内容计算 SHA-256（安全哈希算法）哈希值，让图片理解结果可以判断是否能够复用。
 */
@Component
public class MaterialContentHashCalculator {

    /**
     * calculateSha256（计算 SHA-256 哈希）用于根据本地文件路径生成稳定内容指纹。
     *
     * @param filePath 文件路径
     * @return 十六进制 SHA-256 哈希
     */
    public String calculateSha256(String filePath) {
        // 校验文件路径，避免后续读取文件时出现不明确的异常。
        if (filePath == null || filePath.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "素材文件路径不能为空，无法计算内容哈希");
        }
        // 把字符串路径转换为 Path（文件路径对象）。
        Path path = Path.of(filePath);
        // 校验文件是否真实存在。
        if (!Files.exists(path)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "素材文件不存在，无法计算内容哈希：" + filePath);
        }
        try {
            // 创建 SHA-256（安全哈希算法）摘要器。
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            // 打开文件输入流，避免一次性把大文件全部读入内存。
            try (InputStream inputStream = Files.newInputStream(path)) {
                // 创建 8KB 缓冲区，用于分块读取文件内容。
                byte[] buffer = new byte[8192];
                // 保存每次读取到的字节数。
                int readLength;
                // 循环读取文件内容并更新摘要。
                while ((readLength = inputStream.read(buffer)) != -1) {
                    messageDigest.update(buffer, 0, readLength);
                }
            }
            // 把二进制摘要转换为十六进制字符串。
            return toHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException exception) {
            // JDK（Java 开发工具包）必然支持 SHA-256，这里只做兜底保护。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "当前 Java 运行环境不支持 SHA-256 哈希算法", exception);
        } catch (IOException exception) {
            // 文件读取失败时转换为业务异常，方便接口层返回明确错误。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取素材文件失败，无法计算内容哈希：" + exception.getMessage(), exception);
        }
    }

    /**
     * toHex（转十六进制）用于把二进制哈希结果转换为数据库可保存的字符串。
     *
     * @param bytes 二进制哈希结果
     * @return 十六进制字符串
     */
    private String toHex(byte[] bytes) {
        // 创建字符串构建器，长度为字节数的两倍。
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        // 遍历每个字节。
        for (byte value : bytes) {
            // 把字节转换为两位十六进制文本。
            builder.append(String.format("%02x", value));
        }
        // 返回最终十六进制哈希。
        return builder.toString();
    }
}
