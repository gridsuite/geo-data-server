/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geo.data.server.dto;

import com.powsybl.geo.data.extensions.Coordinate;

import org.junit.Test;

import java.util.ArrayDeque;

import static org.junit.Assert.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public class BranchGeoDataTest {

    @Test
    public void test() {
        LineGeoData lineGeoData = new LineGeoData("l", 400, true);
        BranchGeoData branchGraphic = new BranchGeoData(new ArrayDeque<>(), lineGeoData);
        assertTrue(branchGraphic.getPylons().isEmpty());
        assertEquals(branchGraphic.getLine(), lineGeoData);
        branchGraphic.getPylons().add(new PylonGeoData(new Coordinate(1, 1)));
        branchGraphic.getPylons().add(new PylonGeoData(new Coordinate(2, 1)));
        branchGraphic.getPylons().add(new PylonGeoData(new Coordinate(1, 2)));
        branchGraphic.getPylons().add(new PylonGeoData(new Coordinate(2, 2)));

        BranchGeoData branchGraphic2 = new BranchGeoData();
        branchGraphic2.setLine(null);
        branchGraphic2.setPylons(branchGraphic.getPylons());
        assertNull(branchGraphic2.getLine());
        assertEquals(4, branchGraphic2.getPylons().size());

    }
}
