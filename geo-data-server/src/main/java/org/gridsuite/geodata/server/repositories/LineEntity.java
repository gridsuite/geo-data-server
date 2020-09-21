/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.repositories;

import org.gridsuite.geodata.server.dto.LineGeoData;
import lombok.*;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.List;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Table("lines")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class LineEntity {

    @PrimaryKeyColumn(ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String country;

    @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private String id;

    private boolean side1;

    private String otherCountry;

    private List<CoordinateEntity> coordinates;

    public static LineEntity create(LineGeoData l, boolean side1) {
        return LineEntity.builder()
                .country(side1 ? l.getCountry1().toString() : l.getCountry2().toString())
                .otherCountry(side1 ? l.getCountry2().toString() : l.getCountry1().toString())
                .side1(side1)
                .id(l.getId())
                .coordinates(CoordinateEntity.create(l.getCoordinates()))
                .build();
    }
}
