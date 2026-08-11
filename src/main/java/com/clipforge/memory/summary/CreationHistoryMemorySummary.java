package com.clipforge.memory.summary;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationHistoryMemorySummary（创作历史记忆摘要）。
 *
 * <p>该对象承载从高分创作方案中沉淀出的摘要和标签。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationHistoryMemorySummary {

    /** summary（摘要）表示可复用的创作经验。 */
    private String summary;

    /** tags（标签）表示主题、平台或内容特征。 */
    private List<String> tags;
}
