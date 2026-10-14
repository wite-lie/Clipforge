package com.clipforge.material.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * MaterialMybatisDatabaseConfig（素材 MyBatis 数据库配置）。
 *
 * <p>该配置类只在 db（数据库）Profile（配置环境）下启用，用于扫描 Material 模块自己的 Mapper（映射器）接口。
 */
@Configuration
@Profile("db")
@MapperScan({
        "com.clipforge.material.infrastructure.persistence.mapper"
})
public class MaterialMybatisDatabaseConfig {
}
