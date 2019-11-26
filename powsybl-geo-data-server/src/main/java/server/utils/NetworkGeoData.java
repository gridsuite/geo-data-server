/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package server.utils;
import com.powsybl.data.store.server.repositories.*;
import com.powsybl.iidm.network.*;
import extensions.SubstationGeoData;
import infrastructure.*;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import utils.GeoDataUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Component
public final class NetworkGeoData {

    private final Logger logger = LoggerFactory.getLogger(NetworkGeoData.class);

    @Value("${network-geo-data.iterations}")
    private Integer iterations;

    @Autowired
    private SubstationsRepository substationsRepository;

    @Autowired
    private LinesRepository linesRepository;

    @Autowired
    private LinesCustomRepository linesCustomRepository;

    private NetworkGeoData() {
    }

    private Map<String, SubstationGraphic> initializeSubstationsFromDB() {
        // Read substations from DB
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Map<String, SubstationGraphic> substationsGeoDataDB;
        List<SubstationEntity> substationEntities = substationsRepository.findAll();
        substationsGeoDataDB = substationEntities.stream()
                .map(s -> new SubstationGraphic(Country.valueOf(s.getCountry()), s.getSubstationID(),
                        new Coordinate(s.getCoordinate().getLat(), s.getCoordinate().getLon()), null, null))
                .collect(Collectors.toMap(SubstationGraphic::getId, Function.identity()));
        logger.info("{} substations read from DB in {} ms", substationsGeoDataDB.size(),  stopWatch.getTime());
        return substationsGeoDataDB;
    }

    public  Map<String, SubstationGraphic> getSubstationsCoordinates(Network network) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        logger.info("BEGIN [/v1/substations-graphics/{idNetwork}]");

        Map<String, SubstationGraphic> substationsGeoDataDB = initializeSubstationsFromDB();

        Map<String, SubstationGraphic> substationsGraphicMap = new HashMap<>();

        // Known substations having  gps coordinates
        List<String> knownSubstationsIds = substationsGeoDataDB.values()
                .stream()
                .map(SubstationGraphic::getId)
                .collect(Collectors.toList());

        // Ignore all stranger substations
        List<Substation> networkSubstations = network.getSubstationStream()
                .filter(s -> !s.getId().substring(0, 1).equals("."))
                .collect(Collectors.toList());

        List<Substation> substationstoBeCalculated = networkSubstations
                .stream()
                .filter(s -> !knownSubstationsIds.contains(s.getId()))
                .collect(Collectors.toList());

        List<Substation> readySubstations = networkSubstations
                .stream()
                .filter(s -> knownSubstationsIds.contains(s.getId()))
                .collect(Collectors.toList());

        // Add coordinates to the known substations's gps coordinates
        for (Substation substation : readySubstations) {
            substationsGeoDataDB.get(substation.getId()).setModel(substation);
            substationsGraphicMap.put(substation.getId(), substationsGeoDataDB.get(substation.getId()));
        }

        long accuracyFactor = Math.round(100 * (double) readySubstations.size() / (substationstoBeCalculated.size() + readySubstations.size()));

        if (accuracyFactor < 75) {
            logger.warn("accuracy factor is less than 75% !");
        }

        // accuracy factor
        logger.info("{}% of known substation ", accuracyFactor);

        // db substations count
        logger.info("DB substations count : {} ", substationsGeoDataDB.size());

        // network substations count
        logger.info("network substations count : {} ", networkSubstations.size());

        // exist in network and csv
        logger.info("{} substations exist in the network and in the DB", readySubstations.size());

        // exist in network but not in the csv
        logger.info("{} substations to be calculated", substationstoBeCalculated.size());

        substationsReplacementStrategy(network, networkSubstations, knownSubstationsIds, substationsGraphicMap);

