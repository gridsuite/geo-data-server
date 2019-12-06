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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */

public class PylonGraphicTest {

    @Test
    public void test() {
        PylonGraphic pylonGraphic = new PylonGraphic(new com.powsybl.geo.data.extensions.Coordinate(1, 1));
        PylonGraphic pylonGraphic2 = new PylonGraphic(new com.powsybl.geo.data.extensions.Coordinate(2, 2));

        LineGraphic lineGraphic = new LineGraphic("l", 1, Color.RED, 400, true);
        SegmentGraphic segmentGraphic = new SegmentGraphic(pylonGraphic.getCoordinate(), pylonGraphic2.getCoordinate(), lineGraphic);
        PylonGraphic.Neighbor neighbor = new PylonGraphic.Neighbor(pylonGraphic2, segmentGraphic);
        assertEquals(neighbor.getPylon(), pylonGraphic2);
        assertEquals(neighbor.getSegment(), segmentGraphic);

        assertEquals(pylonGraphic.getCoordinate(), new Coordinate(1, 1));
        assertTrue(pylonGraphic.getNeighbors().isEmpty());
        assertTrue(pylonGraphic.getNeighbors().add(neighbor));
        assertEquals(1, pylonGraphic.getNeighbors().size());
    }
}
