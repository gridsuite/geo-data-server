/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.extensions.Coordinate;
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureJson
class LineRepositoryTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LineRepository repository;

    @Test
    void test() throws JsonProcessingException {
        List<Coordinate> coordinateEntities = new ArrayList<>();
        coordinateEntities.add(new Coordinate(11, 12));
        coordinateEntities.add(new Coordinate(13, 14.1));

        LineEntity.LineEntityBuilder lineEntityBuilder = LineEntity.builder();
        lineEntityBuilder.country("FR")
                .otherCountry("BE")
                .side1(false)
                .id("lineID")
                .substationStart("sub")
                .substationEnd("way")
                .coordinates(objectMapper.writeValueAsString(coordinateEntities));

        assertEquals("LineEntity.LineEntityBuilder(country=FR, id=lineID, side1=false, otherCountry=BE, substationStart$value=sub, substationEnd$value=way, coordinates=[{\"lat\":11.0,\"lon\":12.0},{\"lat\":13.0,\"lon\":14.1}])", lineEntityBuilder.toString());

        repository.save(lineEntityBuilder.build());
        List<LineEntity> lines = repository.findAll();

        assertEquals(1, lines.size());
        assertEquals("lineID", lines.get(0).getId());
        assertEquals("FR", lines.get(0).getCountry());
        assertEquals("BE", lines.get(0).getOtherCountry());
        assertEquals("sub", lines.get(0).getSubstationStart());
        assertEquals("way", lines.get(0).getSubstationEnd());
        assertFalse(lines.get(0).isSide1());
        LineEntity le = LineEntity.create(new LineGeoData("id", Country.AE, Country.AG, "Samy", "Scooby", Collections.emptyList()), true, null);
        assertEquals("id", le.getId());
        assertEquals("AE", le.getCountry());
        assertEquals("AG", le.getOtherCountry());
        assertEquals("Samy", le.getSubstationStart());
        assertEquals("Scooby", le.getSubstationEnd());
        assertTrue(le.isSide1());
    }
}