        logger.info("END [/v1/substations-graphics/{idNetwork}] request time : {} ms", stopWatch.getTime());
        return substationsGraphicMap;
    }

    private  void substationsReplacementStrategy(Network network, List<Substation> networkSubstations, List<String> ids, Map<String, SubstationGraphic> substationGraphicMap) {

        AtomicInteger remaining = new AtomicInteger();

        // adjacency matrix
        HashMap<String, Set<String>> neighbours = updateNeighbours(networkSubstations);

        // let's sort this map by values first : max neigbours having known GPS coords
        HashMap<String, Set<String>> sortedByNeigboursCountHavingKnownGpsCoords = neighbours
                .entrySet()
                .stream()
                .filter(e -> !ids.contains(e.getKey()) && !e.getValue().isEmpty())
                .sorted((e1, e2) ->
                        e2.getValue().stream().map(s -> network.getSubstation(s).getExtension(SubstationGeoData.class)).filter(Objects::nonNull)
                                .collect(Collectors.toSet()).size() -
                                e1.getValue().stream().map(s -> network.getSubstation(s).getExtension(SubstationGeoData.class)).filter(Objects::nonNull)
                                        .collect(Collectors.toSet()).size())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        // substations have 1 neigbour
        logger.info("substations having 0 neigbour : {} ", sortedByNeigboursCountHavingKnownGpsCoords.values().stream().mapToInt(Set::size).filter(e -> e == 0).count());

        // substations have 1 neigbour
        logger.info("substations having 1 neigbour : {} ", sortedByNeigboursCountHavingKnownGpsCoords.values().stream().mapToInt(Set::size).filter(e -> e == 1).count());

        // STEP 1
        stepOne(network, remaining, ids, sortedByNeigboursCountHavingKnownGpsCoords, substationGraphicMap);

        // STEP 2
        if (remaining.get() != 0) {
            stepTwo(network, ids, sortedByNeigboursCountHavingKnownGpsCoords, substationGraphicMap);
        }

    }

    private  void stepOne(Network network, AtomicInteger remaining, List<String> ids, HashMap<String, Set<String>> sortedByNeigboursCountHavingKnownGpsCoords, Map<String, SubstationGraphic> substationGraphicMap) {
        int corrected = 0;
        // STEP 1
        for (int iteration = 0; iteration < iterations; iteration++) {
            logger.info("iteration {} :", iteration);
            for (Entry<String, Set<String>> entry : sortedByNeigboursCountHavingKnownGpsCoords.entrySet()) {
                if (!ids.contains(entry.getKey())) {
                    // cfinalentroid calculation
                    SubstationGraphic substationGraphic = calculateCentroidGeoData(network.getSubstation(entry.getKey()), entry.getValue(), 1, substationGraphicMap);
                    if (substationGraphic != null) {
                        ids.add(entry.getKey());
                        corrected++;
                        substationGraphicMap.put(entry.getKey(), substationGraphic);
                    } else {
                        remaining.getAndIncrement();
                    }
                }
            }
            logger.info("{} substation's coordinates were calculated :", corrected);
            logger.info("remaining {} :", remaining.get());
            if (corrected == 0) {
                break;
            }
            corrected = 0;
            remaining.set(0);
        }
    }

    private  void stepTwo(Network network, List<String> ids,
                          HashMap<String, Set<String>> sortedByNeigboursCountHavingKnownGpsCoords, Map<String, SubstationGraphic> substationGraphicMap) {
        // we have substations that we can't calculate their centroid becauce they have zero or one neigbours known GPS coords
        // step 1
        HashMap<String, Set<String>> remainingMap = sortedByNeigboursCountHavingKnownGpsCoords.entrySet()
                .stream()
                .filter(e -> !ids.contains(e.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        int checkRemaining = remainingMap.size();
        int checkCalculated = 0;
        int iter = 0;

        for (int i = 0; i < iterations; i++) {
            logger.info("Step 2 : iteration {} :", iter);
            iter++;
            for (Entry<String, Set<String>> entry : remainingMap.entrySet()) {
                if (!ids.contains(entry.getKey())) {
                    // centroid calculation
                    SubstationGraphic substationGraphic = calculateCentroidGeoData(network.getSubstation(entry.getKey()), entry.getValue(), 2, substationGraphicMap);
                    if (substationGraphic != null) {
                        ids.add(entry.getKey());
                        checkCalculated++;
                        checkRemaining--;
                        substationGraphicMap.put(entry.getKey(), substationGraphic);
                    }
                }
            }
            logger.info("{} substation's coordinates were calculated :", checkCalculated);
            logger.info("remaining {} :", checkRemaining);
            if (checkRemaining == 0) {
                break;
            }
            checkCalculated = 0;
        }
    }

    private  SubstationGraphic calculateCentroidGeoData(Substation substation, Set<String> neighbours, int step, Map<String, SubstationGraphic> substationGraphicMap) {

        // Get neigbours geo data
        List<SubstationGraphic> neighboursGeoData = neighbours.stream().map(substationGraphicMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        SubstationGraphic substationGraphic = null;

        if (neighboursGeoData.size() > 1) {
            // Centroid calculation
            final OptionalDouble lat;
            final OptionalDouble lon;
            lat = neighboursGeoData.stream().mapToDouble(n -> n.getPosition().getLat()).average();
            lon = neighboursGeoData.stream().mapToDouble(n -> n.getPosition().getLon()).average();
            if (lat.isPresent() && lon.isPresent()) {
                substationGraphic = new SubstationGraphic(substation.getId(), new Coordinate(lat.getAsDouble(), lon.getAsDouble()));
                substationGraphic.setModel(substation);
            }
            return substationGraphic;
        } else if (neighboursGeoData.size() == 1 && step == 2) {
            // Centroid calculation
            final double lat;
            final double lon;
            lat = neighboursGeoData.get(0).getPosition().getLat() - 0.002; //1° correspond à 111KM
            lon = neighboursGeoData.get(0).getPosition().getLon() - 0.007; //1° correspond à 111.11 cos(1) = 60KM

            substationGraphic = new SubstationGraphic(substation.getId(), new Coordinate(lat, lon));
            substationGraphic.setModel(substation);
            substation.addExtension(SubstationGeoData.class, new SubstationGeoData(substation, substationGraphic));

            return substationGraphic;
        } else {
            return null;
        }
    }

    private  HashMap<String, Set<String>> updateNeighbours(List<Substation> networkSbstations) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        HashMap<String, Set<String>> neighbours = new HashMap<>();
        for (Substation s : networkSbstations) {
            neighbours.put(s.getId(), new HashSet<>());
        }

        for (Substation s : networkSbstations) {
            for (VoltageLevel vl : s.getVoltageLevels()) {
                for (Branch branch : vl.getConnectables(Branch.class)) {
                    Substation s1 = branch.getTerminal1().getVoltageLevel().getSubstation();
                    Substation s2 = branch.getTerminal2().getVoltageLevel().getSubstation();
                    if (s1 != s) {
                        neighbours.get(s.getId()).add(s1.getId());
                    } else if (s2 != s) {
                        neighbours.get(s.getId()).add(s2.getId());
                    }
                }
            }
        }
        logger.info("neighbours were calculated in {} ms", stopWatch.getTime());
        return neighbours;
    }

    private void saveNewCalculatedLine(LineGraphic lineGraphic) {
        logger.warn("{} is now ready for use as precalculated line", lineGraphic.getId());
        // now we are sure that it's ordered so we have to save the state
        lineGraphic.setOrdered(true);
        linesRepository.save(LineEntity
                .builder()
                .country(lineGraphic.getCountry().toString())
                .lineID(lineGraphic.getId())
                .voltage(lineGraphic.getVoltage())
                .aerial(lineGraphic.isAerial())
                .ordered(lineGraphic.isOrdered())
                .coordinates(lineGraphic.getCoordinates().stream()
                        .map(p -> CoordinateEntity.builder()
                                .lat(p.getLat())
                                .lon(p.getLon()).build())
                        .collect(Collectors.toList()))
                .build());
    }

    private void addSimpleLine(SubstationGraphic side1, SubstationGraphic side2, Map<String, LineGraphic> networkLineGraphicMap,
                               Line line, double voltage) {
        // we know substations to wchich the line is connected
        Deque<Coordinate> positions = new ArrayDeque<>();
        positions.add(side1.getPosition());
        positions.add(side2.getPosition());

        LineGraphic lineGraphic = new LineGraphic();
        lineGraphic.setVoltage((int) voltage);
        lineGraphic.setColor(GeoDataUtils.baseVoltageFromVoltage((int) voltage).getColor());
        lineGraphic.setCoordinates(positions);
        lineGraphic.setId(line.getId());
        lineGraphic.setDrawOrder(GeoDataUtils.baseVoltageFromVoltage((int) voltage).getOrder());

        networkLineGraphicMap.put(line.getId(), lineGraphic);
    }

    private void calculateLinesCoordinates(Network network, Map<String, SubstationGraphic> substationGraphicMap, Map<String,
            LineGraphic> linesGeoDataDB, Map<String, LineGraphic> networkLineGraphicMap) {
        // for statistics
        int tracedLines = 0;
        List<Line> ignoredLines = new ArrayList<>();
        // for each line of our network
        for (Line line : network.getLines()) {
            SubstationGraphic side1 = substationGraphicMap.get(line.getTerminal1().getVoltageLevel().getSubstation().getId());
            SubstationGraphic side2 = substationGraphicMap.get(line.getTerminal2().getVoltageLevel().getSubstation().getId());

            if (side1 != null && side2 != null) {
                tracedLines++;
                if (linesGeoDataDB.get(line.getId()) != null) {
                    LineGraphic lineGraphic = linesGeoDataDB.get(line.getId());
                    lineGraphic.orderCoordinates(side1, side2, linesGeoDataDB);
                    if (!lineGraphic.isOrdered()) {
                        saveNewCalculatedLine(lineGraphic);
                    }
                    lineGraphic.addExtremities(side1, side2);
                    networkLineGraphicMap.put(line.getId(), lineGraphic);
                } else {
                    addSimpleLine(side1, side2, networkLineGraphicMap, line, line.getTerminal1().getVoltageLevel().getNominalV());
                }
            } else {
                ignoredLines.add(line);
            }
        }

        logger.info("network lines count: {}", network.getLineCount());
        logger.info("{} traced lines", tracedLines);
        logger.info("{} ignored lines ", network.getLineCount() - tracedLines);
    }

    public  Map<String, LineGraphic> getNetworkLinesCoordinates(Network network) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        logger.info("BEGIN [/v1/lines-graphics/{idNetwork}]");

        //get all substations coordinates
        Map<String, SubstationGraphic> substationGraphicMap = getSubstationsCoordinates(network);
        Map<String, LineGraphic> linesGeoDataDB = linesCustomRepository.getAllLines();
        Map<String, LineGraphic> networkLineGraphicMap = new HashMap<>();

        calculateLinesCoordinates(network, substationGraphicMap, linesGeoDataDB, networkLineGraphicMap);

        logger.info("all network lines were calculated, END [/v1/lines-graphics/{idNetwork}] request time : {} ms", stopWatch.getTime());
        return networkLineGraphicMap;
    }

    public  Map<String, LineGraphic> getNetworkLinesCoordinates(Network network, int voltage) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        logger.info("BEGIN [/v1/lines-graphics/{idNetwork}/{voltage}]");

        //get all substations coordinates
        Map<String, SubstationGraphic> substationGraphicMap = getSubstationsCoordinates(network);
        Map<String, LineGraphic> linesGeoDataDB = linesCustomRepository.getLines("FR", voltage);
        Map<String, LineGraphic> networkLineGraphicMap = new HashMap<>();

        calculateLinesCoordinates(network, substationGraphicMap, linesGeoDataDB, networkLineGraphicMap);

        logger.info("END [/v1/lines-graphics/{idNetwork}/{voltage}] request time : {} ms", stopWatch.getTime());
        return networkLineGraphicMap;
    }

    private void networkKnownLinesCoordinates(Network network, Map<String, LineGraphic> linesGeoDataDB, Map<String, LineGraphic> networkLineGraphicMap) {
        // for each line of our network
        for (Line line : network.getLines()) {
            if (linesGeoDataDB.get(line.getId()) != null) {
                LineGraphic lineGraphic = linesGeoDataDB.get(line.getId());
                networkLineGraphicMap.put(line.getId(), lineGraphic);
            }
        }

        logger.info("network lines count: {}", network.getLineCount());
    }

    //This method returns only network's calculated lines even two sides are not known !
    public  Map<String, LineGraphic> getKnownNetworkLinesCoordinates(Network network) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        logger.info("BEGIN [/v1/lines-graphics/{idNetwork}]");

        //get all substations coordinates
        Map<String, LineGraphic> linesGeoDataDB = linesCustomRepository.getAllLines();
        Map<String, LineGraphic> networkLineGraphicMap = new HashMap<>();

        networkKnownLinesCoordinates(network, linesGeoDataDB, networkLineGraphicMap);
        logger.info("Only known lines coordinates are sent, " +
                "END [/v1/lines-graphics/{idNetwork}] request time : {} ms", stopWatch.getTime());
        return networkLineGraphicMap;
    }

    public  Map<String, LineGraphic> getKnownNetworkLinesCoordinates(Network network, int voltage) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        logger.info("BEGIN [/v1/lines-graphics/{idNetwork}/{voltage}]");

        Map<String, LineGraphic> linesGeoDataDB = linesCustomRepository.getLines("FR", voltage);
        Map<String, LineGraphic> networkLineGraphicMap = new HashMap<>();

        networkKnownLinesCoordinates(network, linesGeoDataDB, networkLineGraphicMap);

        logger.info("Only known lines coordinates are sent," +
                "END [/v1/lines-graphics/{idNetwork}] request time : {} ms", stopWatch.getTime());
        return networkLineGraphicMap;
    }

    public  void precalculateLines(Network network) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        logger.info("BEGIN [/v1/precalculate-lines-on-import/{idNetwork}]");

        //get all substations coordinates
        Map<String, SubstationGraphic> substationGraphicMap = getSubstationsCoordinates(network);
        Map<String, LineGraphic> linesGeoDataDB = linesCustomRepository.getAllLines();
        Map<String, LineGraphic> networkLineGraphicMap = new HashMap<>();

        calculateLinesCoordinates(network, substationGraphicMap, linesGeoDataDB, networkLineGraphicMap);

        logger.info("all network lines were calculated, END [/v1/precalculate-lines-on-import/{idNetwork}] request time : {} ms", stopWatch.getTime());
    }
}
