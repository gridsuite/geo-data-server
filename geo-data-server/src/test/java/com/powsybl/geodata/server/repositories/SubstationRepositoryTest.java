/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.repositories;

import com.powsybl.geodata.server.CassandraConfig;
import com.powsybl.geodata.server.dto.SubstationGeoData;
import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.CassandraUnitDependencyInjectionTestExecutionListener;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CassandraConfig.class)
@TestExecutionListeners({ CassandraUnitDependencyInjectionTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class })
@CassandraDataSet(value = "geo_data.cql", keyspace = "geo_data")
@EmbeddedCassandra
public class SubstationRepositoryTest {

    @Autowired
    private SubstationRepository repository;

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
