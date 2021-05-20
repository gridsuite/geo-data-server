/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.repositories;

import lombok.*;
import org.gridsuite.geodata.extensions.Coordinate;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@UserDefinedType("coordinate")
@AllArgsConstructor
@Getter
@Builder
@ToString
public class CoordinateEntity {

    private double lat;

    private double lon;

    public CoordinateEntity(Coordinate coordinate) {
        this.lat = coordinate.getLat();
        this.lon = coordinate.getLon();
    }

    static List<CoordinateEntity> create(List<Coordinate> coordinates) {
        return coordinates.stream()
                .map(p -> CoordinateEntity.builder()
                        .lat(p.getLat())
                        .lon(p.getLon())
                        .build())
                .collect(Collectors.toList());
    }
}
