/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
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
@Tag(name = "Geo data")
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

    @PostMapping(value = "/substations/infos", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get geographical data for substations with the given ids")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Substations geographical data")})
    public ResponseEntity<List<SubstationGeoData>> getSubstations(@Parameter(description = "Network UUID") @RequestParam UUID networkUuid,
                                                                  @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
                                                                  @Parameter(description = "Countries") @RequestParam(name = "country", required = false) List<String> countries,
                                                                  @RequestBody(required = false) List<String> substationIds) {
        Set<Country> countrySet = toCountrySet(countries);
        Network network = networkStoreService.getNetwork(networkUuid, substationIds != null ? PreloadingStrategy.NONE : PreloadingStrategy.COLLECTION);
        if (variantId != null) {
            network.getVariantManager().setWorkingVariant(variantId);
        }
        List<SubstationGeoData> substations = geoDataService.getSubstationsData(network, countrySet, substationIds);
        return ResponseEntity.ok().body(substations);
    }

    @PostMapping(value = "/lines/infos", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get lines geographical data")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Lines geographical data")})
    public ResponseEntity<List<LineGeoData>> getLines(@Parameter(description = "Network UUID")@RequestParam UUID networkUuid,
                                                      @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
                                                      @Parameter(description = "Countries") @RequestParam(name = "country", required = false) List<String> countries,
                                                      @RequestBody(required = false) List<String> lineIds) {
        Set<Country> countrySet = toCountrySet(countries);
        Network network = networkStoreService.getNetwork(networkUuid, lineIds != null ? PreloadingStrategy.NONE : PreloadingStrategy.COLLECTION);
        if (variantId != null) {
            network.getVariantManager().setWorkingVariant(variantId);
        }
        List<LineGeoData> lines = geoDataService.getLinesData(network, countrySet, lineIds);
        return ResponseEntity.ok().body(lines);
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
