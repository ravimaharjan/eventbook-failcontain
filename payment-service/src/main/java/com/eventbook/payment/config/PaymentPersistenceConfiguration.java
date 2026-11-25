package com.eventbook.payment.config;

import com.eventbook.payment.domain.Payment;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class PaymentPersistenceConfiguration {

    @Bean("chargeDataSource")
    @ConfigurationProperties("app.datasource.charge-pool")
    HikariDataSource chargeDataSource() {
        return new HikariDataSource();
    }

    @Bean("queryDataSource")
    @ConfigurationProperties("app.datasource.query-pool")
    HikariDataSource queryDataSource() {
        return new HikariDataSource();
    }

    @Bean("chargeEntityManagerFactory")
    LocalContainerEntityManagerFactoryBean chargeEntityManagerFactory(
            @Qualifier("chargeDataSource") DataSource dataSource) {
        return entityManagerFactory(dataSource, "charge-pool", "update");
    }

    @Bean("queryEntityManagerFactory")
    @DependsOn("chargeEntityManagerFactory")
    LocalContainerEntityManagerFactoryBean queryEntityManagerFactory(
            @Qualifier("queryDataSource") DataSource dataSource) {
        return entityManagerFactory(dataSource, "query-pool", "validate");
    }

    @Bean("chargeTransactionManager")
    PlatformTransactionManager chargeTransactionManager(
            @Qualifier("chargeEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean("queryTransactionManager")
    PlatformTransactionManager queryTransactionManager(
            @Qualifier("queryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Configuration
    @EnableJpaRepositories(
            basePackages = "com.eventbook.payment.repository.charge",
            entityManagerFactoryRef = "chargeEntityManagerFactory",
            transactionManagerRef = "chargeTransactionManager")
    static class ChargeRepositories {
    }

    @Configuration
    @EnableJpaRepositories(
            basePackages = "com.eventbook.payment.repository.query",
            entityManagerFactoryRef = "queryEntityManagerFactory",
            transactionManagerRef = "queryTransactionManager")
    static class QueryRepositories {
    }

    private LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource, String persistenceUnit, String ddlMode) {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan(Payment.class.getPackageName());
        factory.setPersistenceUnitName(persistenceUnit);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", ddlMode,
                "hibernate.jdbc.time_zone", "UTC"));
        return factory;
    }
}
