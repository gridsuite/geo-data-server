/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PylonGraphic {

    static class Neighbor {

        private final PylonGraphic pylon;

        @JsonIgnore
        private final SegmentGraphic segment;

        Neighbor(PylonGraphic pylon, SegmentGraphic segment) {
            this.pylon = Objects.requireNonNull(pylon);
            this.segment = Objects.requireNonNull(segment);
        }

        PylonGraphic getPylon() {
            return pylon;
        }

        SegmentGraphic getSegment() {
            return segment;
        }
    }

    private final Coordinate coordinate;

    @JsonIgnore
    private final Set<Neighbor> neighbors = new HashSet<>();

    public PylonGraphic() {
        coordinate = null;
    }

    public PylonGraphic(Coordinate coordinate) {
        this.coordinate = Objects.requireNonNull(coordinate);
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public Set<Neighbor> getNeighbors() {
        return neighbors;
    }
}