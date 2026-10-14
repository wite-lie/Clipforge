package com.clipforge.prompt.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PromptSection（提示词片段）。
 *
 * <p>该对象表示一次模型调用前的某段 Prompt（提示词）来源，既可以是最终真实发送的 system/user 消息，
 * 也可以是用于诊断的 RAG（检索增强生成）、Memory（记忆）、图片摘要等来源片段。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptSection {

    /** type（类型）表示提示词片段来源。 */
    private PromptSectionType type;

    /** name（名称）表示人类可读的片段名称。 */
    private String name;

    /** content（内容）表示需要估算 Token（词元）的文本。 */
    private String content;

    /** includedInTotal（是否计入总数）表示该片段是否属于真实发送给模型的最终消息。 */
    private boolean includedInTotal;
}
