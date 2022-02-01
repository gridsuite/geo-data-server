/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.repositories;

import com.powsybl.iidm.network.Country;
import org.gridsuite.geodata.server.GeoDataApplication;
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@RunWith(SpringRunner.class)
@ContextHierarchy({
    @ContextConfiguration(classes = {GeoDataApplication.class})
})
public class LineRepositoryTest {

    @Autowired
    private LineRepository repository;

    @Before
    public void setUp() {
        repository.deleteAll();
    }

    @Test
    public void test() {
        List<CoordinateEmbeddable> coordinateEntities = new ArrayList<>();
        coordinateEntities.add(CoordinateEmbeddable.builder().lat(11).lon(12).build());
        coordinateEntities.add(CoordinateEmbeddable.builder().lat(13).lon(14.1).build());

        LineEntity.LineEntityBuilder lineEntityBuilder = LineEntity.builder();
        lineEntityBuilder.country("FR")
                .otherCountry("BE")
                .side1(false)
                .id("lineID")
                .substationStart("sub")
                .substationEnd("way")
                .coordinates(coordinateEntities);

        assertEquals("LineEntity.LineEntityBuilder(country=FR, id=lineID, side1=false, otherCountry=BE, substationStart$value=sub, substationEnd$value=way, coordinates=[CoordinateEmbeddable(lat=11.0, lon=12.0), CoordinateEmbeddable(lat=13.0, lon=14.1)])", lineEntityBuilder.toString());

        repository.save(lineEntityBuilder.build());
        List<LineEntity> lines = repository.findAll();

        assertEquals(1, lines.size());
        assertEquals("lineID", lines.get(0).getId());
        assertEquals("FR", lines.get(0).getCountry());
        assertEquals("BE", lines.get(0).getOtherCountry());
        assertEquals("sub", lines.get(0).getSubstationStart());
        assertEquals("way", lines.get(0).getSubstationEnd());
        assertFalse(lines.get(0).isSide1());
        LineEntity le = LineEntity.create(new LineGeoData("id", Country.AE, Country.AG, "Samy", "Scooby", Collections.emptyList()), true);
        assertEquals("id", le.getId());
        assertEquals("AE", le.getCountry());
        assertEquals("AG", le.getOtherCountry());
        assertEquals("Samy", le.getSubstationStart());
        assertEquals("Scooby", le.getSubstationEnd());
        assertTrue(le.isSide1());

    }
}
