package com.exadel.frs.core.trainservice;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringResourceAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class TestLiquibase {
    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    private Environment env;

    @Autowired
    DataSource dataSource;

    @PostConstruct
    public void initDatabase() {
        try {
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.yaml",
                    new SpringResourceAccessor(resourceLoader),
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(dataSource.getConnection()))
            );
            setLiquibaseChangeLogParams(liquibase);
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (Exception e) {
            //manage exception
            e.printStackTrace();
        }
    }

    private void setLiquibaseChangeLogParams(final Liquibase liquibase) {
        String clientId = env.getProperty("spring.liquibase.parameters.common-client.client-id", "CommonClientId");
        String accessTokenValidity = env.getProperty("spring.liquibase.parameters.common-client.access-token-validity", "2400");
        String refreshTokenValidity = env.getProperty("spring.liquibase.parameters.common-client.refresh-token-validity", "1209600");
        String authorizedGrantTypes = env.getProperty("spring.liquibase.parameters.common-client.authorized-grant-types", "password,refresh_token");

        liquibase.setChangeLogParameter("common-client.client-id", clientId);
        liquibase.setChangeLogParameter("common-client.access-token-validity", accessTokenValidity);
        liquibase.setChangeLogParameter("common-client.refresh-token-validity", refreshTokenValidity);
        liquibase.setChangeLogParameter("common-client.authorized-grant-types", authorizedGrantTypes);
    }
}
