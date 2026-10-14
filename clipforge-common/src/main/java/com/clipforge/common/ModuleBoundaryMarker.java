package com.clipforge.common;

/**
 * ModuleBoundaryMarker（模块边界标记）。
 *
 * <p>该类用于让 common（通用模块）在第一阶段具备独立可构建的 Java（爪哇语言）源码边界。
 * 后续现有 common 能力迁入本模块后会删除该标记类。
 */
public final class ModuleBoundaryMarker {

    private ModuleBoundaryMarker() {
    }
}
