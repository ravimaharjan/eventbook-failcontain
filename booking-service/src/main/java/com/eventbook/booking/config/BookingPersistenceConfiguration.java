package com.eventbook.booking.config;

import com.eventbook.booking.domain.Booking;
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
public class BookingPersistenceConfiguration {

    @Bean("bookingWriteDataSource")
    @ConfigurationProperties("app.datasource.booking-write")
    HikariDataSource bookingWriteDataSource() {
        return new HikariDataSource();
    }

    @Bean("resultConsumerDataSource")
    @ConfigurationProperties("app.datasource.result-consumer")
    HikariDataSource resultConsumerDataSource() {
        return new HikariDataSource();
    }

    @Bean("bookingWriteEntityManagerFactory")
    LocalContainerEntityManagerFactoryBean bookingWriteEntityManagerFactory(
            @Qualifier("bookingWriteDataSource") DataSource dataSource) {
        return entityManagerFactory(dataSource, "booking-write", "update");
    }

    @Bean("resultConsumerEntityManagerFactory")
    @DependsOn("bookingWriteEntityManagerFactory")
    LocalContainerEntityManagerFactoryBean resultConsumerEntityManagerFactory(
            @Qualifier("resultConsumerDataSource") DataSource dataSource) {
        return entityManagerFactory(dataSource, "result-consumer", "validate");
    }

    @Bean("bookingWriteTransactionManager")
    PlatformTransactionManager bookingWriteTransactionManager(
            @Qualifier("bookingWriteEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean("resultConsumerTransactionManager")
    PlatformTransactionManager resultConsumerTransactionManager(
            @Qualifier("resultConsumerEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Configuration
    @EnableJpaRepositories(
            basePackages = "com.eventbook.booking.repository.write",
            entityManagerFactoryRef = "bookingWriteEntityManagerFactory",
            transactionManagerRef = "bookingWriteTransactionManager")
    static class WriteRepositories {
    }

    @Configuration
    @EnableJpaRepositories(
            basePackages = "com.eventbook.booking.repository.result",
            entityManagerFactoryRef = "resultConsumerEntityManagerFactory",
            transactionManagerRef = "resultConsumerTransactionManager")
    static class ResultRepositories {
    }

    private LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource, String persistenceUnit, String ddlMode) {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan(Booking.class.getPackageName());
        factory.setPersistenceUnitName(persistenceUnit);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", ddlMode,
                "hibernate.jdbc.time_zone", "UTC"));
        return factory;
    }
}
