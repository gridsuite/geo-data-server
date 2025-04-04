/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.repositories;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.extensions.Coordinate;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.math3.util.Precision;
import org.gridsuite.geodata.server.dto.SubstationGeoData;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Table(indexes = {@Index(name = "substationEntity_country_index", columnList = "country")})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
@Entity
public class SubstationEntity {

    @Column
    private String country;

    @Id
    @Column
    private String id;

    @Embedded
    private CoordinateEmbeddable coordinate;

    public static SubstationEntity create(SubstationGeoData s, int geoDataRoundPrecision) {
        return SubstationEntity.builder()
            .country(s.getCountry().toString())
            .id(s.getId())
            .coordinate(CoordinateEmbeddable.builder()
                .lat(Precision.round(s.getCoordinate().getLatitude(), geoDataRoundPrecision))
                .lon(Precision.round(s.getCoordinate().getLongitude(), geoDataRoundPrecision))
                .build())
            .build();
    }

    public SubstationGeoData toGeoData() {
        return SubstationGeoData.builder()
            .country(Country.valueOf(country))
            .id(id)
            .coordinate(new Coordinate(coordinate.getLat(), coordinate.getLon()))
            .build();
    }
}
