/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.iidm.network.extensions.Coordinate;
import com.powsybl.iidm.network.extensions.SubstationPosition;
import com.powsybl.ws.commons.LogUtils;
import org.apache.commons.lang3.StringUtils;
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

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.ws.commons.LogUtils.sanitizeParam;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 */
@Service
public class GeoDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoDataService.class);

    static final double CALCULATED_SUBSTATION_OFFSET = 0.005;

    @Autowired
    private ObjectMapper mapper;

    @Value("${network-geo-data.iterations:50}")
    private int maxIterations;

    @Autowired
    private SubstationRepository substationRepository;

    @Autowired
    private LineRepository lineRepository;

    @Autowired
    private DefaultSubstationGeoDataByCountry defaultSubstationsGeoData;

    private Set<String> toCountryIds(Collection<Country> countries) {
        return countries.stream().map(Country::name).collect(Collectors.toSet());
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
            SubstationGeoData substationGeoData = Optional.ofNullable(substationsGeoDataDb.get(substation.getId())).orElseGet(() -> substationsGeoDataDb.get(substation.getNameOrId()));
            if (substationGeoData != null && (substation.getCountry().isEmpty() || substation.getCountry().filter(c -> c.name().equals(substationGeoData.getCountry().name())).isPresent())) {
                substationGeoData.setId(substation.getId());
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

        // adjacency matrix
        Map<String, Set<String>> neighbours = getNeighbours(substations);

        // let's sort this map by values first : max neighbors having known GPS coords
        Map<String, Set<String>> sortedNeighbours = neighbours
                .entrySet()
                .stream()
                .filter(e -> !substationsGeoData.containsKey(e.getKey()))
                .sorted((e1, e2) -> neighboursComparator(network, e1.getValue(), e2.getValue()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        calculateMissingGeoData(network, sortedNeighbours, substationsGeoData, substationsToCalculate);
        calculateDefaultSubstationsGeoData(substationsGeoData, sortedNeighbours);

        return new ArrayList<>(substationsGeoData.values());
    }

    List<SubstationGeoData> getSubstations(Network network, List<String> substationsIds) {
        List<String> escapedIds = substationsIds.stream().map(LogUtils::sanitizeParam).collect(Collectors.toList());
        LOGGER.info("Loading substations geo data for substations with ids {} of network '{}'", StringUtils.join(escapedIds, ", "), network.getId());

        StopWatch stopWatch = StopWatch.createStarted();

        Map<String, SubstationGeoData> substationsGeoData = new HashMap<>();

        Set<String> substationsToCalculate = new HashSet<>();

        Map<String, SubstationGeoData> geoDataForComputation = new HashMap<>();
        Map<String, Set<String>> neighboursBySubstationId = new HashMap<>();

        prepareGeoDataForComputation(network, geoDataForComputation, neighboursBySubstationId, substationsToCalculate, substationsIds.stream().collect(Collectors.toSet()));

        //Calculated data are added to geoDataForComputation
        calculateMissingGeoData(network, neighboursBySubstationId, geoDataForComputation, substationsToCalculate);
        calculateDefaultSubstationsGeoData(geoDataForComputation, neighboursBySubstationId);

        //We remove linked substations from result - we only want requested ones
        geoDataForComputation.keySet().removeIf(key -> !substationsIds.contains(key));

        LOGGER.info("Substations with given ids read/computed from DB in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
        //We return geo data found in the DB and the computed ones
        return new ArrayList<>(Stream.concat(geoDataForComputation.values().stream(), substationsGeoData.values().stream()).collect(Collectors.toList()));
    }

    private void prepareGeoDataForComputation(Network network, Map<String, SubstationGeoData> geoDataForComputation, Map<String, Set<String>> neighboursBySubstationId, Set<String> substationsToCalculate, Set<String> neighbours) {
        Set<String> neighboursToBeTreated = new HashSet<>(neighbours);
        while (!neighboursToBeTreated.isEmpty()) {
            Map<String, SubstationGeoData> foundGeoData = substationRepository.findByIdIn(neighboursToBeTreated).stream()
                    .map(SubstationEntity::toGeoData)
                    .collect(Collectors.toMap(SubstationGeoData::getId, Function.identity()));

            geoDataForComputation.putAll(foundGeoData);

            Set<String> allNeighbours = new HashSet<>();

            List<Substation> substations = new ArrayList<>();

            neighboursToBeTreated.stream().forEach(neighbourId -> {
                if (geoDataForComputation.get(neighbourId) == null && !substationsToCalculate.contains(neighbourId)) {
                    substationsToCalculate.add(neighbourId);
                    substations.add(network.getSubstation(neighbourId));
                }
            });
            Map<String, Set<String>> newNeighbours = getNeighbours(substations);
            substations.stream().forEach(substation -> allNeighbours.addAll(newNeighbours.get(substation.getId())));
            neighboursBySubstationId.putAll(newNeighbours);
            neighboursToBeTreated = allNeighbours;
        }
    }

    private void calculateDefaultSubstationsGeoData(Map<String, SubstationGeoData> substationsGeoData, Map<String, Set<String>> sortedNeighbours) {
        StopWatch stopWatch = StopWatch.createStarted();
        for (Map.Entry<String, SubstationGeoData> entry : defaultSubstationsGeoData.getEntrySet()) {
            Set<String> clutteredSubstationsIds = substationsGeoData.values().stream()
                .filter(substationGeoData -> isCompatible(substationGeoData, entry))
                .map(SubstationGeoData::getId).collect(Collectors.toSet());

            if (clutteredSubstationsIds.size() > DefaultSubstationGeoParameter.DECLUTTERING_THRESHOLD) {
                calculateDefaultSubstationGeoDataRecursively(substationsGeoData, clutteredSubstationsIds, new HashSet<>(clutteredSubstationsIds), new DefaultSubstationGeoParameter(0.0, 0.0, entry.getValue().getCoordinate()), sortedNeighbours);
            }
        }
        stopWatch.stop();
        LOGGER.info("Default substations geo data calculated in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
    }

    private static boolean isCompatible(SubstationGeoData substationGeoData, Entry<String, SubstationGeoData> entry) {
        if (substationGeoData.getCountry() == null) {
            return false;
        }
        if (!substationGeoData.getCountry().equals(entry.getValue().getCountry())) {
            return false;
        }
        if (Math.abs(substationGeoData.getCoordinate().getLatitude() - entry.getValue().getCoordinate().getLatitude()) >= 1.0) {
            return false;
        }
        if (Math.abs(substationGeoData.getCoordinate().getLongitude() - entry.getValue().getCoordinate().getLongitude()) >= 1.0) {
            return false;
        }

        return true;
    }

    private DefaultSubstationGeoParameter calculateDefaultSubstationGeoDataRecursively(Map<String, SubstationGeoData> substationsGeoData, Set<String> substationsToProcess, Set<String> remainingSubstations, DefaultSubstationGeoParameter initialGeoParameters, Map<String, Set<String>> sortedNeighbours) {
        DefaultSubstationGeoParameter geoParameters = initialGeoParameters;
        for (String substationToProcess : substationsToProcess) {
            if (remainingSubstations.contains(substationToProcess)) {
                substationsGeoData.get(substationToProcess).setCoordinate(geoParameters.getCurrentCoordinate());
                remainingSubstations.remove(substationToProcess);
                geoParameters.incrementDefaultSubstationGeoParameters();
            }

            if (sortedNeighbours.get(substationToProcess) != null) {
                Set<String> neighbours = sortedNeighbours.get(substationToProcess).stream()
                        .filter(remainingSubstations::contains)
                        .collect(Collectors.toSet());

                for (String neighbour : neighbours) {
                    substationsGeoData.get(neighbour).setCoordinate(geoParameters.getCurrentCoordinate());
                    remainingSubstations.remove(neighbour);
                    geoParameters.incrementDefaultSubstationGeoParameters();
                }
                geoParameters = calculateDefaultSubstationGeoDataRecursively(substationsGeoData, neighbours, remainingSubstations, geoParameters, sortedNeighbours);
            }
        }
        return geoParameters;
    }

    private static int neighboursComparator(Network network, Set<String> neighbors1, Set<String> neighbors2) {
        return neighbors2.stream().map(s -> network.getSubstation(s).getExtension(SubstationPosition.class)).filter(Objects::nonNull).collect(Collectors.toSet()).size() -
                neighbors1.stream().map(s -> network.getSubstation(s).getExtension(SubstationPosition.class)).filter(Objects::nonNull).collect(Collectors.toSet()).size();
    }

    enum Step {
        ONE,
        TWO
    }

    private void calculateMissingGeoData(Network network, Map<String, Set<String>> sortedNeighbours, Map<String, SubstationGeoData> substationsGeoData,
                                         Set<String> substationsToCalculate) {
        StopWatch stopWatch = StopWatch.createStarted();
        // STEP 1
        step(Step.ONE, network, sortedNeighbours, substationsGeoData, substationsToCalculate);

        // STEP 2
        if (!substationsToCalculate.isEmpty()) {
            step(Step.TWO, network, sortedNeighbours, substationsGeoData, substationsToCalculate);
        }

        stopWatch.stop();
        LOGGER.info("Missing substation geo data calculated in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
    }

    private double nextNeighborhoodOffset(double neighborhoodOffset) {
        return neighborhoodOffset > 0 ? (neighborhoodOffset * -1) : (neighborhoodOffset * -1) + CALCULATED_SUBSTATION_OFFSET;
    }

    private void step(Step step, Network network, Map<String, Set<String>> sortedNeighbours, Map<String, SubstationGeoData> substationsGeoData,
                      Set<String> substationsToCalculate) {

        Map<Set<String>, Double> calculatedSubstationsOffset = new HashMap<>();
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            int calculated = 0;
            for (Iterator<String> it = substationsToCalculate.iterator(); it.hasNext();) {
                String substationId = it.next();
                Set<String> neighbours = sortedNeighbours.get(substationId);
                double neighborhoodOffset = calculatedSubstationsOffset.get(neighbours) != null ? nextNeighborhoodOffset(calculatedSubstationsOffset.get(neighbours)) : 0;

                // centroid calculation
                Substation substation = network.getSubstation(substationId);
                SubstationGeoData substationGeoData = calculateCentroidGeoData(substation, neighbours, step, substationsGeoData, neighborhoodOffset);

                if (substationGeoData != null) {
                    calculated++;
                    substationsGeoData.put(substationId, substationGeoData);
                    calculatedSubstationsOffset.put(neighbours, neighborhoodOffset);
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

    private static Coordinate getAverageCoordinate(List<SubstationGeoData> neighboursGeoData, double neighborhoodOffset) {
        double lat = neighboursGeoData.stream().mapToDouble(n -> n.getCoordinate().getLatitude()).average().orElseThrow(IllegalStateException::new);
        double lon = neighboursGeoData.stream().mapToDouble(n -> n.getCoordinate().getLongitude()).average().orElseThrow(IllegalStateException::new);

        if (neighborhoodOffset != 0) {
            double latDifference = neighboursGeoData.stream().mapToDouble(n -> n.getCoordinate().getLatitude()).max().orElseThrow(IllegalStateException::new) - neighboursGeoData.stream().mapToDouble(n -> n.getCoordinate().getLatitude()).min().orElseThrow(IllegalStateException::new);
            double lonDifference = neighboursGeoData.stream().mapToDouble(n -> n.getCoordinate().getLongitude()).max().orElseThrow(IllegalStateException::new) - neighboursGeoData.stream().mapToDouble(n -> n.getCoordinate().getLongitude()).min().orElseThrow(IllegalStateException::new);

            if (latDifference > lonDifference) {
                lon += neighborhoodOffset;
            } else {
                lat += neighborhoodOffset;
            }
        }
        return new Coordinate(lat, lon);
    }

    private SubstationGeoData calculateCentroidGeoData(Substation substation, Set<String> neighbours, Step step,
                                                              Map<String, SubstationGeoData> substationsGeoData, double neighborhoodOffset) {
        // get neighbours geo data
        List<SubstationGeoData> neighboursGeoData = neighbours.stream().map(substationsGeoData::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String substationCountry = substation.getNullableCountry() != null ? substation.getNullableCountry().name() : null;
        SubstationGeoData defaultSubstationGeoData = defaultSubstationsGeoData.get(substationCountry);

        Coordinate coordinate = null;
        if (neighboursGeoData.size() > 1) {
            // if no neighbour found in the same country, locate the substation to a default position in its country
            if (neighboursGeoData.stream().noneMatch(n -> Objects.equals(n.getCountry(), substation.getNullableCountry())) &&
                    defaultSubstationGeoData != null) {
                neighboursGeoData = Collections.singletonList(defaultSubstationGeoData);
            }
            coordinate = getAverageCoordinate(neighboursGeoData, neighborhoodOffset);
        } else if (neighboursGeoData.size() == 1 && step == Step.TWO) {
            // if neighbour not in the same country, locate the substation to a default position in its country
            if (!Objects.equals(neighboursGeoData.get(0).getCountry(), substation.getNullableCountry()) && defaultSubstationGeoData != null) {
                coordinate = defaultSubstationGeoData.getCoordinate();
            } else {
                double lat = neighboursGeoData.get(0).getCoordinate().getLatitude() - 0.002; // 1° correspond à 111KM
                double lon = neighboursGeoData.get(0).getCoordinate().getLongitude() - 0.007; // 1° correspond à 111.11 cos(1) = 60KM
                coordinate = new Coordinate(lat, lon);
            }
        } else if (neighboursGeoData.isEmpty() && step == Step.TWO && defaultSubstationGeoData != null) {
            // if still no neighbour found at step TWO, try to locate the substation to a default position in its country
            coordinate = defaultSubstationGeoData.getCoordinate();
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

    @Transactional(readOnly = true)
    public List<LineGeoData> getLines(Network network, List<String> linesIds) {
        List<String> escapedIds = linesIds.stream().map(LogUtils::sanitizeParam).collect(Collectors.toList());
        LOGGER.info("Loading substations geo data for substations with ids {} of network '{}'", StringUtils.join(escapedIds, ", "), network.getId());

        Objects.requireNonNull(network);

        StopWatch stopWatch = StopWatch.createStarted();

        List<Line> lines = new ArrayList<>();

        linesIds.stream().forEach(id -> lines.add(network.getLine(id)));

        // read lines from DB
        Map<String, LineGeoData> linesGeoDataDb = lineRepository.findAllById(linesIds).stream().collect(Collectors.toMap(LineEntity::getId, this::toDto));

        List<String> substations = new ArrayList<>();
        lines.forEach(line -> {
            String s1 = line.getTerminal1().getVoltageLevel().getSubstation().orElseThrow().getId();
            String s2 = line.getTerminal2().getVoltageLevel().getSubstation().orElseThrow().getId();
            substations.add(s1);
            substations.add(s2);
        });

        Map<String, SubstationGeoData> substationGeoDataDb = getSubstationMap(network, substations);
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
        return mapper.readValue(coordinates, new TypeReference<List<Coordinate>>() { });
    }

    private Map<String, SubstationGeoData> getSubstationMap(Network network, Set<Country> countries) {
        return getSubstations(network, countries).stream().collect(Collectors.toMap(SubstationGeoData::getId, Function.identity()));
    }

    private Map<String, SubstationGeoData> getSubstationMap(Network network, List<String> substationsIds) {
        return getSubstations(network, substationsIds).stream().collect(Collectors.toMap(SubstationGeoData::getId, Function.identity()));
    }
}
