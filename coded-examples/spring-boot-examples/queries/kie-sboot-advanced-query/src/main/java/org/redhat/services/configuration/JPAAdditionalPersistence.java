package org.redhat.services.configuration;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.managed.DataSourceXAConnectionFactory;
import org.apache.commons.dbcp2.managed.ManagedDataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.drools.persistence.jpa.marshaller.JPAPlaceholderResolverStrategy;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.naming.Context;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import java.util.Properties;

@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "auditEntityManager",
        transactionManagerRef = "transactionManager",
        basePackages = {"com.company.model", "org.drools.persistence.jpa.marshaller"})
@Profile("multi-db")
public class JPAAdditionalPersistence {

    // TODO: Value/Config Helper Class

    @Value("${kie.spring.secondary.datasource.url}")
    String jdbcUri;

    @Value("${kie.spring.secondary.datasource.pu-name:org.jbpm.audit.persistence.jpa}")
    String puName;

    @Value("${kie.spring.secondary.datasource.username:sa}")
    String username;

    @Value("${kie.spring.secondary.datasource.password:sa}")
    String password;

    @Value("${kie.spring.secondary.datasource.properties.hibernate.dialect}")
    String dialect;

    @Value("${kie.spring.secondary.datasource.properties.hibernate.ddl-auto:update}")
    String ddl;

    @Autowired
    private TransactionManager tm;

    /**
     * Use a customised persistece.xml from jbpm if you wish to import jbpm Entity classes i.e. auditing etc..
     */
    protected static final String PERSISTENCE_XML_LOCATION = "classpath:/META-INF/audit-persistence.xml";
    protected static final String AUDIT_PERSISTENCE_UNIT_NAME = "org.jbpm.audit.persistence.jpa";

    @Bean(name = "auditDataSource")
    public XADataSource h2DataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(jdbcUri);
        ds.setUser(username);
        ds.setPassword(password);
        return ds;
    }

    @Bean
    public DataSource auditDatasource() {
        DataSourceXAConnectionFactory dataSourceXAConnectionFactory = new DataSourceXAConnectionFactory(tm,
                h2DataSource());
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
                dataSourceXAConnectionFactory, null);
        GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
        poolableConnectionFactory.setPool(connectionPool);
        return new ManagedDataSource<>(connectionPool, dataSourceXAConnectionFactory.getTransactionRegistry());
    }

    @Bean(name = "auditJPAVendorAdapter")
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setGenerateDdl(true);
        jpaVendorAdapter.setShowSql(true);
        jpaVendorAdapter.setDatabasePlatform(dialect);
        return jpaVendorAdapter;
    }

    public Properties jpaAuditProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", dialect);
        properties.setProperty("hibernate.show_sql", "true");
        properties.setProperty("hibernate.max_fetch_depth", "3");
        properties.setProperty("hibernate.jdbc.fetch_size", "100");
        properties.setProperty("hibernate.ddl-auto", ddl);
        properties.setProperty("hibernate.id.new_generator_mappings", "false");
        return properties;
    }

    @Bean(name = "auditEntityManager")
    public LocalContainerEntityManagerFactoryBean auditEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setPersistenceUnitName(puName);
        factoryBean.setPackagesToScan("com.company.model", "org.drools.persistence.jpa.marshaller");
//		factoryBean.setPersistenceXmlLocation(PERSISTENCE_XML_LOCATION);
        factoryBean.setJtaDataSource(auditDatasource());
        factoryBean.setJpaVendorAdapter(jpaVendorAdapter());
        factoryBean.setJpaProperties(jpaAuditProperties());
        return factoryBean;
    }


    /**
     * This does nothing but used as example for engineering as possible RFE..
     * @param entityManagerFactory
     * @return
     */
    @Bean
    public JPAPlaceholderResolverStrategy jpaPlaceholderResolverStrategy(@Qualifier("auditEntityManager") EntityManagerFactory entityManagerFactory) {
        log.info(">>>> Bootstrapping Audit EMF with PU-Name :: {} ",
                entityManagerFactory.getProperties().get("hibernate.ejb.persistenceUnitName").toString());
        return new JPAPlaceholderResolverStrategy(entityManagerFactory);
    }

    /**
     * JDBC Querying the Spring Way..
     *
     * @param ds
     * @return
     */
    @Bean(name = "auditJDBCTemplate")
    public JdbcTemplate jdbcTemplate(@Qualifier("auditDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

}
