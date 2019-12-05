/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tdo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsybl.geo.data.extensions.Coordinate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Objects;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Data
@NoArgsConstructor
@ToString(exclude = "line")
public class SegmentGraphic {

    private com.powsybl.geo.data.extensions.Coordinate coordinate1;

    private com.powsybl.geo.data.extensions.Coordinate coordinate2;

    @JsonIgnore
    private  LineGraphic line;

    public SegmentGraphic(com.powsybl.geo.data.extensions.Coordinate coordinate1, Coordinate coordinate2, LineGraphic line) {
        this.coordinate1 = Objects.requireNonNull(coordinate1);
        this.coordinate2 = Objects.requireNonNull(coordinate2);
        this.line = line;
    }

}
