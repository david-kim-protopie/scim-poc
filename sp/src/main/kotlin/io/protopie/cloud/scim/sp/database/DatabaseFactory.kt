package io.protopie.cloud.scim.sp.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * 데이터베이스 연결 및 초기화를 담당하는 객체
 */
object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * 데이터베이스 초기화
     */
    fun init() {
        logger.info("초기화 - SCIM 데이터베이스")

        val config =
            HikariConfig().apply {
                driverClassName = "org.h2.Driver"
                jdbcUrl = "jdbc:h2:mem:scim;DB_CLOSE_DELAY=-1"
                username = "sa"
                password = ""
                maximumPoolSize = 10
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        // 테이블 생성
        transaction {
            SchemaUtils.create(Users, Groups)
            logger.info("테이블 생성 완료")
        }
    }

    /**
     * 데이터베이스 초기화 (테스트용)
     */
    fun initForTesting() {
        logger.info("초기화 - 테스트용 SCIM 데이터베이스")

        val config =
            HikariConfig().apply {
                driverClassName = "org.h2.Driver"
                jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
                username = "sa"
                password = ""
                maximumPoolSize = 3
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        // 테이블 생성
        transaction {
            SchemaUtils.create(Users, Groups)
            logger.info("테스트용 테이블 생성 완료")
        }
    }

    /**
     * 데이터베이스 리셋 (테스트용)
     */
    fun reset() {
        transaction {
            SchemaUtils.drop(Users, Groups)
            SchemaUtils.create(Users, Groups)
            logger.info("데이터베이스 리셋 완료")
        }
    }
}
