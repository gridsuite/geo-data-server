/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.dto.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.iidm.network.extensions.Coordinate;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CoordinateJsonModule extends SimpleModule {

    public CoordinateJsonModule() {
        addSerializer(Coordinate.class, new CoordinateSerializer());
        addDeserializer(Coordinate.class, new CoordinateDeserializer());
    }
}
