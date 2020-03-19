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
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {CassandraConfig.class, EmbeddedCassandraFactoryConfig.class})
@EmbeddedCassandra(scripts = "classpath:geo_data_test.cql")
public class LineRepositoryTest {

    @Autowired
    private LineRepository repository;

    @Autowired
    private Cassandra cassandra;

    @After
    public void destroyCassandra() {
        cassandra.stop();
    }

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
