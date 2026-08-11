package com.clipforge.material;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * StoredFile（已存储文件）。
 *
 * <p>该对象表示文件写入存储后的路径和文件名信息。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

    /** storedFilename（存储文件名）表示系统生成的安全文件名。 */
    private String storedFilename;

    /** filePath（文件路径）表示文件在本地或对象存储中的位置。 */
    private String filePath;
}
