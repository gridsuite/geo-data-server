/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.dto;

import com.powsybl.geodata.extensions.Coordinate;
import com.powsybl.iidm.network.Country;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubstationGeoData {

    private String id;

    private Country country;

    private Coordinate coordinate;
}
