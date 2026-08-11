package com.clipforge.tool.definition;

/**
 * ToolEffectType（工具副作用类型）。
 *
 * <p>该枚举用于区分工具是否只读、是否会消耗模型成本、是否会修改系统状态，后续决定能不能暴露给 LLM（大语言模型）或 MCP（模型上下文协议）。
 */
public enum ToolEffectType {

    /** READ_ONLY（只读）表示工具只查询信息，不修改状态，通常可以优先考虑暴露给 LLM。 */
    READ_ONLY,

    /** COSTLY（高成本）表示工具可能调用模型、消耗 Token（词元）或耗时较长。 */
    COSTLY,

    /** MUTATING（会修改状态）表示工具会写数据库、发消息或触发异步任务。 */
    MUTATING,

    /** INTERNAL（内部工具）表示工具只供 Workflow（工作流）内部使用，不暴露给 LLM。 */
    INTERNAL
}
