package com.clipforge.rag.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OcrEngineRequest（OCR 识别请求）。
 *
 * <p>该对象封装 OCR（光学字符识别）需要的文件信息和场景信息，方便后续接入真实供应商时扩展。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrEngineRequest {

    /** documentId（文档编号）用于关联知识文档。 */
    private String documentId;

    /** originalFilename（原始文件名）用于日志和排障。 */
    private String originalFilename;

    /** filePath（文件路径）表示本地文件存储位置。 */
    private String filePath;

    /** fileExtension（文件扩展名）表示当前文件的后缀。 */
    private String fileExtension;

    /** sourceType（来源类型）表示 PDF（便携式文档格式）还是图片。 */
    private String sourceType;

    /** reason（触发原因）用于说明为什么需要走 OCR。 */
    private String reason;
}
