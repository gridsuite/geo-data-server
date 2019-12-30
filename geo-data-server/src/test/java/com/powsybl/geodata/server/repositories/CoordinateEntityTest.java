/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.repositories;

import com.powsybl.geodata.extensions.Coordinate;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public class CoordinateEntityTest {

    @Test
    public void test() {
        List<CoordinateEntity> coordinateEntities =  CoordinateEntity.create(Arrays.asList(new Coordinate(1, 1), new Coordinate(2, 2)));
        assertEquals(2, coordinateEntities.size(), 0);

        CoordinateEntity.CoordinateEntityBuilder coordinateEntityBuilder =  CoordinateEntity.builder().lat(2).lon(2);
        assertEquals("CoordinateEntity.CoordinateEntityBuilder(lat=2.0, lon=2.0)", coordinateEntityBuilder.toString());
        assertEquals("CoordinateEntity(lat=2.0, lon=2.0)", coordinateEntityBuilder.build().toString());
    }
}
