/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.utils;

import com.powsybl.geodata.server.GeoDataService;
import com.powsybl.geodata.server.dto.SubstationGeoData;
import com.powsybl.iidm.network.Network;
import com.powsybl.geodata.server.dto.LineGeoData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public final class GeoDataServerUtils {

    private GeoDataServerUtils() {
    }

    public static List<LineGeoData> getLinesGraphicsElements(GeoDataService geoDataService, Network network, int page, int size) {
        // page begin from 1
        List<LineGeoData> lines = new ArrayList<>(geoDataService.getNetworkLinesCoordinates(network).values());
        int totalSize = lines.size();
        int numberOfPages = totalSize / size;
        int finalPageSize = totalSize % size;

        if (finalPageSize != 0) {
            numberOfPages++;
        }
        if (page > numberOfPages || page <= 0) {
            return new ArrayList<>();
        }
        int firstIndex = (page - 1) * size;
        int lastIndex  = firstIndex + size;
        if (lastIndex > (totalSize - 1)) {
            lastIndex = totalSize - 1;
        }
        return lines.subList(firstIndex, lastIndex);
    }

    public static List<SubstationGeoData> getSubstationsGraphicsElements(GeoDataService geoDataService, Network network, int page, int size) {
        // page begin from 1
        List<SubstationGeoData> substationGeoData = new ArrayList<>(geoDataService.getSubstationsCoordinates(network).values());
        int totalSize = substationGeoData.size();
        int numberOfPages = totalSize / size;
        int finalPageSize = totalSize % size;

        if (finalPageSize != 0) {
            numberOfPages++;
        }
        if (page > numberOfPages || page <= 0) {
            return new ArrayList<>();
        }
        int firstIndex = (page - 1) * size;
        int lastIndex  = firstIndex + size;
        if (lastIndex > (totalSize - 1)) {
            lastIndex = totalSize - 1;
        }
        return substationGeoData.subList(firstIndex, lastIndex);
    }
}
