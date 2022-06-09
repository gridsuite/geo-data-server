/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.geodata.extensions.Coordinate;
import org.gridsuite.geodata.extensions.SubstationPosition;
import org.gridsuite.geodata.server.dto.DefaultSubstationGeoDataByCountry;
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
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 */
@Service
public class GeoDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoDataService.class);

    private static DefaultSubstationGeoDataByCountry defaultSubstationsGeoData;

    private ObjectMapper mapper = new ObjectMapper();

    @Value("${network-geo-data.iterations:5}")
    private int maxIterations;

    @Autowired
    private SubstationRepository substationRepository;

    @Autowired
    private LineRepository lineRepository;

    private Set<String> toCountryIds(Collection<Country> countries) {
        return countries.stream().map(Country::name).collect(Collectors.toSet());
    }

    @PostConstruct
    public void init() {
        defaultSubstationsGeoData = new DefaultSubstationGeoDataByCountry();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("config/substationGeoDataByCountry.json").getFile());
            defaultSubstationsGeoData = mapper.readValue(file, DefaultSubstationGeoDataByCountry.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, SubstationGeoData> readSubstationGeoDataFromDb(Set<Country> countries) {
        // read substations from DB
        StopWatch stopWatch = StopWatch.createStarted();

        List<SubstationEntity> substationEntities = countries.isEmpty() ? substationRepository.findAll() :
            substationRepository.findByCountryIn(toCountryIds(countries));
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

        DefaultSubstationGeoDataByCountry defaultSubstations = new DefaultSubstationGeoDataByCountry();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("config/substationGeoDataByCountry.json").getFile());
            defaultSubstations = mapper.readValue(file, DefaultSubstationGeoDataByCountry.class);
        } catch (IOException e) {
            e.printStackTrace();
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

    private static Coordinate getAverageCoordinate(List<SubstationGeoData> neighboursGeoData) {
        double lat = neighboursGeoData.stream().mapToDouble(n -> n.getCoordinate().getLat()).average().orElseThrow(IllegalStateException::new);
        double lon = neighboursGeoData.stream().mapToDouble(n -> n.getCoordinate().getLon()).average().orElseThrow(IllegalStateException::new);
        return new Coordinate(lat, lon);
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
            if (neighboursGeoData.stream().noneMatch(n -> Objects.equals(n.getCountry(), substation.getNullableCountry()))) {
                if (defaultSubstationsGeoData.get(substation.getNullableCountry().name()) != null) {
                    neighboursGeoData.clear();
                    neighboursGeoData.add(defaultSubstationsGeoData.get(substation.getNullableCountry().name()));
                }
            }
            coordinate = getAverageCoordinate(neighboursGeoData);
        } else if (neighboursGeoData.size() == 1 && step == Step.TWO) {
            // centroid calculation
            if (!Objects.equals(neighboursGeoData.get(0).getCountry(), substation.getNullableCountry()) && defaultSubstationsGeoData.get(substation.getNullableCountry().name()) != null) {
                neighboursGeoData.clear();
                neighboursGeoData.add(defaultSubstationsGeoData.get(substation.getNullableCountry().name()));
                coordinate = getAverageCoordinate(neighboursGeoData);
            } else {
                double lat = neighboursGeoData.get(0).getCoordinate().getLat() - 0.002; // 1° correspond à 111KM
                double lon = neighboursGeoData.get(0).getCoordinate().getLon() - 0.007; // 1° correspond à 111.11 cos(1) = 60KM
                coordinate = new Coordinate(lat, lon);
            }
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
                for (Line line : vl.getConnectables(Line.class)) {
                    Substation s1 = line.getTerminal1().getVoltageLevel().getSubstation().orElseThrow(); // TODO
                    Substation s2 = line.getTerminal2().getVoltageLevel().getSubstation().orElseThrow(); // TODO
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

        try {
            List<LineEntity> linesEntities = new ArrayList<>(linesGeoData.size());
            for (LineGeoData l : linesGeoData) {
                String coords = mapper.writeValueAsString(l.getCoordinates());
                if (l.getCountry1() == l.getCountry2()) {
                    linesEntities.add(LineEntity.create(l, true, coords));
                } else {
                    linesEntities.add(LineEntity.create(l, true, coords));
                    linesEntities.add(LineEntity.create(l, false, coords));
                }
            }
            lineRepository.saveAll(linesEntities);
        } catch (JsonProcessingException e) {
            throw new GeoDataException(GeoDataException.Type.PARSING_ERROR, e);
        }
    }

    boolean emptyOrEquals(String emptyable, String s) {
        return emptyable.isEmpty() || s.equals(emptyable);
    }

    /**
     * returns the line gps coordinates in the network order with the substations
     * coordinates added at each extremity.
     *
     * returns null when the substations at the end of the line are missing.
     */
    private LineGeoData getLineGeoDataWithEndSubstations(Map<String, LineGeoData> linesGeoDataDb, Map<String, SubstationGeoData> substationGeoDataDb, Line line) {
        LineGeoData geoData = linesGeoDataDb.get(line.getId());
        Substation sub1 = line.getTerminal1().getVoltageLevel().getSubstation().orElseThrow(); // TODO
        Substation sub2 = line.getTerminal2().getVoltageLevel().getSubstation().orElseThrow(); // TODO
        SubstationGeoData substation1GeoData = substationGeoDataDb.get(sub1.getId());
        SubstationGeoData substation2GeoData = substationGeoDataDb.get(sub2.getId());

        // TODO: we return null here even if we have line data
        // because the method is called "withEndSubstations"...
        // We could refactor this in separate methods if we ever have a
        // need to return the line in the network order without the substations
        if (substation1GeoData == null || substation2GeoData == null) {
            LOGGER.error("line {} has substations with unknown gps positions({}={}, {}={})", line.getId(),
                    sub1.getId(), substation1GeoData,
                    sub2.getId(), substation2GeoData);
            return null;
        }

        Coordinate substation1Coordinate = substation1GeoData.getCoordinate();
        Coordinate substation2Coordinate = substation2GeoData.getCoordinate();
        if (geoData == null || geoData.getCoordinates().isEmpty() || (geoData.getSubstationStart().isEmpty() && geoData.getSubstationEnd().isEmpty())) {
            return new LineGeoData(line.getId(), sub1.getNullableCountry(), sub2.getNullableCountry(), sub1.getId(), sub2.getId(),
                List.of(substation1Coordinate, substation2Coordinate));
        } else if (emptyOrEquals(geoData.getSubstationStart(),  sub2.getId()) && emptyOrEquals(geoData.getSubstationEnd(), sub1.getId())) {
            return new LineGeoData(line.getId(), sub1.getNullableCountry(), sub2.getNullableCountry(),
                geoData.getSubstationStart(),
                geoData.getSubstationEnd(),
                addCoordinates(substation1Coordinate, geoData.getCoordinates(), substation2Coordinate, true));
        } else if (emptyOrEquals(geoData.getSubstationStart(), sub1.getId()) && emptyOrEquals(geoData.getSubstationEnd(), sub2.getId())) {
            return new LineGeoData(line.getId(), sub1.getNullableCountry(), sub2.getNullableCountry(),
                geoData.getSubstationStart(),
                geoData.getSubstationEnd(),
                addCoordinates(substation1Coordinate, geoData.getCoordinates(), substation2Coordinate, false));
        }

        LOGGER.error("line {} has different substations set in geographical data ({}, {}) and network data ({}, {})", line.getId(), geoData.getSubstationStart(), geoData.getSubstationEnd(), sub1.getId(), sub2.getId());
        return new LineGeoData(line.getId(), sub1.getNullableCountry(), sub2.getNullableCountry(), sub1.getId(), sub2.getId(),
            List.of(substation1Coordinate, substation2Coordinate));

    }

    private List<Coordinate> addCoordinates(Coordinate substationStart, List<Coordinate> list, Coordinate substationEnd, boolean reverse) {
        List<Coordinate> res = new ArrayList<>(list.size() + 2);
        // we build the line as geoSubStart, [coordinates], geoSubEnd
        res.add(reverse ? substationEnd : substationStart);
        res.addAll(list);
        res.add(reverse ? substationStart : substationEnd);
        if (reverse) { // so he have the same direction as the network
            Collections.reverse(res);
        }
        return res;

    }

    @Transactional(readOnly = true)
    public List<LineGeoData> getLines(Network network, Set<Country> countries) {
        LOGGER.info("Loading lines geo data for countries {} of network '{}'", countries, network.getId());

        Objects.requireNonNull(network);
        Objects.requireNonNull(countries);

        StopWatch stopWatch = StopWatch.createStarted();

        List<Line> lines = network.getLineStream().collect(Collectors.toList());

        // read lines from DB
        Set<String> lineIds = lines.stream().map(Line::getId).collect(Collectors.toSet());
        Map<String, LineGeoData> linesGeoDataDb = lineRepository.findAllById(lineIds).stream().collect(Collectors.toMap(LineEntity::getId, this::toDto));

        // we also want the destination substation (so we add the neighbouring country)
        Set<Country> countryAndNextTo =
            lines.stream().flatMap(line -> line.getTerminals().stream().map(term -> term.getVoltageLevel().getSubstation().orElseThrow().getNullableCountry()).filter(Objects::nonNull))
                .collect(Collectors.toSet());
        Map<String, SubstationGeoData> substationGeoDataDb = getSubstationMap(network, countryAndNextTo);
        List<LineGeoData> lineGeoData = lines.stream().map(line -> getLineGeoDataWithEndSubstations(linesGeoDataDb, substationGeoDataDb, line))
            .filter(Objects::nonNull).collect(Collectors.toList());
        LOGGER.info("{} lines read from DB in {} ms", linesGeoDataDb.size(), stopWatch.getTime(TimeUnit.MILLISECONDS));

        return lineGeoData;
    }

    public LineGeoData toDto(LineEntity lineEntity) {
        try {
            return new LineGeoData(lineEntity.getId(), toDtoCountry(lineEntity.getCountry()),
                toDtoCountry(lineEntity.getOtherCountry()), lineEntity.getSubstationStart(), lineEntity.getSubstationEnd(),
                toDto(lineEntity.getCoordinates())
            );
        } catch (JsonProcessingException e) {
            throw new GeoDataException(GeoDataException.Type.PARSING_ERROR, e);
        }
    }

    private Country toDtoCountry(String country) {
        return Country.valueOf(country);
    }

    private List<Coordinate> toDto(String coordinates) throws JsonProcessingException {
        return mapper.readValue(coordinates, List.class);
    }

    private Map<String, SubstationGeoData> getSubstationMap(Network network, Set<Country> countries) {
        return getSubstations(network, countries).stream().collect(Collectors.toMap(SubstationGeoData::getId, Function.identity()));
    }
}
