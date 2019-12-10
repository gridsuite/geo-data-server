/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.utils;

import com.powsybl.geodata.extensions.Coordinate;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public final class GeoDataUtils {

    private GeoDataUtils() {
    }

    // Distance between two coordinates
    public static double distance(Coordinate coordinate1, Coordinate coordinate2, String unit) {
        if ((coordinate1.getLat() == coordinate2.getLat()) && (coordinate1.getLon() == coordinate2.getLon())) {
            return 0;
        } else {
            double theta = coordinate1.getLon() - coordinate2.getLon();
            double dist = Math.sin(Math.toRadians(coordinate1.getLat())) * Math.sin(Math.toRadians(coordinate2.getLat()))
                    + Math.cos(Math.toRadians(coordinate1.getLat())) * Math.cos(Math.toRadians(coordinate2.getLat())) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            if (unit.equals("K")) {
                dist = dist * 1.609344;
            } else if (unit.equals("N")) {
                dist = dist * 0.8684;
            }
            return dist;
        }
    }
}
