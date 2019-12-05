/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tdo;

import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */

public class SubstationGraphicTest {

    @Test
    public void test() {
        SubstationGraphic substationGraphic = new SubstationGraphic("id", new Coordinate(1, 2));
        assertEquals("id", substationGraphic.getId());
        assertEquals(substationGraphic.getPosition(), new Coordinate(1, 2));
        assertNull(substationGraphic.getModel());
        Substation s = EurostagTutorialExample1Factory.create().getSubstation("P1");
        substationGraphic.setModel(s);
        assertEquals(substationGraphic.getModel(), s);
        System.out.println(substationGraphic);
        assertEquals("SubstationGraphic(country=null, id=id, position=Coordinate(lat=1.0, lon=2.0), voltages=null, model=P1)", substationGraphic.toString());
    }
}
