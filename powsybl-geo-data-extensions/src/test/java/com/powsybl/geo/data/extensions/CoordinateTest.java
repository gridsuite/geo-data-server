/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geo.data.extensions;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public class CoordinateTest {

    @Test
    public void test() {
        Coordinate coordinate = new Coordinate(1, 2);
        Coordinate coordinate2 = new Coordinate(1, 2);
        Coordinate coordinate13 = new Coordinate(coordinate);
        Assert.assertEquals(1, coordinate.getLat(), 0);
        Assert.assertEquals(2, coordinate.getLon(), 0);
        Assert.assertEquals("Coordinate(lat=1.0, lon=2.0)", coordinate.toString());
        Assert.assertEquals(coordinate, coordinate2);
        Assert.assertEquals(coordinate.hashCode(), coordinate2.hashCode());
        Coordinate coordinate3 = new Coordinate(coordinate);
        Assert.assertEquals(coordinate3, coordinate);
    }
}
