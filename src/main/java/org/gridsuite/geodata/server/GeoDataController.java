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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoDataController.class);

    @Autowired
    private GeoDataService geoDataService;

    @Autowired
    private NetworkStoreService networkStoreService;

    private final ExecutorService executorService;

    public GeoDataController(@Value("${max-concurrent-requests}") int maxConcurrentRequests) {
        this.executorService = Executors.newFixedThreadPool(maxConcurrentRequests);
    }

    private static Set<Country> toCountrySet(@RequestParam(required = false) List<String> countries) {
        return countries != null ? countries.stream().map(Country::valueOf).collect(Collectors.toSet()) : Collections.emptySet();
    }

    @GetMapping(value = "/substations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SubstationGeoData>> getSubstations(
            @RequestParam UUID networkUuid,
            @RequestParam(name = "variantId", required = false) String variantId,
            @RequestParam(name = "country", required = false) List<String> countries,
            @RequestParam(name = "substationId", required = false) List<String> substationIds) throws ExecutionException, InterruptedException {

        CompletableFuture<ResponseEntity<List<SubstationGeoData>>> futureSubstations = CompletableFuture.supplyAsync(() -> {
            try {
                Set<Country> countrySet = toCountrySet(countries);
                Network network = networkStoreService.getNetwork(networkUuid, substationIds != null ? PreloadingStrategy.NONE : PreloadingStrategy.COLLECTION);
                if (variantId != null) {
                    network.getVariantManager().setWorkingVariant(variantId);
                }
                List<SubstationGeoData> substations;
                if (substationIds != null) {
                    if (!countrySet.isEmpty()) {
                        LOGGER.warn("Countries will not be taken into account to filter substation position.");
                    }
                    substations = geoDataService.getSubstationsByIds(network, new HashSet<>(substationIds));
                } else {
                    substations = geoDataService.getSubstationsByCountries(network, countrySet);
                }
                return ResponseEntity.ok(substations);
            } catch (Exception e) {
                LOGGER.error("Error fetching line data: ", e);
                return ResponseEntity.internalServerError().body(Collections.emptyList());
            }
        }, executorService);
        return futureSubstations.get();
    }

    @GetMapping(value = "/lines", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<LineGeoData>> getLines(
            @RequestParam UUID networkUuid,
            @RequestParam(name = "variantId", required = false) String variantId,
            @RequestParam(name = "country", required = false) List<String> countries,
            @RequestParam(name = "lineId", required = false) List<String> lineIds) throws ExecutionException, InterruptedException {

        CompletableFuture<ResponseEntity<List<LineGeoData>>> futureLines = CompletableFuture.supplyAsync(() -> {
            try {
                Set<Country> countrySet = toCountrySet(countries);
                Network network = networkStoreService.getNetwork(networkUuid, lineIds != null ? PreloadingStrategy.NONE : PreloadingStrategy.COLLECTION);
                if (variantId != null) {
                    network.getVariantManager().setWorkingVariant(variantId);
                }
                List<LineGeoData> lines;
                if (lineIds != null) {
                    if (!countrySet.isEmpty()) {
                        LOGGER.warn("Countries will not be taken into account to filter line position.");
                    }
                    lines = geoDataService.getLinesByIds(network, new HashSet<>(lineIds));
                } else {
                    lines = geoDataService.getLinesByCountries(network, countrySet);
                }
                return ResponseEntity.ok(lines);
            } catch (Exception e) {
                LOGGER.error("Error fetching line data: ", e);
                return ResponseEntity.internalServerError().body(Collections.emptyList());
            }
        }, executorService);

        return futureLines.get();
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

    @PreDestroy
    private void preDestroy() {
        executorService.shutdown();
    }
}
