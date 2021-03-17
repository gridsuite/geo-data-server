/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.datastax.oss.driver.api.core.type.codec.MappingCodec;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import com.datastax.oss.driver.api.core.type.codec.registry.MutableCodecRegistry;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.datastax.oss.driver.internal.core.type.codec.registry.DefaultCodecRegistry;
import org.gridsuite.geodata.extensions.Coordinate;
import org.gridsuite.geodata.server.repositories.LineRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.config.SessionFactoryFactoryBean;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Configuration
@PropertySource(value = {"classpath:cassandra.properties"})
@PropertySource(value = {"file:/config/cassandra.properties"}, ignoreResourceNotFound = true)
@EnableCassandraRepositories(basePackageClasses = LineRepository.class)
public class CassandraConfig {

    @Bean
    public CqlSessionFactoryBean session(Environment env) {

        CqlSessionFactoryBean session = new CqlSessionFactoryBean();
        session.setContactPoints(env.getRequiredProperty("cassandra.contact-points"));
        session.setPort(Integer.parseInt(env.getRequiredProperty("cassandra.port")));
        session.setLocalDatacenter("datacenter1");
        session.setKeyspaceName(CassandraConstants.KEYSPACE_GEO_DATA);
        return session;
    }

    @Bean
    public CassandraMappingContext mappingContext() {
        return new CassandraMappingContext();
    }

    @Bean
    public CassandraConverter converter(CassandraMappingContext mappingContext) {
        MappingCassandraConverter mappingCassandraConverter = new MappingCassandraConverter(mappingContext);
        CodecRegistry codecRegistry = new DefaultCodecRegistry("");
        mappingCassandraConverter.setCodecRegistry(codecRegistry);
        return mappingCassandraConverter;
    }

    @Bean
    public SessionFactoryFactoryBean sessionFactory(CqlSession session, CassandraConverter converter) {
        SessionFactoryFactoryBean sessionFactory = new SessionFactoryFactoryBean();
        sessionFactory.setSession(session);
        sessionFactory.setConverter(converter);

        CodecRegistry codecRegistry = session.getContext().getCodecRegistry();

        UserDefinedType coordinateUdt =
                session
                        .getMetadata()
                        .getKeyspace(CassandraConstants.KEYSPACE_GEO_DATA)
                        .flatMap(ks -> ks.getUserDefinedType("coordinate"))
                        .orElseThrow(IllegalStateException::new);
        // The "inner" codec that handles the conversions from CQL from/to UdtValue
        TypeCodec<UdtValue> innerCodec = codecRegistry.codecFor(coordinateUdt);
        // The mapping codec that will handle the conversions from/to UdtValue and Coordinates
        CoordinateCodec coordinateCodec = new CoordinateCodec(innerCodec);
        ((MutableCodecRegistry) codecRegistry).register(coordinateCodec);

        return sessionFactory;
    }

    @Bean
    public CassandraAdminTemplate cassandraTemplate(CqlSession session, CassandraConverter converter) {
        return new CassandraAdminTemplate(session, converter);
    }

    static class CoordinateCodec extends MappingCodec<UdtValue, Coordinate> {

        public CoordinateCodec(TypeCodec<UdtValue> innerCodec) {
            super(innerCodec, GenericType.of(Coordinate.class));
        }

        @Override
        public UserDefinedType getCqlType() {
            return (UserDefinedType) super.getCqlType();
        }

        @Override
        protected Coordinate innerToOuter(UdtValue value) {
            return value == null ? null : new Coordinate(
                    value.getDouble("lat"),
                    value.getDouble("lon")
            );
        }

        @Override
        protected UdtValue outerToInner(Coordinate value) {
            return value == null ? null : getCqlType().newValue()
                    .setDouble("lat", value.getLat())
                    .setDouble("lon", value.getLon());
        }
    }
}
