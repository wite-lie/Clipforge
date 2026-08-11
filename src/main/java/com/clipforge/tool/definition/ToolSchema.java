package com.clipforge.tool.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ToolSchema（工具结构描述）。
 *
 * <p>该对象用轻量方式描述工具输入和输出结构，第一阶段不引入完整 JSON Schema（对象表示结构模式），避免为了工具元数据引入过重复杂度。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolSchema {

    /** javaType（Java 类型）表示输入或输出对象的类名。 */
    private String javaType;

    /** description（描述）说明该结构承载哪些业务字段。 */
    private String description;

    /** example（示例）用于文档、Trace（执行链路追踪）和后续工具适配参考。 */
    private String example;

    /**
     * of（构造结构描述）用于快速创建 ToolSchema（工具结构描述）。
     *
     * @param javaType Java 类型
     * @param description 描述
     * @return 工具结构描述
     */
    public static ToolSchema of(String javaType, String description) {
        return ToolSchema.builder()
                .javaType(javaType)
                .description(description)
                .build();
    }
}
