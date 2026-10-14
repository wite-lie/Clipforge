package com.clipforge.contract.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DisableMemoryResponse（停用记忆响应）。
 *
 * <p>该对象用于返回停用记忆的结果，支持重复停用时保持幂等。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisableMemoryResponse {

    /** memoryId（记忆编号）表示被处理的记忆编号。 */
    private String memoryId;

    /** memoryType（记忆类别）表示用户偏好、项目记忆或创作历史记忆。 */
    private String memoryType;

    /** status（状态）表示处理后的记忆状态。 */
    private String status;

    /** updated（是否更新）表示本次调用是否真正改变了数据库状态。 */
    private boolean updated;
}
