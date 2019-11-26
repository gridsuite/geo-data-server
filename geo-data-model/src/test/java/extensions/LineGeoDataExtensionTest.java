/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package extensions;

import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import infrastructure.Coordinate;
import infrastructure.LineGraphic;
import javafx.scene.paint.Color;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public class LineGeoDataExtensionTest {

    @Test
    public void test() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        Line line = network.getLine("NHV1_NHV2_1");

        LineGraphic lineGraphic = new LineGraphic("line", 1, Color.BLACK, 225, true);
        lineGraphic.getCoordinates().add(new Coordinate(1, 1));
        lineGraphic.getCoordinates().add(new Coordinate(2, 2));

        LineGeoData lineGeoData = new LineGeoData(line, lineGraphic);

        assertEquals(lineGeoData.getLineGraphic(), lineGraphic);
        assertEquals("line-geo-data", lineGeoData.getName());

        line.addExtension(LineGeoData.class, lineGeoData);
    }
}
