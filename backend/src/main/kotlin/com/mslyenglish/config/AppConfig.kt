package com.mslyenglish.config

import io.ktor.server.config.*

data class JwtConfig(
    val issuer: String,
    val audience: String,
    val secret: String,
    val expiresInMs: Long
)

data class StorageConfig(val uploadDir: String)

data class DefaultAdminConfig(val username: String, val password: String)

data class DatabaseConfig(
    val jdbcUrl: String,
    val driverClassName: String,
    val username: String,
    val password: String,
    val maximumPoolSize: Int
)

data class AppConfig(
    val jwt: JwtConfig,
    val storage: StorageConfig,
    val defaultAdmin: DefaultAdminConfig,
    val db: DatabaseConfig
)

fun ApplicationConfig.toAppConfig(): AppConfig = AppConfig(
    jwt = JwtConfig(
        issuer = property("app.jwt.issuer").getString(),
        audience = property("app.jwt.audience").getString(),
        secret = property("app.jwt.secret").getString(),
        expiresInMs = property("app.jwt.expiresInMs").getString().toLong()
    ),
    storage = StorageConfig(property("app.storage.uploadDir").getString()),
    defaultAdmin = DefaultAdminConfig(
        username = property("app.admin.defaultUsername").getString(),
        password = property("app.admin.defaultPassword").getString()
    ),
    db = DatabaseConfig(
        jdbcUrl = property("database.jdbcUrl").getString(),
        driverClassName = property("database.driverClassName").getString(),
        username = property("database.username").getString(),
        password = property("database.password").getString(),
        maximumPoolSize = property("database.maximumPoolSize").getString().toInt()
    )
)
