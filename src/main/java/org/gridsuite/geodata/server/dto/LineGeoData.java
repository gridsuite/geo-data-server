/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.dto;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.extensions.Coordinate;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@ToString
public class LineGeoData {

    private String id;

    private Country country1;

    private Country country2;

    String substationStart;

    String substationEnd;

    private List<Coordinate> coordinates = new ArrayList<>();
}
