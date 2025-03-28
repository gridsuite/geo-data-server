/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.dto.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.extensions.Coordinate;
import org.gridsuite.geodata.server.dto.LineGeoData;

import java.util.List;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class LineGeoDataDeserializer extends StdDeserializer<LineGeoData> {
    public LineGeoDataDeserializer() {
        super(LineGeoData.class);
    }

    static class ParsingContext {
        String id;
        Country country1;
        Country country2;
        String substationStart;
        String substationEnd;
        List<Coordinate> coordinates;
    }

    @Override
    public LineGeoData deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        ParsingContext context = new ParsingContext();
        JsonUtil.parseObject(jsonParser, name -> {
            switch (name) {
                case "id":
                    jsonParser.nextToken();
                    context.id = jsonParser.getValueAsString();
                    return true;
                case "country1", "c1":
                    jsonParser.nextToken();
                    context.country1 = Country.valueOf(jsonParser.getValueAsString());
                    return true;
                case "country2", "c2":
                    jsonParser.nextToken();
                    context.country2 = Country.valueOf(jsonParser.getValueAsString());
                    return true;
                case "substationStart", "start":
                    jsonParser.nextToken();
                    context.substationStart = jsonParser.getValueAsString();
                    return true;
                case "substationEnd", "end":
                    jsonParser.nextToken();
                    context.substationEnd = jsonParser.getValueAsString();
                    return true;
                case "coordinates":
                    jsonParser.nextToken();
                    context.coordinates = JsonUtil.readList(deserializationContext, jsonParser, Coordinate.class);
                    return true;
                default:
                    return false;
            }
        });
        return new LineGeoData(context.id, context.country1, context.country2, context.substationStart, context.substationEnd, context.coordinates);
    }
}
