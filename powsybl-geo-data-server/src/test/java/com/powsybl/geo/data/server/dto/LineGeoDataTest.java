/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geo.data.server.dto;

import com.powsybl.geo.data.extensions.Coordinate;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public class LineGeoDataTest {

    @Test
    public void test() {
        LineGeoData lineGeoData = new LineGeoData("l", 400, true);

        assertEquals("l", lineGeoData.getId());
        assertTrue(lineGeoData.getCoordinates().isEmpty());
        assertNull(lineGeoData.getModel());

        Line line = EurostagTutorialExample1Factory.create().getLine("NHV1_NHV2_1");
        lineGeoData.setModel(line);
        assertEquals(lineGeoData.getModel(), line);

        SubstationGeoData side1 = new SubstationGeoData("id", new com.powsybl.geo.data.extensions.Coordinate(0, 0));
        SubstationGeoData side2 = new SubstationGeoData("id", new com.powsybl.geo.data.extensions.Coordinate(9, 18));

        lineGeoData.getCoordinates().addAll(Arrays.asList(new com.powsybl.geo.data.extensions.Coordinate(1, 2), new com.powsybl.geo.data.extensions.Coordinate(2, 3),
                new com.powsybl.geo.data.extensions.Coordinate(3, 3), new com.powsybl.geo.data.extensions.Coordinate(5, 4)));

        lineGeoData.orderCoordinates(side1, side2, new HashMap<>());

        assertEquals(4, lineGeoData.getCoordinates().size());

        LineGeoData lineGeoData1 = LineGeoData.builder()
                .aerial(true)
                .coordinates(new ArrayDeque<>())
                .country(Country.FR)
                .voltage(400)
                .build();

        lineGeoData1.getCoordinates().addAll(Arrays.asList(new com.powsybl.geo.data.extensions.Coordinate(1, 1), new com.powsybl.geo.data.extensions.Coordinate(2, 2),
                new com.powsybl.geo.data.extensions.Coordinate(3, 3), new Coordinate(7, 9)));
        lineGeoData1.orderCoordinates(side1, side2, new HashMap<>());
        lineGeoData1.addExtremities(side1, side2);

        assertEquals(4, lineGeoData.getCoordinates().size());

        assertNotNull(lineGeoData1);
    }
}
