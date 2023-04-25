/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.dto.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.iidm.network.extensions.Coordinate;

import java.io.IOException;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CoordinateSerializer extends StdSerializer<Coordinate> {

    public CoordinateSerializer() {
        super(Coordinate.class);
    }

    @Override
    public void serialize(Coordinate coordinate, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("lat", coordinate.getLatitude());
        jsonGenerator.writeNumberField("lon", coordinate.getLongitude());
        jsonGenerator.writeEndObject();
    }
}
