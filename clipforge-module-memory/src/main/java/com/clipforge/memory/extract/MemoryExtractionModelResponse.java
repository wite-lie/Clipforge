package com.clipforge.memory.extract;

import com.clipforge.ai.api.chat.ChatModelResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MemoryExtractionModelResponse（记忆抽取模型响应）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryExtractionModelResponse {

    /** memories（抽取结果）表示模型补充出的长期记忆候选。 */
    @Builder.Default
    private List<ExtractedUserPreferenceMemory> memories = new ArrayList<>();

    /** modelResponse（模型响应）表示本次 LLM 调用的原始元信息。 */
    private ChatModelResponse modelResponse;

    /** rawContent（原始内容）表示模型返回的原始文本。 */
    private String rawContent;
}
