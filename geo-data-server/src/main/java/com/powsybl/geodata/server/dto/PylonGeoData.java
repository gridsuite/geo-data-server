/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.dto;

import com.powsybl.geodata.extensions.Coordinate;

import java.util.Objects;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PylonGeoData {

    public final Coordinate coordinate;

    public PylonGeoData() {
        coordinate = null;
    }

    public PylonGeoData(Coordinate coordinate) {
        this.coordinate = Objects.requireNonNull(coordinate);
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }
}
