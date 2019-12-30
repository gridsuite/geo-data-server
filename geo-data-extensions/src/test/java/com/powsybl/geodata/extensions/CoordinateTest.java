/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.extensions;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public class CoordinateTest {

    @Test
    public void test() {
        Coordinate coordinate = new Coordinate(1, 2);
        assertEquals(1, coordinate.getLat(), 0);
        assertEquals(2, coordinate.getLon(), 0);

        Coordinate coordinate1 = new Coordinate(1, 2);

        assertNotSame(coordinate, coordinate1);

        Coordinate coordinate2 = new Coordinate(coordinate);
        assertEquals(coordinate.getLat(), coordinate2.getLat(), 0);
        assertEquals(coordinate.getLon(), coordinate2.getLon(), 0);

        Coordinate coordinate3 = new Coordinate();
        coordinate3.setLat(1);
        coordinate3.setLon(2);
        assertEquals(1, coordinate3.getLat(), 0);
        assertEquals(2, coordinate3.getLon(), 0);
    }
}
