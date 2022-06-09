/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author Nicolas Noir <nicolas.oir at rte-france.com>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DefaultSubstationGeoDataByCountry {
    Map<String, SubstationGeoData> substationsGeoDataByCountry;

    public SubstationGeoData get(String countryName) {
        return substationsGeoDataByCountry.get(countryName);
    }
}
