/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tdo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Line;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.GeoDataUtils;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class LineGraphic {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineGraphic.class);

    private  String id;

    private  int drawOrder;

    private int voltage;

    private Country country;

    @JsonIgnore
    private Color color;

    private  boolean aerial;

    private boolean ordered;

    private Deque<Coordinate> coordinates = new ArrayDeque<>();

    @JsonIgnore
    private Line model;

    public LineGraphic(String id, int drawOrder, Color color, int voltage, boolean aerial) {
        this.id = Objects.requireNonNull(id);
        this.drawOrder = drawOrder;
        this.color = Objects.requireNonNull(color);
        this.aerial = aerial;
        this.voltage = voltage;
    }

    public void addExtremities(SubstationGraphic side1, SubstationGraphic side2) {
        coordinates.addFirst(side1.getPosition());
        coordinates.addLast(side2.getPosition());
    }

    public void orderCoordinates(SubstationGraphic side1, SubstationGraphic side2, Map<String, LineGraphic> lines) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        if (lines.get(id) != null && lines.get(id).isOrdered()) {
            this.coordinates = lines.get(id).getCoordinates();
            return;
        }
        // index segment by position side 1 and 2
        // create pylons
        List<PylonGraphic> pylons = coordinates.stream().map(PylonGraphic::new).collect(Collectors.toList());

        PylonGraphic current =  new PylonGraphic(side1.getPosition());

        Deque<PylonGraphic> orderedPylons = new ArrayDeque<>();

        while (!pylons.isEmpty()) {
            PylonGraphic nearest = getNearestNeigbour(current, pylons);
            if (GeoDataUtils.distance(current.getCoordinate(), side2.getPosition(), "KM") < GeoDataUtils.distance(current.getCoordinate(), nearest.getCoordinate(), "KM")) {
                break;
            }
            current = nearest;
            orderedPylons.add(current);
            pylons.remove(current);
        }
        coordinates.clear();
        coordinates = orderedPylons.stream().map(p -> new Coordinate(p.getCoordinate())).collect(Collectors.toCollection(ArrayDeque::new));
        LOGGER.info("line {} was calculatd in {} ms", id, stopWatch.getTime());
    }

    private PylonGraphic getNearestNeigbour(PylonGraphic pylonGraphic, List<PylonGraphic> pylons) {
        PylonGraphic nearest = pylons.get(0);
        double minDistance = GeoDataUtils.distance(pylonGraphic.getCoordinate(), nearest.getCoordinate(), "KM");
        for (PylonGraphic p : pylons) {
            if (GeoDataUtils.distance(pylonGraphic.getCoordinate(), p.getCoordinate(), "KM") < minDistance) {
                nearest = p;
                minDistance = GeoDataUtils.distance(pylonGraphic.getCoordinate(), p.getCoordinate(), "KM");
            }
        }
        return nearest;
    }
}
