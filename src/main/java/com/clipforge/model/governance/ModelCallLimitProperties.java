package com.clipforge.model.governance;

import lombok.Getter;
import lombok.Setter;

/**
 * ModelCallLimitProperties（单类模型调用限流配置）用于描述一种模型调用类型的并发数和等待时间。
 */
@Getter
@Setter
public class ModelCallLimitProperties {

    /** maxConcurrent（最大并发数）表示同一 JVM（Java 虚拟机）内该类模型调用最多同时运行多少个。 */
    private Integer maxConcurrent;

    /** timeoutMillis（等待超时毫秒数）表示并发满时最多等待多久，超过后直接失败。 */
    private Long timeoutMillis;
}
