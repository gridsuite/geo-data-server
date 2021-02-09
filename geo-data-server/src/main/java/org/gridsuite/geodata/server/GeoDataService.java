/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import org.gridsuite.geodata.extensions.Coordinate;
import org.gridsuite.geodata.extensions.SubstationPosition;
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
import org.gridsuite.geodata.server.repositories.*;
import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Service
public class GeoDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoDataService.class);

    @Value("${network-geo-data.iterations:5}")
    private int maxIterations;

    @Autowired
    private SubstationRepository substationRepository;

    @Autowired
    private LineRepository lineRepository;

    @Autowired
    private LineCustomRepository lineCustomRepository;

    private Map<String, SubstationGeoData> readSubstationGeoDataFromDb(Set<Country> countries) {
        // read substations from DB
        // TODO filter by country
        StopWatch stopWatch = StopWatch.createStarted();

        List<SubstationEntity> substationEntities = substationRepository.findAll();
        Map<String, SubstationGeoData> substationsGeoDataDB = substationEntities.stream()
                .map(SubstationEntity::toGeoData)
                .collect(Collectors.toMap(SubstationGeoData::getId, Function.identity()));

        LOGGER.info("{} substations read from DB in {} ms", substationsGeoDataDB.size(),  stopWatch.getTime(TimeUnit.MILLISECONDS));

        return substationsGeoDataDB;
    }

    List<SubstationGeoData> getSubstations(Network network, Set<Country> countries) {
        LOGGER.info("Loading substations geo data for countries {} of network '{}'", countries, network.getId());

        Objects.requireNonNull(network);
        Objects.requireNonNull(countries);

        // get substations from the db
        Map<String, SubstationGeoData> substationsGeoDataDb = readSubstationGeoDataFromDb(countries);

        // filter substation by countries
        List<Substation> substations = network.getSubstationStream()
                .filter(s -> countries.isEmpty() || s.getCountry().filter(countries::contains).isPresent())
                .collect(Collectors.toList());

        // split substations with a known position and the others
        Map<String, SubstationGeoData> substationsGeoData = new HashMap<>();
        Set<String> substationsToCalculate = new HashSet<>();
        for (Substation substation : substations) {
            SubstationGeoData substationGeoData = substationsGeoDataDb.get(substation.getId());
            if (substationGeoData != null) {
                substationsGeoData.put(substation.getId(), substationGeoData);
            } else {
                substationsToCalculate.add(substation.getId());
            }
        }

        LOGGER.info("{} substations, {} found in the DB, {} not found", substations.size(), substationsGeoData.size(), substationsToCalculate.size());

        long accuracyFactor = Math.round(100 * (double) substationsGeoData.size() / (substationsToCalculate.size() + substationsGeoData.size()));
        if (accuracyFactor < 75) {
            LOGGER.warn("Accuracy factor is less than 75% !");
        }

        calculateMissingGeoData(network, substations, substationsGeoData, substationsToCalculate);

        return new ArrayList<>(substationsGeoData.values());
    }

    private static int neighboursComparator(Network network, Set<String> neighbors1, Set<String> neighbors2) {
        return neighbors2.stream().map(s -> network.getSubstation(s).getExtension(SubstationPosition.class)).filter(Objects::nonNull).collect(Collectors.toSet()).size() -
                neighbors1.stream().map(s -> network.getSubstation(s).getExtension(SubstationPosition.class)).filter(Objects::nonNull).collect(Collectors.toSet()).size();
    }

    enum Step {
        ONE,
        TWO
    }

    private void calculateMissingGeoData(Network network, List<Substation> substations, Map<String, SubstationGeoData> substationsGeoData,
                                         Set<String> substationsToCalculate) {
        StopWatch stopWatch = StopWatch.createStarted();

        // adjacency matrix
        Map<String, Set<String>> neighbours = getNeighbours(substations);

        // let's sort this map by values first : max neighbors having known GPS coords
        Map<String, Set<String>> sortedNeighbours = neighbours
                .entrySet()
                .stream()
                .filter(e -> !substationsGeoData.containsKey(e.getKey()))
                .sorted((e1, e2) -> neighboursComparator(network, e1.getValue(), e2.getValue()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        // STEP 1
        step(Step.ONE, network, sortedNeighbours, substationsGeoData, substationsToCalculate);

        // STEP 2
        if (!substationsToCalculate.isEmpty()) {
            step(Step.TWO, network, sortedNeighbours, substationsGeoData, substationsToCalculate);
        }

        stopWatch.stop();

        LOGGER.info("Missing substation geo data calculated in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
    }

    private void step(Step step, Network network, Map<String, Set<String>> sortedNeighbours, Map<String, SubstationGeoData> substationsGeoData,
                      Set<String> substationsToCalculate) {
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            int calculated = 0;
            for (Iterator<String> it = substationsToCalculate.iterator(); it.hasNext();) {
                String substationId = it.next();
                Set<String> neighbours = sortedNeighbours.get(substationId);

                // centroid calculation
                Substation substation = network.getSubstation(substationId);
                SubstationGeoData substationGeoData = calculateCentroidGeoData(substation, neighbours, step, substationsGeoData);
                if (substationGeoData != null) {
                    calculated++;
                    substationsGeoData.put(substationId, substationGeoData);
                    it.remove();
                }
            }
            LOGGER.info("Step {}, iteration {}, {} substation's coordinates have been calculated, {} remains unknown",
                    step == Step.ONE ? 1 : 2, iteration, calculated, substationsToCalculate.size());
            if (calculated == 0) {
                break;
            }
        }
    }

    private static SubstationGeoData calculateCentroidGeoData(Substation substation, Set<String> neighbours, Step step,
                                                              Map<String, SubstationGeoData> substationsGeoData) {
        // get neighbours geo data
        List<SubstationGeoData> neighboursGeoData = neighbours.stream().map(substationsGeoData::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Coordinate coordinate = null;
        if (neighboursGeoData.size() > 1) {
            // centroid calculation
            double lat = neighboursGeoData.stream().mapToDouble(n -> n.getCoordinate().getLat()).average().orElseThrow(IllegalStateException::new);
            double lon = neighboursGeoData.stream().mapToDouble(n -> n.getCoordinate().getLon()).average().orElseThrow(IllegalStateException::new);
            coordinate = new Coordinate(lat, lon);
        } else if (neighboursGeoData.size() == 1 && step == Step.TWO) {
            // centroid calculation
            double lat = neighboursGeoData.get(0).getCoordinate().getLat() - 0.002; // 1° correspond à 111KM
            double lon = neighboursGeoData.get(0).getCoordinate().getLon() - 0.007; // 1° correspond à 111.11 cos(1) = 60KM
            coordinate = new Coordinate(lat, lon);
        }

        Country country = substation.getCountry().orElse(null);
        return coordinate != null ? new SubstationGeoData(substation.getId(), country, coordinate) : null;
    }

    private static Map<String, Set<String>> getNeighbours(List<Substation> substations) {
        StopWatch stopWatch = StopWatch.createStarted();

        Map<String, Set<String>> neighbours = new HashMap<>();
        for (Substation s : substations) {
            neighbours.put(s.getId(), new HashSet<>());
        }

        for (Substation s : substations) {
            for (VoltageLevel vl : s.getVoltageLevels()) {
                for (Branch<?> branch : vl.getConnectables(Branch.class)) {
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

        LOGGER.info("Neighbours calculated in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));

        return neighbours;
    }

    @SuppressWarnings("javasecurity:S5145")
    void saveSubstations(List<SubstationGeoData> substationsGeoData) {
        LOGGER.info("Saving {} substations geo data", substationsGeoData.size());

        List<SubstationEntity> substationEntities = substationsGeoData.stream().map(SubstationEntity::create).collect(Collectors.toList());
        substationRepository.saveAll(substationEntities);
    }

    @SuppressWarnings("javasecurity:S5145")
    void saveLines(List<LineGeoData> linesGeoData) {
        LOGGER.info("Saving {} lines geo data", linesGeoData.size());

        List<LineEntity> linesEntities = new ArrayList<>(linesGeoData.size());
        for (LineGeoData l : linesGeoData) {
            if (l.getCountry1() == l.getCountry2())  {
                linesEntities.add(LineEntity.create(l, true));
            } else {
                linesEntities.add(LineEntity.create(l, true));
                linesEntities.add(LineEntity.create(l, false));
            }
        }
        lineRepository.saveAll(linesEntities);
    }

    private LineGeoData getLineGeoDataWithEndSubstations(Map<String, LineGeoData> linesGeoDataDb, Map<String, SubstationGeoData> substationGeoDataDb, Line line) {
        LineGeoData geoData = linesGeoDataDb.get(line.getId());
        Substation sub1 = line.getTerminal1().getVoltageLevel().getSubstation();
        Substation sub2 = line.getTerminal2().getVoltageLevel().getSubstation();
        Coordinate substation1Coordinate = substationGeoDataDb.get(sub1.getId()).getCoordinate();
        Coordinate substation2Coordinate = substationGeoDataDb.get(sub2.getId()).getCoordinate();

        if (geoData == null || geoData.getCoordinates().isEmpty()) {
            return new LineGeoData(line.getId(), sub1.getNullableCountry(), sub2.getNullableCountry(), "",
                List.of(substation1Coordinate, substation2Coordinate));
        }

        boolean reverse = sub2.getId().equals(geoData.getSubstationStart());
        return new LineGeoData(line.getId(), sub1.getNullableCountry(), sub2.getNullableCountry(),
            geoData.getSubstationStart(),
            addCoordinates(substation1Coordinate, geoData.getCoordinates(), substation2Coordinate, reverse));
    }

    private List<Coordinate> addCoordinates(Coordinate prepend, List<Coordinate> list, Coordinate append, boolean reverse) {
        List<Coordinate> res = new ArrayList<>(list.size() + 2);
        res.add(reverse ? prepend : append);
        res.addAll(list);
        res.add(reverse ? append : prepend);
        if (reverse) {
            Collections.reverse(res);
        }
        return res;

    }

    List<LineGeoData> getLines(Network network, Set<Country> countries) {
        LOGGER.info("Loading lines geo data for countries {} of network '{}'", countries, network.getId());

        Objects.requireNonNull(network);
        Objects.requireNonNull(countries);

        StopWatch stopWatch = StopWatch.createStarted();

        // read lines from DB
        // TODO filter by country
        Map<String, LineGeoData> linesGeoDataDb = lineCustomRepository.getLines();

        List<Line> lines = network.getLineStream()
                .filter(line -> countries.isEmpty()
                        || line.getTerminal1().getVoltageLevel().getSubstation().getCountry().map(countries::contains).isPresent()
                        || line.getTerminal2().getVoltageLevel().getSubstation().getCountry().map(countries::contains).isPresent())
                .collect(Collectors.toList());
        // we also want the destination substation (so we add the neighbouring country)
        Set<Country> countryAndNextTo =
            lines.stream().flatMap(line -> line.getTerminals().stream().map(term -> term.getVoltageLevel().getSubstation().getNullableCountry()).filter(Objects::nonNull))
            .collect(Collectors.toSet());
        Map<String, SubstationGeoData> substationGeoDataDb = getSubstationMap(network, countryAndNextTo);
        List<LineGeoData> lineGeoData = lines.stream().map(line -> getLineGeoDataWithEndSubstations(linesGeoDataDb, substationGeoDataDb, line)).collect(Collectors.toList());
        LOGGER.info("{} lines read from DB in {} ms", linesGeoDataDb.size(),  stopWatch.getTime(TimeUnit.MILLISECONDS));

        return lineGeoData;
    }

    private Map<String, SubstationGeoData> getSubstationMap(Network network, Set<Country> countries) {
        return getSubstations(network, countries).stream().collect(Collectors.toMap(SubstationGeoData::getId, Function.identity()));
    }
}
