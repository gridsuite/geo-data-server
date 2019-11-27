/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package extensions;

import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.xml.NetworkXml;
import infrastructure.Coordinate;

import infrastructure.SubstationGraphic;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public class SubstationGeoDataSerializerTest extends AbstractConverterTest {

    @Test
    public void test() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        network.setCaseDate(new DateTime("2019-08-20T15:09:13.492Z"));

        Substation substation = network.getSubstation("P1");
        assertNotNull(substation);

        SubstationGraphic substationGraphic = new SubstationGraphic("id", new Coordinate(1, 2));
        SubstationGeoData substationGeoData = new SubstationGeoData(substation, substationGraphic);

        assertEquals(substationGeoData.getSubstationGraphic(), substationGraphic);
        assertEquals("substation-geo-data", substationGeoData.getName());

        substation.addExtension(SubstationGeoData.class, substationGeoData);

        NetworkXml.write(network, System.out);

        Network network2 = roundTripXmlTest(network,
                NetworkXml::writeAndValidate,
                NetworkXml::read,
                "/networkwithSubstationDataExtension.xml");
    }
}
