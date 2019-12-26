/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server;

import com.powsybl.geodata.server.dto.LineGeoData;
import com.powsybl.geodata.server.dto.SubstationGeoData;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@RestController
@RequestMapping(value = GeoDataController.API_VERSION)
@Api(value = "Geo data")
@ComponentScan(basePackageClasses = {GeoDataController.class, GeoDataService.class, NetworkStoreService.class})
public class GeoDataController {

    static final String API_VERSION = "v1";

    @Autowired
    private GeoDataService geoDataService;

    @Autowired
    private NetworkStoreService networkStoreService;

    private static Set<Country> toCountrySet(@RequestParam(required = false) List<String> countries) {
        return countries != null ? countries.stream().map(Country::valueOf).collect(Collectors.toSet()) : Collections.emptySet();
    }

    @GetMapping(value = "/substations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get substations geographical data", response = List.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Substations geographical data")})
    public ResponseEntity<List<SubstationGeoData>> getSubstations(@RequestParam UUID networkUuid,
                                                                  @RequestParam(required = false) List<String> countries) {
        Set<Country> countrySet = toCountrySet(countries);
        Network network = networkStoreService.getNetwork(networkUuid);
        List<SubstationGeoData> substations = geoDataService.getSubstations(network, countrySet);
        return ResponseEntity.ok().body(substations);
    }

    @GetMapping(value = "/lines", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get lines geographical data", response = List.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Lines geographical data")})
    public ResponseEntity<List<LineGeoData>> getLines(@RequestParam UUID networkUuid,
                                                      @RequestParam(required = false) List<String> countries) {
        Set<Country> countrySet = toCountrySet(countries);
        Network network = networkStoreService.getNetwork(networkUuid);
        List<LineGeoData> lines = geoDataService.getLines(network, countrySet);
        return ResponseEntity.ok().body(lines);
    }

    @PostMapping(value = "/substations")
    @ApiOperation(value = "Save substations geographical data")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Substations geographical data have been correctly saved")})
    public void saveSubstations(@RequestBody List<SubstationGeoData> substationGeoData) {
        geoDataService.saveSubstations(substationGeoData);
    }

    @PostMapping(value = "/lines")
    @ApiOperation(value = "Save lines geographical data")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Lines geographical data have been correctly saved")})
    public void saveLines(@RequestBody List<LineGeoData> linesGeoData) {
        geoDataService.saveLines(linesGeoData);
    }
}
