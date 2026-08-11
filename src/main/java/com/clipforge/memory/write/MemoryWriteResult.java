package com.clipforge.memory.write;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MemoryWriteResult（记忆写入结果）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryWriteResult {

    /** userPreferenceWrittenCount（用户偏好写入数量）表示最终落库的用户偏好记忆总数。 */
    private int userPreferenceWrittenCount;

    /** rulePreferenceWrittenCount（规则偏好写入数量）表示规则抽取最终落库的数量。 */
    private int rulePreferenceWrittenCount;

    /** llmPreferenceWrittenCount（LLM 偏好写入数量）表示模型补充抽取最终落库的数量。 */
    private int llmPreferenceWrittenCount;

    /** historyMemoryWritten（是否写入历史记忆）表示是否沉淀高分创作经验。 */
    private boolean historyMemoryWritten;

    /** projectMemoryInitialized（是否初始化项目记忆）表示是否首次生成项目画像。 */
    private boolean projectMemoryInitialized;

    /** messages（处理消息）用于调试和排查记忆写入分支。 */
    @Builder.Default
    private List<String> messages = new ArrayList<>();

    /**
     * skipped（跳过结果）用于构造未执行写入的返回值。
     *
     * @param reason 跳过原因
     * @return 记忆写入结果
     */
    public static MemoryWriteResult skipped(String reason) {
        return MemoryWriteResult.builder().messages(List.of(reason)).build();
    }
}
