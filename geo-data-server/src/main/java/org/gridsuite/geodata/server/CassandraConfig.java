/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.datastax.driver.core.*;
import com.powsybl.commons.PowsyblException;
import org.gridsuite.geodata.extensions.Coordinate;
import org.gridsuite.geodata.server.repositories.LineRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.nio.ByteBuffer;

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
    public CassandraClusterFactoryBean cluster(Environment env) {
        CassandraClusterFactoryBean clusterFactory = new CassandraClusterFactoryBean();
        clusterFactory.setContactPoints(env.getRequiredProperty("cassandra.contact-points"));
        clusterFactory.setPort(Integer.parseInt(env.getRequiredProperty("cassandra.port")));

        CodecRegistry codecRegistry = new CodecRegistry();
        clusterFactory.setClusterBuilderConfigurer(builder -> {
            builder.withCodecRegistry(codecRegistry);
            Cluster cluster = builder.build();
            KeyspaceMetadata keyspace = cluster.getMetadata().getKeyspace(getKeyspaceName());
            if (keyspace == null) {
                throw new PowsyblException("Keyspace '" + getKeyspaceName() + "' not found");
            }
            var coordinateType = keyspace.getUserType("coordinate");
            TypeCodec<UDTValue> coordinateTypeCodec = codecRegistry.codecFor(coordinateType);
            var coordinateCodec = new CoordinateCodec(coordinateTypeCodec, Coordinate.class);
            codecRegistry.register(coordinateCodec);
            return builder;
        });
        return clusterFactory;
    }

    @Bean
    public CassandraMappingContext cassandraMapping(Cluster cluster, Environment env) {
        CassandraMappingContext mappingContext =  new CassandraMappingContext();
        mappingContext.setUserTypeResolver(new SimpleUserTypeResolver(cluster, getKeyspaceName()));
        return mappingContext;
    }

    static class CoordinateCodec extends TypeCodec<Coordinate> {

        private final TypeCodec<UDTValue> innerCodec;

        private final UserType userType;

        public CoordinateCodec(TypeCodec<UDTValue> innerCodec, Class<Coordinate> javaType) {
            super(innerCodec.getCqlType(), javaType);
            this.innerCodec = innerCodec;
            this.userType = (UserType) innerCodec.getCqlType();
        }

        @Override
        public ByteBuffer serialize(Coordinate value, ProtocolVersion protocolVersion) {
            return innerCodec.serialize(toUDTValue(value), protocolVersion);
        }

        @Override
        public Coordinate deserialize(ByteBuffer bytes, ProtocolVersion protocolVersion) {
            return toCoordinate(innerCodec.deserialize(bytes, protocolVersion));
        }

        @Override
        public Coordinate parse(String value) {
            return value == null || value.isEmpty()  ? null : toCoordinate(innerCodec.parse(value));
        }

        @Override
        public String format(Coordinate value) {
            return value == null ? null : innerCodec.format(toUDTValue(value));
        }

        protected Coordinate toCoordinate(UDTValue value) {
            return value == null ? null : new Coordinate(
                    value.getDouble("lat"),
                    value.getDouble("lon")
            );
        }

        protected UDTValue toUDTValue(Coordinate value) {
            return value == null ? null : userType.newValue()
                    .setDouble("lat", value.getLat())
                    .setDouble("lon", value.getLon());
        }
    }
}
