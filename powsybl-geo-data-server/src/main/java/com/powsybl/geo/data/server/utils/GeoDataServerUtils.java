package com.powsybl.geo.data.server.utils;

import com.powsybl.geo.data.server.GeoDataService;
import com.powsybl.geo.data.server.dto.SubstationGeoData;
import com.powsybl.iidm.network.Network;
import com.powsybl.geo.data.server.dto.LineGeoData;

import java.util.ArrayList;
import java.util.List;

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
