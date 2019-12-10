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
import static org.junit.Assert.assertTrue;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */

public class PylonGeoDataTest {

    @Test
    public void test() {
        PylonGeoData pylonGeoData = new PylonGeoData(new Coordinate(1, 1));
        PylonGeoData pylonGeoData2 = new PylonGeoData(new Coordinate(2, 2));

        LineGeoData lineGeoData = new LineGeoData("l", 400, true);
        SegmentGeoData segmentGeoData = new SegmentGeoData(pylonGeoData.getCoordinate(), pylonGeoData2.getCoordinate(), lineGeoData);
        PylonGeoData.Neighbor neighbor = new PylonGeoData.Neighbor(pylonGeoData2, segmentGeoData);
        assertEquals(neighbor.getPylon(), pylonGeoData2);
        assertEquals(neighbor.getSegment(), segmentGeoData);

        assertEquals(pylonGeoData.getCoordinate(), new Coordinate(1, 1));
        assertTrue(pylonGeoData.getNeighbors().isEmpty());
        assertTrue(pylonGeoData.getNeighbors().add(neighbor));
        assertEquals(1, pylonGeoData.getNeighbors().size());
    }
}
