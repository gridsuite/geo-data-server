/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsybl.geodata.extensions.Coordinate;
import com.powsybl.geodata.server.utils.GeoDataUtils;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Line;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class LineGeoData {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineGeoData.class);

    private  String id;

    private int voltage;

    private Country country;

    private  boolean aerial;

    private boolean ordered;

    private Deque<Coordinate> coordinates = new ArrayDeque<>();

    @JsonIgnore
    private Line model;

    public LineGeoData(String id, int voltage, boolean aerial) {
        this.id = Objects.requireNonNull(id);
        this.aerial = aerial;
        this.voltage = voltage;
    }

    public void addExtremities(SubstationGeoData side1, SubstationGeoData side2) {
        coordinates.addFirst(side1.getPosition());
        coordinates.addLast(side2.getPosition());
    }

    public void orderCoordinates(SubstationGeoData side1, SubstationGeoData side2, Map<String, LineGeoData> lines) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        if (lines.get(id) != null && lines.get(id).isOrdered()) {
            this.coordinates = lines.get(id).getCoordinates();
            return;
        }
        // index segment by position side 1 and 2
        // create pylons
        List<PylonGeoData> pylons = coordinates.stream().map(PylonGeoData::new).collect(Collectors.toList());

        PylonGeoData current =  new PylonGeoData(side1.getPosition());

        Deque<PylonGeoData> orderedPylons = new ArrayDeque<>();

        while (!pylons.isEmpty()) {
            PylonGeoData nearest = getNearestNeigbour(current, pylons);
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

    private PylonGeoData getNearestNeigbour(PylonGeoData pylonGeoData, List<PylonGeoData> pylons) {
        PylonGeoData nearest = pylons.get(0);
        double minDistance = GeoDataUtils.distance(pylonGeoData.getCoordinate(), nearest.getCoordinate(), "KM");
        for (PylonGeoData p : pylons) {
            if (GeoDataUtils.distance(pylonGeoData.getCoordinate(), p.getCoordinate(), "KM") < minDistance) {
                nearest = p;
                minDistance = GeoDataUtils.distance(pylonGeoData.getCoordinate(), p.getCoordinate(), "KM");
            }
        }
        return nearest;
    }
}
