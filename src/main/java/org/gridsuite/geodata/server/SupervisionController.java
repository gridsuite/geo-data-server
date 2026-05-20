/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Radouane Khouadri <redouane.khouadri_externe at rte-france.com>
 */
@RestController
@RequestMapping(value = GeoDataController.API_VERSION + "/supervision")
@Tag(name = "Geo data - Supervision")
public class SupervisionController {

    private final GeoDataService geoDataService;

    public SupervisionController(GeoDataService geoDataService) {
        this.geoDataService = geoDataService;
    }

    @PostMapping(value = "/substations")
    @Operation(summary = "Save substations geographical data")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Substations geographical data have been correctly saved")})
    public ResponseEntity<Void> saveSubstations(@RequestBody List<SubstationGeoData> substationGeoData) {
        geoDataService.saveSubstations(substationGeoData);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/lines")
    @Operation(summary = "Save lines geographical data")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Lines geographical data have been correctly saved")})
    public ResponseEntity<Void> saveLines(@RequestBody List<LineGeoData> linesGeoData) {
        geoDataService.saveLines(linesGeoData);
        return ResponseEntity.ok().build();
    }
}
