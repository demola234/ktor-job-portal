package com.example.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlinx.coroutines.Dispatchers

object DatabaseFactory {

    fun init(config: ApplicationConfig) {
        val jdbcUrl  = config.property("database.url").getString()
        val user     = config.property("database.user").getString()
        val password = config.property("database.password").getString()

        runMigrations(jdbcUrl, user, password)

        val hikari = buildHikariDataSource(jdbcUrl, user, password)
        Database.connect(hikari)
    }

    private fun runMigrations(url: String, user: String, password: String) {
        Flyway.configure()
            .dataSource(url, user, password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }

    private fun buildHikariDataSource(url: String, user: String, password: String): HikariDataSource {
        val config = HikariConfig().apply {
            jdbcUrl         = url
            username        = user
            this.password   = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle     = 2
            idleTimeout     = 600_000
            connectionTimeout = 30_000
            maxLifetime     = 1_800_000
            isAutoCommit    = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }
}

// Convenience suspend wrapper — runs a DB block on the IO dispatcher inside a transaction
suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }