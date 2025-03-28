/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.dto.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.gridsuite.geodata.server.dto.LineGeoData;

import java.io.IOException;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class LineGeoDataSerializer extends StdSerializer<LineGeoData> {
    public LineGeoDataSerializer() {
        super(LineGeoData.class);
    }

    @Override
    public void serialize(LineGeoData lineGeoData, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("id", lineGeoData.getId());
        jsonGenerator.writeStringField("c1", lineGeoData.getCountry1() != null ? lineGeoData.getCountry1().toString() : null);
        jsonGenerator.writeStringField("c2", lineGeoData.getCountry2() != null ? lineGeoData.getCountry2().toString() : null);
        jsonGenerator.writeStringField("start", lineGeoData.getSubstationStart());
        jsonGenerator.writeStringField("end", lineGeoData.getSubstationEnd());
        serializerProvider.defaultSerializeField("coordinates", lineGeoData.getCoordinates(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }
}
