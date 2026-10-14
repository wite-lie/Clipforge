package com.clipforge.memory.summary.conversation;

import com.clipforge.ai.api.chat.ChatModelResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ConversationSummaryGenerateResult（会话摘要生成结果）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryGenerateResult {

    /** summaryContent（摘要内容）表示 LLM（大语言模型）生成的滚动摘要正文。 */
    private String summaryContent;

    /** modelResponse（模型响应）表示本次摘要调用的模型元信息。 */
    private ChatModelResponse modelResponse;
}
