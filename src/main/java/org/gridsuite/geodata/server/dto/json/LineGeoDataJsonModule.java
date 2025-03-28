/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.dto.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.gridsuite.geodata.server.dto.LineGeoData;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class LineGeoDataJsonModule extends SimpleModule {
    public LineGeoDataJsonModule() {
        addSerializer(LineGeoData.class, new LineGeoDataSerializer());
        addDeserializer(LineGeoData.class, new LineGeoDataDeserializer());
    }
}
