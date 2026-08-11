package com.clipforge.memory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * MemoryReadStats（记忆读取统计）。
 *
 * <p>该对象记录本次读取命中了多少 Memory（记忆），方便后续写入 Trace（执行链路追踪）。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryReadStats {

    /** userPreferenceCount（用户偏好数量）表示本次注入的用户偏好条数。 */
    private int userPreferenceCount;

    /** projectMemoryHit（是否命中项目记忆）表示是否读取到项目画像。 */
    private boolean projectMemoryHit;

    /** historyMemoryCount（历史记忆数量）表示本次注入的历史经验条数。 */
    private int historyMemoryCount;

    /** conversationSummaryHit（是否命中会话摘要）表示是否读取到滚动摘要。 */
    private boolean conversationSummaryHit;

    /** userPreferenceLimit（用户偏好上限）表示读取器应用的偏好裁剪上限。 */
    private int userPreferenceLimit;

    /** historyMemoryLimit（历史记忆上限）表示读取器应用的历史摘要裁剪上限。 */
    private int historyMemoryLimit;
}
