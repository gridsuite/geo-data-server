/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geo.data.server;

import com.datastax.driver.core.*;
import com.powsybl.geo.data.extensions.Coordinate;
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
@EnableCassandraRepositories(basePackages = "com.powsybl.geo.data.server.repositories")
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Override
    protected String getKeyspaceName() {
        return CassandraConstants.KEYSPACE_GEO_DATA;
    }

    @Bean
    public CassandraClusterFactoryBean cluster(Environment env) {

        CassandraClusterFactoryBean cluster = new CassandraClusterFactoryBean();
        cluster.setContactPoints(env.getRequiredProperty("cassandra.contact-points"));
        cluster.setPort(Integer.parseInt(env.getRequiredProperty("cassandra.port")));

        CodecRegistry codecRegistry = new CodecRegistry();
        cluster.setClusterBuilderConfigurer(builder -> {
            builder.withCodecRegistry(codecRegistry);
            Cluster cluster1 = builder.build();

            UserType coordinateType = cluster1.getMetadata().getKeyspace(CassandraConstants.KEYSPACE_GEO_DATA).getUserType("coordinate");
            TypeCodec<UDTValue> coordinateTypeCodec = codecRegistry.codecFor(coordinateType);
            CoordinateCodec coordinateCodec = new CoordinateCodec(coordinateTypeCodec, Coordinate.class);
            codecRegistry.register(coordinateCodec);
            return builder;
        });
        return cluster;
    }

    @Bean
    public CassandraMappingContext cassandraMapping(Cluster cluster, Environment env) {
        CassandraMappingContext mappingContext =  new CassandraMappingContext();
        mappingContext.setUserTypeResolver(new SimpleUserTypeResolver(cluster, CassandraConstants.KEYSPACE_GEO_DATA));
        return mappingContext;
    }

    private static class CoordinateCodec extends TypeCodec<Coordinate> {

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
