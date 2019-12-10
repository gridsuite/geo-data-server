/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.dto;

import com.powsybl.geodata.extensions.Coordinate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */

public class PylonGeoDataTest {

    @Test
    public void test() {
        PylonGeoData pylonGeoData = new PylonGeoData(new Coordinate(1, 1));
        assertEquals(pylonGeoData.getCoordinate(), new Coordinate(1, 1));
    }
}
