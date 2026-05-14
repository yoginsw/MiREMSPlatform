package io.mirems.core.infra.persistence;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Enables MiREMS JPA repositories only when an application provides a DataSource. */
@Configuration
@ConditionalOnBean(DataSource.class)
@EntityScan(basePackages = "io.mirems.core.domain")
@EnableJpaRepositories(basePackages = "io.mirems.core.infra.persistence")
public class MiremsPersistenceConfiguration {
}
