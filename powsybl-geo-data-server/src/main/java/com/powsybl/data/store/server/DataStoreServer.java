/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.data.store.server;

import com.powsybl.data.store.server.repositories.*;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import tdo.LineGraphic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.powsybl.data.store.server.repositories.LinesCustomRepository;
import server.utils.DataStoreServerUtils;
import server.utils.NetworkGeoData;
import tdo.SubstationGraphic;
import io.swagger.annotations.Api;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@RestController
@RequestMapping(value = DataStoreServer.API_VERSION)
@Api(value = "/geo/data/store", tags = "Geo data store")
@ComponentScan(basePackageClasses = {DataStoreServer.class, NetworkGeoData.class, NetworkStoreService.class})
public class DataStoreServer {

    static final String API_VERSION = "v1";

    private static final Logger LOGGER = LoggerFactory.getLogger(DataStoreServer.class);

    @Autowired
    private NetworkGeoData networkGeoData;

    @Autowired
    private NetworkStoreService service;

    @Autowired
    private SubstationsRepository substationsRepository;

    @Autowired
    private LinesRepository linesRepository;

    @Autowired
    private LinesCustomRepository linesCustomRepository;

    @GetMapping(value = "lines-with-pagination/{idNetwork}")
    @ApiOperation(value = "Get Network Lines graphics", produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of lines graphics")})
    public ResponseEntity<List<LineGraphic>> getLinesGraphicsWithPagination(@PathVariable UUID idNetwork, @RequestParam(name = "page") int page, @RequestParam(name = "size") int size) {
        Network network = service.getNetwork(idNetwork);
        return  ResponseEntity.ok().body(DataStoreServerUtils.getLinesGraphicsElements(networkGeoData, network, page, size));
    }

    @GetMapping(value = "lines/{idNetwork}")
    @ApiOperation(value = "Get Network lines graphics", produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of lines graphics")})
    public ResponseEntity<List<LineGraphic>> getLinesGraphics(@PathVariable UUID idNetwork) {
        Network network = service.getNetwork(idNetwork);
        return ResponseEntity.ok().body(new ArrayList<>(networkGeoData.getNetworkLinesCoordinates(network).values()));
    }

    @GetMapping(value = "substations/{idNetwork}")
    @ApiOperation(value = "Get Network substations graphics", produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of substations graphics")})
    public ResponseEntity<List<SubstationGraphic>> getSubstationsGraphic(@PathVariable UUID idNetwork) {
        Network network = service.getNetwork(idNetwork);
        return  ResponseEntity.ok().body(new ArrayList<>(networkGeoData.getSubstationsCoordinates(network).values()));
    }

    @GetMapping(value = "substations-with-pagination/{idNetwork}")
    @ApiOperation(value = "Get Network substations graphics", produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of substations graphics")})
    public ResponseEntity<List<SubstationGraphic>> getSubstationsGraphicsWithPagination(@PathVariable UUID idNetwork, @RequestParam(name = "page") int page, @RequestParam(name = "size") int size) {
        Network network = service.getNetwork(idNetwork);
        List<SubstationGraphic> substationGraphics = DataStoreServerUtils.getSubstationsGraphicsElements(networkGeoData, network, page, size);
        return ResponseEntity.ok().body(substationGraphics);
    }

    @GetMapping(value = "lines/{idNetwork}/{voltage}")
    @ApiOperation(value = "Get Network lines graphics", produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of lines graphics")})
    public ResponseEntity<List<LineGraphic>> getLinesGraphicsByVoltage(@PathVariable UUID idNetwork, @PathVariable int voltage) {
        Network network = service.getNetwork(idNetwork);
        return ResponseEntity.ok().body(new ArrayList<>(networkGeoData.getNetworkLinesCoordinates(network, voltage).values()));
    }

    @GetMapping(value = "lines-basic/{idNetwork}/{voltage}")
    @ApiOperation(value = "Get Network lines graphics", produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of known lines graphics")})
    public ResponseEntity<List<LineGraphic>> getLinesGraphicsByVoltageLightVersion(@PathVariable UUID idNetwork, @PathVariable int voltage) {
        Network network = service.getNetwork(idNetwork);
        return ResponseEntity.ok().body(new ArrayList<>(networkGeoData.getKnownNetworkLinesCoordinates(network, voltage).values()));
    }

    @GetMapping(value = "lines-basic/{idNetwork}")
    @ApiOperation(value = "Get Network lines graphics", produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of known lines graphics")})
    public ResponseEntity<List<LineGraphic>> getLinesGraphicsLightVersion(@PathVariable UUID idNetwork) {
        Network network = service.getNetwork(idNetwork);
        return ResponseEntity.ok().body(new ArrayList<>(networkGeoData.getKnownNetworkLinesCoordinates(network).values()));
    }

    @PostMapping(value = "substations")
    @ApiOperation(value = "Save/Update substations")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "substations Saved")})
    public ResponseEntity<Void> saveSubstations(@RequestBody List<SubstationGraphic> substationGraphics) {
        LOGGER.info("/substations [POST] : Save Substations request received");
        List<SubstationEntity> substationEntities = new ArrayList<>();
        substationGraphics.forEach(s -> substationEntities.add(SubstationEntity.builder()
                .country(s.getCountry().toString())
                .substationID(s.getId())
                .coordinate(CoordinateEntity.builder().lat(s.getPosition().getLat()).lon(s.getPosition().getLon()).build())
                .build()));
        substationsRepository.saveAll(substationEntities);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "lines")
    @ApiOperation(value = "Save/Update lines")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "lines Saved")})
    public ResponseEntity<Void> saveLines(@RequestBody List<LineGraphic> linesGraphics) {
        LOGGER.info("/lines [POST] : Save Lines request received");
        List<String> savedLines = new ArrayList<>(linesCustomRepository.getAllLines().keySet());
        // ignore lines that exist and calculated
        List<LineGraphic> filteredLinesGraphics = linesGraphics.stream().filter(lg -> !savedLines.contains(lg.getId())).collect(Collectors.toList());

        List<LineEntity> linesEntities = new ArrayList<>();
        filteredLinesGraphics.forEach(l -> linesEntities.add(LineEntity.builder()
                .country(l.getCountry().toString())
                .voltage(l.getVoltage())
                .lineID(l.getId())
                .aerial(l.isAerial())
                .ordered(false)
                .coordinates(l.getCoordinates().stream()
                        .map(p -> CoordinateEntity.builder().lat(p.getLat()).lon(p.getLon()).build())
                        .collect(Collectors.toList()))
                .build()));

        linesRepository.saveAll(linesEntities);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "precalculate-lines/{idNetwork}")
    @ApiOperation(value = "Get Network lines graphics", produces = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of lines graphics")})
    public ResponseEntity<List<LineGraphic>> precalculateLines(@PathVariable UUID idNetwork) {
        Network network = service.getNetwork(idNetwork);
        networkGeoData.precalculateLines(network);
        return ResponseEntity.ok().build();
    }
}
