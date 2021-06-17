/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.datastax.oss.driver.api.core.CqlSession;
import org.gridsuite.geodata.server.repositories.LineRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.config.SessionFactoryFactoryBean;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Configuration
@PropertySource(value = {"classpath:cassandra.properties"})
@PropertySource(value = {"file:/config/cassandra.properties"}, ignoreResourceNotFound = true)
@EnableCassandraRepositories(basePackageClasses = LineRepository.class)
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${cassandra-keyspace:geo_data}")
    private String keyspaceName;

    @Override
    protected String getKeyspaceName() {
        return keyspaceName;
    }

    @Bean
    public CqlSessionFactoryBean cassandraSession(Environment env) {
        var session = new CqlSessionFactoryBean();
        session.setContactPoints(env.getRequiredProperty("cassandra.contact-points"));
        session.setPort(Integer.parseInt(env.getRequiredProperty("cassandra.port")));
        session.setLocalDatacenter("datacenter1");
        session.setKeyspaceName(getKeyspaceName());
        return session;
    }

    @Bean
    public SessionFactoryFactoryBean cassandraSessionFactory(CqlSession session, CassandraConverter converter) {
        var sessionFactory = new SessionFactoryFactoryBean();
        sessionFactory.setSession(session);
        sessionFactory.setConverter(converter);
        return sessionFactory;
    }
}
