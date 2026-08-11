package com.clipforge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ProjectResponse（项目响应）。
 *
 * <p>该对象用于接口返回 Project（项目）基础信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    /** projectId（项目编号）表示对外使用的项目唯一编号。 */
    private String projectId;

    /** userId（用户编号）表示项目所属用户。 */
    private String userId;

    /** name（项目名称）表示项目的人类可读名称。 */
    private String name;

    /** description（项目描述）用于说明项目目标和背景。 */
    private String description;
}
