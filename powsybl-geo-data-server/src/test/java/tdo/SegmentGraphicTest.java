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

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */

public class SegmentGraphicTest {

    @Test
    public void test() {
        PylonGraphic pylonGraphic = new PylonGraphic(new com.powsybl.geo.data.extensions.Coordinate(1, 1));
        PylonGraphic pylonGraphic2 = new PylonGraphic(new Coordinate(2, 2));

        LineGraphic lineGraphic = new LineGraphic("l", Color.RED, 400, true);
        SegmentGraphic segmentGraphic = new SegmentGraphic(pylonGraphic.getCoordinate(), pylonGraphic2.getCoordinate(), lineGraphic);

        assertEquals(segmentGraphic.getCoordinate1(), pylonGraphic.getCoordinate());
        assertEquals(segmentGraphic.getCoordinate2(), pylonGraphic2.getCoordinate());
        assertEquals(segmentGraphic.getLine(), lineGraphic);
        assertEquals("SegmentGraphic(coordinate1=Coordinate(lat=1.0, lon=1.0), coordinate2=Coordinate(lat=2.0, lon=2.0))", segmentGraphic.toString());
    }
}
