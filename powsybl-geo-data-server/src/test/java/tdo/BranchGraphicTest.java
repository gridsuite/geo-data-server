/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tdo;

import com.powsybl.geo.data.extensions.Coordinate;
import org.junit.Test;

import java.awt.*;
import java.util.ArrayDeque;

import static org.junit.Assert.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public class BranchGraphicTest {

    @Test
    public void test() {
        LineGraphic lineGraphic = new LineGraphic("l", Color.RED, 400, true);
        BranchGraphic branchGraphic = new BranchGraphic(new ArrayDeque<>(), lineGraphic);
        assertTrue(branchGraphic.getPylons().isEmpty());
        assertEquals(branchGraphic.getLine(), lineGraphic);
        branchGraphic.getPylons().add(new PylonGraphic(new com.powsybl.geo.data.extensions.Coordinate(1, 1)));
        branchGraphic.getPylons().add(new PylonGraphic(new com.powsybl.geo.data.extensions.Coordinate(2, 1)));
        branchGraphic.getPylons().add(new PylonGraphic(new com.powsybl.geo.data.extensions.Coordinate(1, 2)));
        branchGraphic.getPylons().add(new PylonGraphic(new Coordinate(2, 2)));

        BranchGraphic branchGraphic2 = new BranchGraphic();
        branchGraphic2.setLine(null);
        branchGraphic2.setPylons(branchGraphic.getPylons());
        assertNull(branchGraphic2.getLine());
        assertEquals(4, branchGraphic2.getPylons().size());

    }
}
