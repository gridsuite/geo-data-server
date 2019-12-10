/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public final class PaginationUtils {

    private PaginationUtils() {
    }

    public static <T> List<T> getSublist(List<T> list, int page, int size) {
        int totalSize = list.size();

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

        return list.subList(firstIndex, lastIndex);
    }
}
