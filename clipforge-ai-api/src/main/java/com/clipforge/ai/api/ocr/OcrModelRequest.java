package com.clipforge.ai.api.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OcrModelRequest（OCR 模型请求）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrModelRequest {

    /** sourceId（来源编号）可以是知识文档编号或素材编号。 */
    private String sourceId;

    /** originalFilename（原始文件名）用于排障和供应商调用记录。 */
    private String originalFilename;

    /** filePath（文件路径）表示本地文件或已落盘素材的位置。 */
    private String filePath;

    /** fileExtension（文件扩展名）用于帮助 OCR 实现选择解析策略。 */
    private String fileExtension;

    /** sourceType（来源类型）例如 KNOWLEDGE_DOCUMENT、IMAGE 或 VIDEO_FRAME。 */
    private String sourceType;

    /** reason（触发原因）说明为什么本次需要调用 OCR。 */
    private String reason;
}
