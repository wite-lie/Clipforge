package com.clipforge.agent.intent;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ConversationIntentProperties（会话意图识别配置）。
 *
 * <p>该配置控制规则识别、LLM（大语言模型）分类、置信度阈值和 Prompt（提示词）文件路径。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.intent")
public class ConversationIntentProperties {

    /** enabled（是否启用）表示是否启用增强意图识别。 */
    private boolean enabled = true;

    /** llmEnabled（是否启用大模型分类）表示规则无法稳定识别时是否调用 LLM（大语言模型）。 */
    private boolean llmEnabled = true;

    /** ruleConfidenceThreshold（规则置信度阈值）表示规则结果达到该阈值时直接采用。 */
    private double ruleConfidenceThreshold = 0.80;

    /** llmConfidenceThreshold（模型置信度阈值）表示模型分类结果达到该阈值时才采用。 */
    private double llmConfidenceThreshold = 0.65;

    /** temperature（温度）表示意图分类模型的随机性，建议保持较低。 */
    private Double temperature = 0.0;

    /** maxTokens（最大输出词元）表示意图分类最多输出多少 Token（词元）。 */
    private Integer maxTokens = 500;

    /** maxRecentMessages（最近消息数量）表示最多向分类模型提供多少条短历史。 */
    private int maxRecentMessages = 6;

    /** systemPath（系统提示词路径）表示意图分类系统 Prompt（提示词）文件路径。 */
    private String systemPath = "classpath:prompts/对话意图-系统提示词.md";

    /** userPath（用户提示词路径）表示意图分类用户 Prompt（提示词）文件路径。 */
    private String userPath = "classpath:prompts/对话意图-用户提示词.md";
}
