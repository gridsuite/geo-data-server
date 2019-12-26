/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.repositories;

import com.powsybl.geodata.extensions.Coordinate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
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
public class CoordinateEntity {

    private double lat;

    private double lon;

    static List<CoordinateEntity> create(List<Coordinate> coordinates) {
        return coordinates.stream()
                .map(p -> CoordinateEntity.builder()
                        .lat(p.getLat())
                        .lon(p.getLon())
                        .build())
                .collect(Collectors.toList());
    }
}
