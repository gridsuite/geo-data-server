/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package extensions;

import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.xml.NetworkXml;
import infrastructure.Coordinate;
import infrastructure.LineGraphic;
import org.joda.time.DateTime;
import org.junit.Test;

import java.awt.*;
import java.io.IOException;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public class LineGeoDataSerializerTest extends AbstractConverterTest {

    @Test
    public void test() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        network.setCaseDate(new DateTime("2019-08-20T15:43:58.556+02:00"));
        Line line = network.getLine("NHV1_NHV2_1");

        LineGraphic lineGraphic =  new LineGraphic("line", 0, Color.BLACK, 400, true);
        lineGraphic.getCoordinates().add(new Coordinate(1, 1));
        lineGraphic.getCoordinates().add(new Coordinate(2, 2));

        LineGeoData lineGeoData = new LineGeoData(line, lineGraphic);
        line.addExtension(LineGeoData.class, lineGeoData);

        NetworkXml.write(network, System.out);

        Network network2 = roundTripXmlTest(network,
                NetworkXml::writeAndValidate,
                NetworkXml::read,
                "/networkwithLineGeoDataExtension.xml");
    }
}
