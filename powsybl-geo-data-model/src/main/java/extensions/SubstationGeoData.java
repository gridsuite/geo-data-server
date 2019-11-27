/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package extensions;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Substation;
import infrastructure.SubstationGraphic;

/**
 *
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public class SubstationGeoData extends AbstractExtension<Substation> {

    static final String NAME = "substation-geo-data";

    private SubstationGraphic substationGraphic;

    public SubstationGeoData(Substation substation, SubstationGraphic substationGraphic) {
        super(substation);
        this.substationGraphic = substationGraphic;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public SubstationGraphic getSubstationGraphic() {
        return substationGraphic;
    }
}
