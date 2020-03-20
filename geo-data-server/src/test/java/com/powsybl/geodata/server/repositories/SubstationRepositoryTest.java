/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.repositories;

import com.github.nosan.embedded.cassandra.api.Cassandra;
import com.github.nosan.embedded.cassandra.spring.test.EmbeddedCassandra;
import com.powsybl.geodata.server.CassandraConfig;
import com.powsybl.geodata.server.EmbeddedCassandraFactoryConfig;
import com.powsybl.geodata.server.GeoDataApplication;
import com.powsybl.geodata.server.dto.SubstationGeoData;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {CassandraConfig.class, GeoDataApplication.class, EmbeddedCassandraFactoryConfig.class})
@EmbeddedCassandra(scripts = {"classpath:create_keyspace.cql", "classpath:geo_data.cql"})
public class SubstationRepositoryTest {

    @Autowired
    private SubstationRepository repository;

    @Autowired
    private Cassandra cassandra;

    @After
    public void stopCassandra() {
        cassandra.stop();
    }

    @Test
    public void test() {
        SubstationEntity.SubstationEntityBuilder substationEntityBuilder = SubstationEntity.builder()
                .country("FR")
                .id("ID")
                .coordinate(CoordinateEntity.builder().lat(3).lon(2).build());

        SubstationEntity substationEntity = substationEntityBuilder.build();

        assertEquals("SubstationEntity.SubstationEntityBuilder(country=FR, id=ID, coordinate=CoordinateEntity(lat=3.0, lon=2.0))", substationEntityBuilder.toString());
        assertEquals("SubstationEntity(country=FR, id=ID, coordinate=CoordinateEntity(lat=3.0, lon=2.0))", substationEntity.toString());

        SubstationEntity substationEntity2 = SubstationEntity.builder()
                .country("FR")
                .id("ID2")
                .coordinate(CoordinateEntity.builder().lat(4).lon(5).build())
                .build();

        SubstationEntity substationEntity3 = SubstationEntity.builder()
                .country("FR")
                .id("ID3")
                .coordinate(CoordinateEntity.builder().lat(6).lon(7).build())
                .build();

        repository.save(substationEntity);
        repository.save(substationEntity2);
        repository.save(substationEntity3);

        List<SubstationEntity> substations = repository.findAll();

        assertEquals(3, substations.size());

        SubstationGeoData substationGeoData = substations.get(0).toGeoData();

        assertEquals(3, substationGeoData.getCoordinate().getLat(), 0);
        assertEquals(2, substationGeoData.getCoordinate().getLon(), 0);

        assertEquals("FR", substations.get(0).getCountry());
        assertEquals("ID", substations.get(0).getId());
        assertEquals(3, substations.get(0).getCoordinate().getLat(), 0);
        assertEquals(2, substations.get(0).getCoordinate().getLon(), 0);
    }
}
