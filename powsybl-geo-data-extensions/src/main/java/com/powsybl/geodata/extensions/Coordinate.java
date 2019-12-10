/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.extensions;

import groovy.transform.builder.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Coordinate {

    private  double lat;

    private  double lon;

    public Coordinate(Coordinate coordinate) {
        this.lon = coordinate.getLon();
        this.lat = coordinate.getLat();
    }
}
