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

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public class LineGraphicTest {

    @Test
    public void test() {
        LineGraphic lineGraphic = new LineGraphic("l", 400, true);

        assertEquals("l", lineGraphic.getId());
        assertTrue(lineGraphic.getCoordinates().isEmpty());
        assertNull(lineGraphic.getModel());

        Line line = EurostagTutorialExample1Factory.create().getLine("NHV1_NHV2_1");
        lineGraphic.setModel(line);
        assertEquals(lineGraphic.getModel(), line);

        SubstationGraphic side1 = new SubstationGraphic("id", new com.powsybl.geo.data.extensions.Coordinate(0, 0));
        SubstationGraphic side2 = new SubstationGraphic("id", new com.powsybl.geo.data.extensions.Coordinate(9, 18));

        lineGraphic.getCoordinates().addAll(Arrays.asList(new com.powsybl.geo.data.extensions.Coordinate(1, 2), new com.powsybl.geo.data.extensions.Coordinate(2, 3),
                new com.powsybl.geo.data.extensions.Coordinate(3, 3), new com.powsybl.geo.data.extensions.Coordinate(5, 4)));

        lineGraphic.orderCoordinates(side1, side2, new HashMap<>());

        assertEquals(4, lineGraphic.getCoordinates().size());

        LineGraphic lineGraphic1 = LineGraphic.builder()
                .aerial(true)
                .coordinates(new ArrayDeque<>())
                .country(Country.FR)
                .voltage(400)
                .build();

        lineGraphic1.getCoordinates().addAll(Arrays.asList(new com.powsybl.geo.data.extensions.Coordinate(1, 1), new com.powsybl.geo.data.extensions.Coordinate(2, 2),
                new com.powsybl.geo.data.extensions.Coordinate(3, 3), new Coordinate(7, 9)));
        lineGraphic1.orderCoordinates(side1, side2, new HashMap<>());
        lineGraphic1.addExtremities(side1, side2);

        assertEquals(4, lineGraphic.getCoordinates().size());

        assertNotNull(lineGraphic1);
    }
}
