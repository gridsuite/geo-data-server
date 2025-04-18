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
import com.google.common.collect.Streams;
import com.powsybl.commons.exceptions.UncheckedInterruptedException;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.Coordinate;
import com.powsybl.iidm.network.extensions.SubstationPosition;
import com.powsybl.ws.commons.LogUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Precision;
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
import org.gridsuite.geodata.server.repositories.LineEntity;
import org.gridsuite.geodata.server.repositories.LineRepository;
import org.gridsuite.geodata.server.repositories.SubstationEntity;
import org.gridsuite.geodata.server.repositories.SubstationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.round;
import static org.gridsuite.geodata.server.GeoDataException.Type.FAILED_LINES_LOADING;
import static org.gridsuite.geodata.server.GeoDataException.Type.FAILED_SUBSTATIONS_LOADING;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 */
@Service
public class GeoDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoDataService.class);

    static final double CALCULATED_SUBSTATION_OFFSET = 0.005;

    private final ObjectMapper mapper;

    @Value("${network-geo-data.iterations:50}")
    private int maxIterations;

    private final SubstationRepository substationRepository;

    private final LineRepository lineRepository;

    private final DefaultSubstationGeoDataByCountry defaultSubstationsGeoData;

    private final GeoDataExecutionService geoDataExecutionService;
    private final int geoDataRoundPrecision;

    public GeoDataService(ObjectMapper mapper,
                          SubstationRepository substationRepository,
                          LineRepository lineRepository,
                          DefaultSubstationGeoDataByCountry defaultSubstationsGeoData,
                          GeoDataExecutionService geoDataExecutionService,
                          @Value("${geo_data_round_precision}") int geoDataRoundPrecision) {
        this.mapper = mapper;
        this.substationRepository = substationRepository;
        this.lineRepository = lineRepository;
        this.defaultSubstationsGeoData = defaultSubstationsGeoData;
        this.geoDataExecutionService = geoDataExecutionService;
        this.geoDataRoundPrecision = geoDataRoundPrecision;
    }

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

        LOGGER.info("{} substations read from DB in {} ms", substationsGeoDataDB.size(), stopWatch.getTime(TimeUnit.MILLISECONDS));

        return substationsGeoDataDB;
    }

    List<SubstationGeoData> getSubstationsByCountries(Network network, Set<Country> countries) {
        LOGGER.info("Loading substations geo data for countries {} of network '{}'", countries, network.getId());

        Objects.requireNonNull(network);
        Objects.requireNonNull(countries);

        // get substations from the db
        Map<String, SubstationGeoData> substationsGeoDataDb = readSubstationGeoDataFromDb(countries);

        // filter substation by countries
        List<Substation> substations = network.getSubstationStream()
                .filter(s -> countries.isEmpty() || s.getCountry().filter(countries::contains).isPresent())
                .toList();

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

        long accuracyFactor = round(100 * (double) substationsGeoData.size() / (substationsToCalculate.size() + substationsGeoData.size()));
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

    List<SubstationGeoData> getSubstationsByIds(Network network, Set<String> substationIds) {
        String escapedIds = StringUtils.join(substationIds.stream().map(LogUtils::sanitizeParam).toList(), ", ");
        LOGGER.info("Loading substations geo data for substations with ids {} of network '{}'", escapedIds, network.getId());

        Objects.requireNonNull(network);

        StopWatch stopWatch = StopWatch.createStarted();

        Set<String> substationsToCalculate = new HashSet<>();

        Map<String, SubstationGeoData> geoDataForComputation = new HashMap<>();
        Map<String, Set<String>> neighboursBySubstationId = new HashMap<>();

        prepareGeoDataForComputation(network, geoDataForComputation, neighboursBySubstationId, substationsToCalculate, substationIds);

        //Calculated data are added to geoDataForComputation
        calculateMissingGeoData(network, neighboursBySubstationId, geoDataForComputation, substationsToCalculate);
        calculateDefaultSubstationsGeoData(geoDataForComputation, neighboursBySubstationId);

        //We remove linked substations from result - we only want requested ones
        geoDataForComputation.keySet().removeIf(key -> !substationIds.contains(key));

        LOGGER.info("Substations with given ids read/computed from DB in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
        //We return geo data found in the DB and the computed ones
        return geoDataForComputation.values().stream().toList();
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

            neighboursToBeTreated.forEach(neighbourId -> {
                if (geoDataForComputation.get(neighbourId) == null && !substationsToCalculate.contains(neighbourId)) {
                    substationsToCalculate.add(neighbourId);
                    substations.add(network.getSubstation(neighbourId));
                }
            });
            Map<String, Set<String>> newNeighbours = getNeighbours(substations);
            substations.forEach(substation -> allNeighbours.addAll(newNeighbours.get(substation.getId())));
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
                .toList();

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

        List<SubstationEntity> substationEntities = substationsGeoData.stream().map(s -> SubstationEntity.create(s, geoDataRoundPrecision)).toList();
        substationRepository.saveAll(substationEntities);
    }

    @SuppressWarnings("javasecurity:S5145")
    void saveLines(List<LineGeoData> linesGeoData) {
        LOGGER.info("Saving {} lines geo data", linesGeoData.size());

        try {
            List<LineEntity> linesEntities = new ArrayList<>(linesGeoData.size());
            for (LineGeoData l : linesGeoData) {
                List<Coordinate> fullCoordinates = Objects.requireNonNull(l.getCoordinates());
                // round the coordinates
                List<Coordinate> roundedCoordinates = fullCoordinates.stream().map(coordinate ->
                    new Coordinate(Precision.round(coordinate.getLatitude(), geoDataRoundPrecision),
                        Precision.round(coordinate.getLongitude(), geoDataRoundPrecision))).toList();
                String jsonCoords = mapper.writeValueAsString(roundedCoordinates);
                if (l.getCountry1() == l.getCountry2()) {
                    linesEntities.add(LineEntity.create(l, true, jsonCoords));
                } else {
                    linesEntities.add(LineEntity.create(l, true, jsonCoords));
                    linesEntities.add(LineEntity.create(l, false, jsonCoords));
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
     * <p>
     * returns null when the substations at the end of the line are missing.
     */
    private LineGeoData getLineGeoDataWithEndSubstations(Map<String, LineGeoData> linesGeoDataDb, Map<String, SubstationGeoData> substationGeoDataDb, String lineId, Substation substation1, Substation substation2) {
        LineGeoData geoData = linesGeoDataDb.get(lineId);
        SubstationGeoData substation1GeoData = substationGeoDataDb.get(substation1.getId());
        SubstationGeoData substation2GeoData = substationGeoDataDb.get(substation2.getId());

        // TODO: we return null here even if we have line data
        // because the method is called "withEndSubstations"...
        // We could refactor this in separate methods if we ever have a
        // need to return the line in the network order without the substations
        if (substation1GeoData == null || substation2GeoData == null) {
            LOGGER.error("line {} has substations with unknown gps positions({}={}, {}={})", lineId,
                    substation1.getId(), substation1GeoData,
                    substation2.getId(), substation2GeoData);
            return null;
        }

        Coordinate substation1Coordinate = substation1GeoData.getCoordinate();
        Coordinate substation2Coordinate = substation2GeoData.getCoordinate();
        if (geoData == null || geoData.getCoordinates().isEmpty() || geoData.getSubstationStart().isEmpty() && geoData.getSubstationEnd().isEmpty()) {
            return new LineGeoData(lineId, substation1.getNullableCountry(), substation2.getNullableCountry(), substation1.getId(), substation2.getId(),
                List.of(substation1Coordinate, substation2Coordinate));
        } else if (emptyOrEquals(geoData.getSubstationStart(), substation2.getId()) && emptyOrEquals(geoData.getSubstationEnd(), substation1.getId())) {
            return new LineGeoData(lineId, substation1.getNullableCountry(), substation2.getNullableCountry(),

                geoData.getSubstationStart(),
                geoData.getSubstationEnd(),
                addCoordinates(substation1Coordinate, geoData.getCoordinates(), substation2Coordinate, true));
        } else if (emptyOrEquals(geoData.getSubstationStart(), substation1.getId()) && emptyOrEquals(geoData.getSubstationEnd(), substation2.getId())) {
            return new LineGeoData(lineId, substation1.getNullableCountry(), substation2.getNullableCountry(),
                geoData.getSubstationStart(),
                geoData.getSubstationEnd(),
                addCoordinates(substation1Coordinate, geoData.getCoordinates(), substation2Coordinate, false));
        }

        LOGGER.error("line {} has different substations set in geographical data ({}, {}) and network data ({}, {})", lineId, geoData.getSubstationStart(), geoData.getSubstationEnd(), substation1.getId(), substation2.getId());
        return new LineGeoData(lineId, substation1.getNullableCountry(), substation2.getNullableCountry(), substation1.getId(), substation2.getId(),
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

    List<LineGeoData> getLinesByCountries(Network network, Set<Country> countries) {
        LOGGER.info("Loading lines geo data for countries {} of network '{}'", countries, network.getId());

        Objects.requireNonNull(network);
        Objects.requireNonNull(countries);

        StopWatch stopWatch = StopWatch.createStarted();

        // read lines from DB
        Map<String, Pair<Substation, Substation>> mapSubstationsByLine =
                Streams.concat(network.getLineStream(), network.getTieLineStream(), network.getHvdcLineStream())
                        .collect(Collectors.toMap(Identifiable::getId, this::getSubstations));

        Map<String, LineGeoData> linesGeoDataDb = lineRepository.findAllById(mapSubstationsByLine.keySet()).stream().collect(Collectors.toMap(LineEntity::getId, this::toDto));

        // we also want the destination substation (so we add the neighbouring country)
        Set<Country> countryAndNextTo = mapSubstationsByLine.entrySet().stream().flatMap(entry ->
             Stream.of(entry.getValue().getLeft(), entry.getValue().getRight()).map(Substation::getNullableCountry).filter(Objects::nonNull)).collect(Collectors.toSet());

        Map<String, SubstationGeoData> substationGeoDataDb = getSubstationMapByCountries(network, countryAndNextTo);
        List<LineGeoData> geoData = new ArrayList<>();

        mapSubstationsByLine.forEach((key, value) -> {
            LineGeoData geo = getLineGeoDataWithEndSubstations(linesGeoDataDb, substationGeoDataDb, key, value.getLeft(), value.getRight());
            if (geo != null) {
                geoData.add(geo);
            }
        });

        LOGGER.info("{} lines read from DB in {} ms", linesGeoDataDb.size(), stopWatch.getTime(TimeUnit.MILLISECONDS));

        return geoData;
    }

    private Pair<Substation, Substation> getSubstations(Identifiable<?> identifiable) {
        return switch (identifiable.getType()) {
            case LINE -> Pair.of(((Line) identifiable).getTerminal1().getVoltageLevel().getSubstation().orElseThrow(),
                ((Line) identifiable).getTerminal2().getVoltageLevel().getSubstation().orElseThrow());
            case TIE_LINE ->
                Pair.of(((TieLine) identifiable).getDanglingLine1().getTerminal().getVoltageLevel().getSubstation().orElseThrow(),
                    ((TieLine) identifiable).getDanglingLine2().getTerminal().getVoltageLevel().getSubstation().orElseThrow());
            case HVDC_LINE ->
                Pair.of(((HvdcLine) identifiable).getConverterStation1().getTerminal().getVoltageLevel().getSubstation().orElseThrow(),
                    ((HvdcLine) identifiable).getConverterStation2().getTerminal().getVoltageLevel().getSubstation().orElseThrow());
            default -> throw new IllegalStateException("Unexpected equipment type:" + identifiable.getType());
        };
    }

    @Transactional(readOnly = true)
    public List<SubstationGeoData> getSubstationsData(Network network, Set<Country> countrySet, List<String> substationIds) {
        CompletableFuture<List<SubstationGeoData>> substationGeoDataFuture = geoDataExecutionService.supplyAsync(() -> {
            if (substationIds != null) {
                if (!countrySet.isEmpty()) {
                    LOGGER.warn("Countries will not be taken into account to filter substation position.");
                }
                return getSubstationsByIds(network, new HashSet<>(substationIds));
            } else {
                return getSubstationsByCountries(network, countrySet);
            }
        });
        try {
            return substationGeoDataFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        } catch (Exception e) {
            throw new GeoDataException(FAILED_SUBSTATIONS_LOADING, e);
        }
    }

    @Transactional(readOnly = true)
    public List<LineGeoData> getLinesData(Network network, Set<Country> countrySet, List<String> lineIds) {
        CompletableFuture<List<LineGeoData>> lineGeoDataFuture = geoDataExecutionService.supplyAsync(() -> {
            if (lineIds != null) {
                if (!countrySet.isEmpty()) {
                    LOGGER.warn("Countries will not be taken into account to filter line position.");
                }
                return getLinesByIds(network, new HashSet<>(lineIds));
            } else {
                return getLinesByCountries(network, countrySet);
            }
        });
        try {
            return lineGeoDataFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        } catch (Exception e) {
            throw new GeoDataException(FAILED_LINES_LOADING, e);
        }
    }

    List<LineGeoData> getLinesByIds(Network network, Set<String> linesIds) {
        String escapedIds = StringUtils.join(linesIds.stream().map(LogUtils::sanitizeParam).toList(), ", ");
        LOGGER.info("Loading lines geo data for lines with ids {} of network '{}'", escapedIds, network.getId());

        Objects.requireNonNull(network);

        StopWatch stopWatch = StopWatch.createStarted();

        List<Line> lines = new ArrayList<>();

        linesIds.forEach(id -> lines.add(network.getLine(id)));

        // read lines from DB
        Map<String, LineGeoData> linesGeoDataDb = lineRepository.findAllById(linesIds).stream().collect(Collectors.toMap(LineEntity::getId, this::toDto));

        Set<String> substations = new HashSet<>();
        lines.forEach(line -> {
            String s1 = line.getTerminal1().getVoltageLevel().getSubstation().orElseThrow().getId();
            String s2 = line.getTerminal2().getVoltageLevel().getSubstation().orElseThrow().getId();
            substations.add(s1);
            substations.add(s2);
        });

        Map<String, SubstationGeoData> substationGeoDataDb = getSubstationMapByIds(network, substations);
        List<LineGeoData> lineGeoData = lines.stream().map(line -> getLineGeoDataWithEndSubstations(linesGeoDataDb, substationGeoDataDb, line.getId(),
                line.getTerminal1().getVoltageLevel().getSubstation().orElseThrow(),
                line.getTerminal2().getVoltageLevel().getSubstation().orElseThrow()))
                .filter(Objects::nonNull).toList();
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
        return mapper.readValue(coordinates, new TypeReference<>() {
        });
    }

    private Map<String, SubstationGeoData> getSubstationMapByCountries(Network network, Set<Country> countries) {
        return getSubstationsByCountries(network, countries).stream().collect(Collectors.toMap(SubstationGeoData::getId, Function.identity()));
    }

    private Map<String, SubstationGeoData> getSubstationMapByIds(Network network, Set<String> substationsIds) {
        return getSubstationsByIds(network, substationsIds).stream().collect(Collectors.toMap(SubstationGeoData::getId, Function.identity()));
    }
}
