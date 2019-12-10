/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geo.data.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsybl.geo.data.extensions.Coordinate;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Substation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubstationGeoData {

    private Country country;

    private String id;

    private Coordinate position;

    private List<Integer> voltages;

    @JsonIgnore
    private Substation model;

    public SubstationGeoData(String id, Coordinate position) {
        this.id = Objects.requireNonNull(id);
        this.position = Objects.requireNonNull(position);
    }
}
