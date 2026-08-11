package com.clipforge.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KnowledgeStoredFile（已存储知识文件）。
 *
 * <p>该对象表示知识文件写入本地存储后的文件名、路径和内容哈希。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeStoredFile {

    /** storedFilename（存储文件名）表示系统生成的安全文件名。 */
    private String storedFilename;

    /** filePath（文件路径）表示文件在本地磁盘中的保存位置。 */
    private String filePath;

    /** fileHash（文件哈希）表示文件内容的 SHA-256（安全哈希算法）摘要。 */
    private String fileHash;
}
