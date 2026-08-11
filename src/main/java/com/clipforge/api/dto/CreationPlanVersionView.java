package com.clipforge.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationPlanVersionView（创作方案版本视图）。
 *
 * <p>该对象用于在质量复盘接口中展示一次运行内的方案版本链。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationPlanVersionView {

    /** versionId（版本编号）表示方案版本业务编号。 */
    private String versionId;

    /** versionNo（版本序号）表示该运行下第几个方案版本。 */
    private Integer versionNo;

    /** versionType（版本类型）表示 INITIAL（初始生成）或 REVISED（修正版）。 */
    private String versionType;

    /** sourceVersionId（来源版本编号）表示修正版来源于哪个版本。 */
    private String sourceVersionId;

    /** score（评分）表示该版本综合评分。 */
    private Integer score;

    /** createdByPurpose（创建用途）表示创建该版本的模型调用用途。 */
    private String createdByPurpose;

    /** modelCallRecordId（模型调用记录编号）表示创建该版本关联的模型调用。 */
    private String modelCallRecordId;

    /** finalVersion（是否最终版本）表示本次运行最终采用该版本。 */
    private Boolean finalVersion;

    /** planJson（方案 JSON）保存完整创作方案。 */
    private String planJson;

    /** createTime（创建时间）表示版本创建时间。 */
    private LocalDateTime createTime;
}
