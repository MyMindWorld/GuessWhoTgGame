package ru.gromov.guessWhoTgBot.config

import com.zaxxer.hikari.HikariDataSource
import lombok.extern.slf4j.Slf4j
import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.*
import javax.sql.DataSource

@Slf4j
@Profile("dbtest")
@Configuration
@EnableJpaRepositories("ru.gromov.guessWhoTgBot.db.repository")
@EnableTransactionManagement
open class DatabaseConf {
    @Bean
    @Primary
    open fun dataSource(): DataSource? {
        val containersInitializer = TestContainerConfig().getInstance()!!
        val dataSource = HikariDataSource()
        dataSource.driverClassName = "org.postgresql.Driver"
        dataSource.username = containersInitializer.username
        dataSource.password = containersInitializer.password
        dataSource.jdbcUrl = containersInitializer.url
        dataSource.isAutoCommit = false
        dataSource.maximumPoolSize = 1
        dataSource.connectionTimeout = 1000
        dataSource.leakDetectionThreshold = 1000
        return dataSource
    }

    @Bean
    open fun entityManagerFactory(dataSource: DataSource?): LocalContainerEntityManagerFactoryBean? {
        val factoryBean = LocalContainerEntityManagerFactoryBean()
        factoryBean.jpaVendorAdapter = HibernateJpaVendorAdapter()
        factoryBean.dataSource = dataSource!!
        factoryBean.setPersistenceProviderClass(HibernatePersistenceProvider::class.java)
        factoryBean.setPackagesToScan("ru.gromov.guessWhoTgBot")
        factoryBean.persistenceUnitName = "guessWhoTgBot"
        factoryBean.setJpaProperties(jpaHibernateProperties()!!)
        factoryBean.afterPropertiesSet()
        return factoryBean
    }

    protected open fun jpaHibernateProperties(): Properties? {
        val properties = Properties()
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop")
        properties.setProperty("hibernate.show_sql", "true")
        properties.setProperty("hibernate.generate_statistics", "false")
        properties.setProperty("hibernate.type", "info")
        properties.setProperty("hibernate.format_sql", "false")
        properties.setProperty("hibernate.use_sql_comments", "false")
        properties.setProperty("hibernate.jdbc.batch_size", "20")
        properties.setProperty("hibernate.order_inserts", "true")
        properties.setProperty("hibernate.order_updates", "true")
        properties.setProperty("hibernate.jdbc.batch_versioned_data", "true")
        properties.setProperty("hibernate.jdbc.lob.non_contextual_creation", "true")
        return properties
    }
}
