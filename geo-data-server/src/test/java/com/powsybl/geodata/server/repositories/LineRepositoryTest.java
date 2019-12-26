/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.repositories;

import com.powsybl.geodata.server.CassandraConfig;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CassandraConfig.class)
@TestExecutionListeners({ CassandraUnitDependencyInjectionTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class })
@CassandraDataSet(value = "geo_data.cql", keyspace = "geo_data")
@EmbeddedCassandra
public class LineRepositoryTest {

    @Autowired
    private LineRepository repository;

    @Test
    public void test() {
        List<CoordinateEntity> coordinateEntities = new ArrayList<>();
        coordinateEntities.add(CoordinateEntity.builder().lat(11).lon(12).build());
        coordinateEntities.add(CoordinateEntity.builder().lat(13).lon(14.1).build());

        LineEntity.LineEntityBuilder lineEntityBuilder = LineEntity.builder();
        lineEntityBuilder.country("FR")
                .otherCountry("BE")
                .side1(false)
                .id("lineID")
                .coordinates(coordinateEntities);

        assertEquals("LineEntity.LineEntityBuilder(country=FR, id=lineID, side1=false, otherCountry=BE, coordinates=[CoordinateEntity(lat=11.0, lon=12.0), CoordinateEntity(lat=13.0, lon=14.1)])", lineEntityBuilder.toString());

        repository.save(lineEntityBuilder.build());

        List<LineEntity> lines = repository.findAll();

        assertEquals(1, lines.size());
        assertEquals("lineID", lines.get(0).getId());
        assertEquals("FR", lines.get(0).getCountry());
        assertEquals("BE", lines.get(0).getOtherCountry());
        assertFalse(lines.get(0).isSide1());
        assertEquals(2, lines.get(0).getCoordinates().size());
    }
}
