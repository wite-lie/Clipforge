package com.clipforge.material.sampling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MaterialSamplingDecision（素材抽样决策）用于记录单个素材在抽样策略中的选择结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialSamplingDecision {

    /** itemId（条目编号）表示图片素材编号或视频帧素材编号。 */
    private String itemId;

    /** decisionType（决策类型）表示该条目是否被选中。 */
    private MaterialSamplingDecisionType decisionType;

    /** originalIndex（原始顺序）表示该条目进入抽样器前的位置。 */
    private int originalIndex;

    /** reason（原因）用于说明选中或跳过的依据，方便后续观测和排查。 */
    private String reason;
}
