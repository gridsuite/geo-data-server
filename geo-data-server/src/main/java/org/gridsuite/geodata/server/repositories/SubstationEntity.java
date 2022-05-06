/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.repositories;

import lombok.NoArgsConstructor;
import com.powsybl.iidm.network.extensions.Coordinate;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
import com.powsybl.iidm.network.Country;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

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

    public static SubstationEntity create(SubstationGeoData s) {
        return SubstationEntity.builder()
                .country(s.getCountry().toString())
                .id(s.getId())
                .coordinate(CoordinateEmbeddable.builder()
                        .lat(s.getCoordinate().getLatitude())
                        .lon(s.getCoordinate().getLongitude())
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
