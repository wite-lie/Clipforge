package com.clipforge.contract.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ProjectMemoryResponse（项目记忆响应）。
 *
 * <p>该对象用于接口返回项目级长期画像。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemoryResponse {

    /** memoryId（记忆编号）表示对外使用的记忆唯一编号。 */
    private String memoryId;

    /** projectId（项目编号）表示该记忆归属项目。 */
    private String projectId;

    /** userId（用户编号）表示该项目记忆归属用户。 */
    private String userId;

    /** summary（摘要）表示项目长期定位。 */
    private String summary;

    /** targetPlatform（目标平台）表示项目常用平台。 */
    private String targetPlatform;

    /** targetAudience（目标受众）表示项目长期面向的人群。 */
    private String targetAudience;

    /** contentStyle（内容风格）表示项目稳定内容调性。 */
    private String contentStyle;

    /** constraintsJson（约束 JSON）表示项目级额外约束。 */
    private String constraintsJson;

    /** status（状态）表示项目记忆是否启用。 */
    private String status;
}
