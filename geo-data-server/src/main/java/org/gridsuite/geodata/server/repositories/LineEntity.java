/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.repositories;

import org.gridsuite.geodata.server.dto.LineGeoData;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(indexes = {@Index(name = "lineEntity_country_index", columnList = "country"),
    @Index(name = "lineEntity_otherCountry_index", columnList = "otherCountry")})
public class LineEntity {

    @Column
    private String country;

    @Id
    @Column
    private String id;

    @Column
    private boolean side1;

    @Column
    private String otherCountry;

    @Column
    @Builder.Default
    private String substationStart = "";

    @Column
    @Builder.Default
    private String substationEnd = "";

    @Column(columnDefinition = "TEXT")
    private String coordinates;

    public static LineEntity create(LineGeoData l, boolean side1, String coordinates) {
        return LineEntity.builder()
                .country(side1 ? l.getCountry1().toString() : l.getCountry2().toString())
                .otherCountry(side1 ? l.getCountry2().toString() : l.getCountry1().toString())
                .side1(side1)
                .id(l.getId())
                .substationStart(l.getSubstationStart())
                .substationEnd(l.getSubstationEnd())
                .coordinates(coordinates)
                .build();
    }
}
