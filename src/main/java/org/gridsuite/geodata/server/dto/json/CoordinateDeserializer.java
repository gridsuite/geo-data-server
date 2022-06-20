/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.dto.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.iidm.network.extensions.Coordinate;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CoordinateDeserializer extends StdDeserializer<Coordinate> {

    public CoordinateDeserializer() {
        super(Coordinate.class);
    }

    static class ParsingContext {
        double lat;
        double lon;
    }

    @Override
    public Coordinate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        ParsingContext context = new ParsingContext();
        JsonUtil.parseObject(jsonParser, name -> {
            switch (name) {
                case "lat":
                    jsonParser.nextToken();
                    context.lat = jsonParser.getValueAsDouble();
                    return true;
                case "lon":
                    jsonParser.nextToken();
                    context.lon = jsonParser.getValueAsDouble();
                    return true;
                default:
                    return false;
            }
        });
        return new Coordinate(context.lat, context.lon);
    }
}
