/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package server.utils;

import tdo.BaseVoltage;
import com.powsybl.geo.data.extensions.Coordinate;

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

    public static BaseVoltage baseVoltageFromVoltage(int voltage) {
        switch (voltage) {
            case 0:
                return BaseVoltage.VL_OFF;
            case 45:
                return BaseVoltage.VL_45_KV;
            case 63:
                return BaseVoltage.VL_63_KV;
            case 90:
                return BaseVoltage.VL_90_KV;
            case 150:
                return BaseVoltage.VL_150_KV;
            case 225:
                return BaseVoltage.VL_225_KV;
            case 400:
            case 380:
                return BaseVoltage.VL_400_KV;
            case -1:
            case 20:
            case 42:
                return BaseVoltage.VL_INF_45_KV;
            case -99:
                return BaseVoltage.VL_CONTINOUS_CURRENT;
            default:
                throw new AssertionError(voltage);
        }
    }
}
