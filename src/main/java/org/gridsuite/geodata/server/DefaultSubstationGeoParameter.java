/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.powsybl.iidm.network.extensions.Coordinate;
import lombok.*;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DefaultSubstationGeoParameter {

    static final double DEFAULT_SUBSTATION_RADIUS_OFFSET = 0.0001;
    static final double DEFAULT_SUBSTATION_ANGLE_OFFSET = 25;

    static final double DECLUTTERING_THRESHOLD = 5;

    private Double radius;

    private Double angle;

    private Coordinate currentCoordinate;

    public void incrementDefaultSubstationGeoParameters() {
        this.radius += DEFAULT_SUBSTATION_RADIUS_OFFSET;
        this.angle += DEFAULT_SUBSTATION_ANGLE_OFFSET;
        currentCoordinate = new Coordinate(currentCoordinate.getLatitude() + Math.sqrt(this.radius) * Math.cos(Math.toRadians(this.angle)),
                currentCoordinate.getLongitude() + Math.sqrt(this.radius) * Math.sin(Math.toRadians(this.angle)));
    }
}
