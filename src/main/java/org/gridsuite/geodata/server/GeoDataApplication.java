/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import org.gridsuite.geodata.server.dto.json.CoordinateJsonModule;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@SpringBootApplication
public class GeoDataApplication {

    public static void main(String[] args) {
        SpringDocUtils.getConfig().replaceWithSchema(
                com.powsybl.iidm.network.extensions.Coordinate.class, new JsonSchema()
                .addProperty("lat", new NumberSchema().format("double"))
                .addProperty("lon", new NumberSchema().format("double")));
        SpringApplication.run(GeoDataApplication.class, args);
    }

    @Bean
    public CoordinateJsonModule createCoordinateJsonModule() {
        return new CoordinateJsonModule();
    }
}
