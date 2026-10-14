package com.clipforge.memory.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * MemoryMybatisDatabaseConfig（记忆模块 MyBatis 数据库配置）。
 *
 * <p>该配置只在 db Profile（数据库配置环境）下扫描记忆模块承载的 Mapper（映射器），包括用户偏好、项目记忆、创作历史、会话滚动摘要和会话消息读取支撑。
 */
@Configuration
@Profile("db")
@MapperScan("com.clipforge.memory.infrastructure.persistence.mapper")
public class MemoryMybatisDatabaseConfig {
}
