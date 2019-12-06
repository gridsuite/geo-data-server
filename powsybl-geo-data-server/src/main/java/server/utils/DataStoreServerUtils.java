package server.utils;

import com.powsybl.iidm.network.Network;
import tdo.LineGraphic;
import tdo.SubstationGraphic;

import java.util.ArrayList;
import java.util.List;

public final class DataStoreServerUtils {

    private DataStoreServerUtils() {
    }

    public static List<LineGraphic> getLinesGraphicsElements(NetworkGeoData networkGeoData, Network network, int page, int size) {
        // page begin from 1
        List<LineGraphic> lines = new ArrayList<>(networkGeoData.getNetworkLinesCoordinates(network).values());
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

    public static List<SubstationGraphic> getSubstationsGraphicsElements(NetworkGeoData networkGeoData, Network network, int page, int size) {
        // page begin from 1
        List<SubstationGraphic> substationGraphics = new ArrayList<>(networkGeoData.getSubstationsCoordinates(network).values());
        int totalSize = substationGraphics.size();
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
        return substationGraphics.subList(firstIndex, lastIndex);
    }
}
