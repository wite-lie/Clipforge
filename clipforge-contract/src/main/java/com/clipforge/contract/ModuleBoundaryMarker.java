package com.clipforge.contract;

/**
 * ModuleBoundaryMarker（模块边界标记）。
 *
 * <p>该类用于让 contract（契约模块）在第一阶段具备独立可构建的 Java（爪哇语言）源码边界。
 * 后续跨模块命令、事件和查询对象会迁入本模块。
 */
public final class ModuleBoundaryMarker {

    private ModuleBoundaryMarker() {
    }
}
