/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geo.data.server.dto;

import com.powsybl.geo.data.extensions.Coordinate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */

public class SegmentGraphicTest {

    @Test
    public void test() {
        PylonGeoData pylonGeoData = new PylonGeoData(new com.powsybl.geo.data.extensions.Coordinate(1, 1));
        PylonGeoData pylonGeoData2 = new PylonGeoData(new Coordinate(2, 2));

        LineGeoData lineGeoData = new LineGeoData("l", 400, true);
        SegmentGraphic segmentGraphic = new SegmentGraphic(pylonGeoData.getCoordinate(), pylonGeoData2.getCoordinate(), lineGeoData);

        assertEquals(segmentGraphic.getCoordinate1(), pylonGeoData.getCoordinate());
        assertEquals(segmentGraphic.getCoordinate2(), pylonGeoData2.getCoordinate());
        assertEquals(segmentGraphic.getLine(), lineGeoData);
        assertEquals("SegmentGraphic(coordinate1=Coordinate(lat=1.0, lon=1.0), coordinate2=Coordinate(lat=2.0, lon=2.0))", segmentGraphic.toString());
    }
}
