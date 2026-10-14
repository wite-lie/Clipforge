package com.clipforge.contract.knowledge;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RecoverStuckKnowledgeDocumentResponse（恢复卡住知识文档响应）。
 *
 * <p>该对象用于返回一次扫描中被识别和标记的卡住文档处理任务。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoverStuckKnowledgeDocumentResponse {

    /** timeoutMinutes（超时分钟数）表示本次扫描使用的超时阈值。 */
    private Long timeoutMinutes;

    /** scannedCount（扫描命中数量）表示本次查询到的卡住运行中日志数量。 */
    private Integer scannedCount;

    /** recoveredCount（恢复数量）表示本次成功标记失败的任务数量。 */
    private Integer recoveredCount;

    /** recoveredDocumentIds（恢复文档编号列表）表示被标记为失败的知识文档。 */
    private List<String> recoveredDocumentIds;
}
