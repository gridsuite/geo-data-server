/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.powsybl.iidm.network.extensions.Coordinate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.databind.node.*;
import org.springframework.boot.jackson.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.*;
import java.io.IOException;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@SpringBootApplication
public class GeoDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeoDataApplication.class, args);
    }

    //TODO to remove, Coordinate in powsybl-core doesn't have a default constructor
    //Also coordinate in powsybl-core has longitude/latitude, whereas our coordinate class
    //used to have lat/lon; so accept both for now
    @JsonComponent
    public static class CoordinateJsonComponent extends JsonDeserializer<Coordinate> {

        @Override
        public Coordinate deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext)
            throws IOException {
            TreeNode treeNode = jsonParser.getCodec().readTree(jsonParser);
            if (treeNode != null) {
                DoubleNode latitude = (DoubleNode) treeNode.get("lat");
                DoubleNode longitude = (DoubleNode) treeNode.get("lon");
                if (latitude != null && longitude != null) {
                    return new Coordinate(latitude.doubleValue(), longitude.doubleValue());
                }
                latitude = (DoubleNode) treeNode.get("latitude");
                longitude = (DoubleNode) treeNode.get("longitude");
                if (latitude != null && longitude != null) {
                    return new Coordinate(latitude.doubleValue(), longitude.doubleValue());
                }
            }
            return null;
        }
    }
}
