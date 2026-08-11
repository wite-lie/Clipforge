package com.clipforge.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeBaseQueryCondition（知识库查询条件）。
 *
 * <p>该对象用于在应用层和仓储层之间传递知识库列表筛选条件，避免 Controller（控制器）直接接触数据库查询细节。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseQueryCondition {

    /** userId（用户编号）用于筛选某个用户拥有的知识库。 */
    private String userId;

    /** category（分类）用于筛选平台规则、动物科普、案例库等知识库。 */
    private String category;

    /** defaultPlatform（默认平台）用于筛选某个平台适配的知识库。 */
    private String defaultPlatform;

    /** status（状态）用于筛选 ACTIVE（启用中）或 ARCHIVED（已归档）的知识库。 */
    private String status;

    /** enabled（是否启用）用于筛选启用或禁用的知识库。 */
    private Integer enabled;
}
