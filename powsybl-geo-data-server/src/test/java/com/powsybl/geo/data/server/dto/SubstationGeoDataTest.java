/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geo.data.server.dto;

import com.powsybl.geo.data.extensions.Coordinate;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */

public class SubstationGeoDataTest {

    @Test
    public void test() {
        SubstationGeoData substationGeoData = new SubstationGeoData("id", new Coordinate(1, 2));
        assertEquals("id", substationGeoData.getId());
        assertEquals(substationGeoData.getPosition(), new Coordinate(1, 2));
        assertNull(substationGeoData.getModel());
        Substation s = EurostagTutorialExample1Factory.create().getSubstation("P1");
        substationGeoData.setModel(s);
        assertEquals(substationGeoData.getModel(), s);
        System.out.println(substationGeoData);
        assertEquals("SubstationGeoData(country=null, id=id, position=Coordinate(lat=1.0, lon=2.0), voltages=null, model=P1)", substationGeoData.toString());
    }
}
