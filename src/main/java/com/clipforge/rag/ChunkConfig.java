package com.clipforge.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ChunkConfig（切片配置）。
 *
 * <p>该对象承载用户通过 chunkConfigJson（切片配置 JSON）设置的切片参数。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkConfig {

    /** maxSegmentSize（最大切片大小）表示每个切片的最大字符数。 */
    private Integer maxSegmentSize;

    /** maxOverlapSize（最大重叠大小）表示相邻切片之间的重叠字符数。 */
    private Integer maxOverlapSize;

    /** chunkSize（切片大小）兼容早期配置字段，语义等同于 maxSegmentSize。 */
    private Integer chunkSize;

    /** overlap（重叠大小）兼容早期配置字段，语义等同于 maxOverlapSize。 */
    private Integer overlap;

    /** regex（正则表达式）用于 REGEX（正则）切片策略。 */
    private String regex;
}
