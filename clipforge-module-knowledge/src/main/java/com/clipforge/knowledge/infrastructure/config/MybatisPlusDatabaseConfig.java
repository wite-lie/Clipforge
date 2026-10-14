package com.clipforge.knowledge.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * MybatisPlusDatabaseConfig（MyBatis-Plus 数据库配置）。
 *
 * <p>该配置类只在 db（数据库）Profile（配置环境）下启用，用于扫描 Mapper（映射器）接口。
 */
@Configuration
@Profile("db")
@MapperScan("com.clipforge.knowledge.infrastructure.persistence.mapper")
public class MybatisPlusDatabaseConfig {
}
