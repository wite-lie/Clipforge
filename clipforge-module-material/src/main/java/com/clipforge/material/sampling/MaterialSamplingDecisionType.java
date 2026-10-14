package com.clipforge.material.sampling;

/**
 * MaterialSamplingDecisionType（素材抽样决策类型）用于描述某个素材是否进入理解链路。
 */
public enum MaterialSamplingDecisionType {

    /** SELECTED（已选择）表示该素材会进入模型理解。 */
    SELECTED,

    /** SKIPPED_BY_SAMPLING（被抽样跳过）表示该素材因为数量控制没有进入模型理解。 */
    SKIPPED_BY_SAMPLING
}
