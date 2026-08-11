package com.clipforge.memory.extract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MemoryExtractionTriggerDecision（记忆抽取触发决策）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryExtractionTriggerDecision {

    /** shouldInvokeLlm（是否调用 LLM）表示本次是否允许启动模型补充抽取。 */
    private boolean shouldInvokeLlm;

    /** reason（原因）用于说明是否调用的判定结果。 */
    private String reason;
}
