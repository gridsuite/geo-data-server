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

public class SegmentGeoDataTest {

    @Test
    public void test() {
        PylonGeoData pylonGeoData = new PylonGeoData(new Coordinate(1, 1));
        PylonGeoData pylonGeoData2 = new PylonGeoData(new Coordinate(2, 2));

        LineGeoData lineGeoData = new LineGeoData("l", 400, true);
        SegmentGeoData segmentGeoData = new SegmentGeoData(pylonGeoData.getCoordinate(), pylonGeoData2.getCoordinate(), lineGeoData);

        assertEquals(segmentGeoData.getCoordinate1(), pylonGeoData.getCoordinate());
        assertEquals(segmentGeoData.getCoordinate2(), pylonGeoData2.getCoordinate());
        assertEquals(segmentGeoData.getLine(), lineGeoData);
        assertEquals("SegmentGeoData(coordinate1=Coordinate(lat=1.0, lon=1.0), coordinate2=Coordinate(lat=2.0, lon=2.0))", segmentGeoData.toString());
    }
}
