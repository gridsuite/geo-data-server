/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.repositories;

import lombok.*;
import com.powsybl.iidm.network.extensions.Coordinate;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
@Embeddable
public class CoordinateEmbeddable {

    @Column(name = "latitude")
    private double lat;

    @Column(name = "longitude")
    private double lon;

    static List<CoordinateEmbeddable> create(List<Coordinate> coordinates) {
        return coordinates.stream()
                .map(p -> CoordinateEmbeddable.builder()
                        .lat(p.getLatitude())
                        .lon(p.getLongitude())
                        .build())
                .collect(Collectors.toList());
    }
}
