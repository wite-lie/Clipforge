package com.clipforge.memory.extract;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MemoryExtractionProperties（记忆抽取配置属性）。
 *
 * <p>该配置用于控制规则抽取与 LLM（大语言模型）抽取的切换、触发场景、Prompt（提示词）路径和输出约束。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.memory.extraction")
public class MemoryExtractionProperties {

    /** enabled（是否启用）表示是否开启记忆抽取总开关。 */
    private boolean enabled = true;

    /** llmEnabled（是否启用 LLM）表示是否允许在高价值场景下调用模型补充抽取。 */
    private boolean llmEnabled = false;

    /** llmTriggerTypes（LLM 触发类型）表示哪些场景允许调用 LLM 抽取。 */
    private List<MemoryExtractionTriggerType> llmTriggerTypes = new ArrayList<>(List.of(
            MemoryExtractionTriggerType.PROJECT_INIT,
            MemoryExtractionTriggerType.CONVERSATION_END,
            MemoryExtractionTriggerType.MANUAL_CORRECTION
    ));

    /** minPersistConfidence（最小落库置信度）表示低于该阈值的候选项不自动写入。 */
    private BigDecimal minPersistConfidence = new BigDecimal("0.7000");

    /** maxCandidateCount（最大候选数）表示单次抽取最多允许写入的记忆条数。 */
    private int maxCandidateCount = 5;

    /** promptVersion（提示词版本）用于记录当前抽取模板版本。 */
    private String promptVersion = "memory-extraction-v1";

    /** systemPath（系统提示词路径）定义抽取器角色与边界。 */
    private String systemPath = "classpath:prompts/记忆抽取-系统提示词.md";

    /** userPath（用户提示词路径）定义抽取任务的动态输入格式。 */
    private String userPath = "classpath:prompts/记忆抽取-用户提示词.md";

    /** temperature（温度）控制抽取稳定性。 */
    private double temperature = 0.1;

    /** maxTokens（最大输出词元）控制抽取结果长度。 */
    private Integer maxTokens = 800;

    /** logPrompt（是否打印 Prompt）仅用于本地调试。 */
    private boolean logPrompt = false;
}
