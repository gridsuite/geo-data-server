/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.Coordinate;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.NoEquipmentNetworkFactory;
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
import org.gridsuite.geodata.server.repositories.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@SpringBootTest(classes = GeoDataApplication.class)
class GeoDataServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubstationRepository substationRepository;

    @Autowired
    private LineRepository lineRepository;

    @Autowired
    private GeoDataService geoDataService;

    @Autowired
    private DefaultSubstationGeoDataByCountry defaultSubstationsGeoData;

    @BeforeEach
    void setUp() throws Exception {
        List<SubstationEntity> substationEntities = new ArrayList<>();

        substationEntities.add(SubstationEntity.builder()
                .id("P1")
                .country("FR")
                .coordinate(new CoordinateEmbeddable(1, 1))
                .build());

        substationEntities.add(SubstationEntity.builder()
                .id("P2")
                .country("FR")
                .coordinate(new CoordinateEmbeddable(3, 1))
                .build());

        substationEntities.add(SubstationEntity.builder()
                .id("P3")
                .country("FR")
                .coordinate(new CoordinateEmbeddable(2, 7))
                .build());

        substationEntities.add(SubstationEntity.builder()
                .id("P12")
                .country("FR")
                .coordinate(new CoordinateEmbeddable(10, 20))
                .build());

        substationRepository.saveAll(substationEntities);

        List<LineEntity> lineEntities = new ArrayList<>();

        lineEntities.add(LineEntity.builder()
                .id("NHV2_NHV5")
                .country("BE")
                .otherCountry("FR")
                .coordinates(objectMapper.writeValueAsString(Arrays.asList(new CoordinateEmbeddable(2, 1), new CoordinateEmbeddable(2.5, 1), new CoordinateEmbeddable(3, 1))))
                .substationStart("P2")
                .substationEnd("P5")
                .build());

        lineEntities.add(LineEntity.builder()
                .id("NHV2_NHV3")
                .country("FR")
                .otherCountry("FR")
                .substationStart("P2")
                .substationEnd("P3")
                .coordinates(objectMapper.writeValueAsString(Arrays.asList(new CoordinateEmbeddable(3, 1), new CoordinateEmbeddable(5, 6), new CoordinateEmbeddable(2, 7))))
                .build());

        lineEntities.add(LineEntity.builder()
            .id("NHV2_NHV3_inverted")
            .country("FR")
            .otherCountry("FR")
            .substationStart("P2")
            .substationEnd("P3")
            .coordinates(objectMapper.writeValueAsString(Arrays.asList(new CoordinateEmbeddable(3, 1), new CoordinateEmbeddable(5, 6), new CoordinateEmbeddable(2, 7))))
            .build());

        lineEntities.add(LineEntity.builder()
            .id("WRONG_CONFIG")
            .country("FR")
            .otherCountry("FR")
            .substationStart("OOUPS")
            .substationEnd("P3")
            .coordinates(objectMapper.writeValueAsString(Arrays.asList(new CoordinateEmbeddable(3, 1), new CoordinateEmbeddable(5, 6), new CoordinateEmbeddable(2, 7))))
            .build());

        lineRepository.saveAll(lineEntities);
    }

    @AfterEach
    void cleanDb() {
        lineRepository.deleteAll();
        substationRepository.deleteAll();
    }

    private static LineGeoData getFromList(List<LineGeoData> list, String id) {
        Optional<LineGeoData> res = list.stream().filter(l -> l.getId().equals(id)).findAny();
        assertTrue(res.isPresent());
        return res.get();
    }

    @Test
    void test() {
        Network network = createGeoDataNetwork();
        List<SubstationGeoData> substationsGeoData = geoDataService.getSubstationsByCountries(network, new HashSet<>(Collections.singletonList(Country.FR)));

        assertEquals(4, substationsGeoData.size());
        assertEquals(2, substationsGeoData.stream().filter(s -> s.getId().equals("P4")).toList().get(0).getCoordinate().getLatitude(), 0);
        assertEquals(3, substationsGeoData.stream().filter(s -> s.getId().equals("P4")).toList().get(0).getCoordinate().getLongitude(), 0);

        List<LineGeoData> linesGeoData = geoDataService.getLinesByCountries(network, new HashSet<>(List.of(Country.FR)));

        assertEquals(15, linesGeoData.size());
        assertEquals(2, getFromList(linesGeoData, "NHV1_NHV2_1").getCoordinates().size()); // line with no coordinate, so [substation1, substation2]
        List<Coordinate> lineNHV2 = getFromList(linesGeoData, "NHV2_NHV3").getCoordinates();
        List<Coordinate> lineNHV3 = new ArrayList<>(getFromList(linesGeoData, "NHV2_NHV3_inverted").getCoordinates());
        Collections.reverse(lineNHV3);
        assertEquals(lineNHV2, lineNHV3); // should be the same path
        assertEquals(5, lineNHV2.size()); // line with 3 coordinate, so [substation1, c1, c2, c3, substation2]

        List<Coordinate> wrong = getFromList(linesGeoData, "WRONG_CONFIG").getCoordinates();
        assertEquals(2, wrong.size()); // wrong substation origin/end, so only (sub1, sub2)

        List<SubstationGeoData> substationsGeoData2 = geoDataService.getSubstationsByCountries(network, new HashSet<>(ImmutableList.of(Country.FR, Country.BE)));

        assertEquals(5, substationsGeoData2.size());
        assertEquals(2, substationsGeoData2.stream().filter(s -> s.getId().equals("P4")).toList().get(0).getCoordinate().getLatitude(), 0);
        assertEquals(3, substationsGeoData2.stream().filter(s -> s.getId().equals("P4")).toList().get(0).getCoordinate().getLongitude(), 0);

        assertEquals(4, substationsGeoData2.stream().filter(s -> s.getId().equals("P5")).toList().get(0).getCoordinate().getLatitude(), 0);
        assertEquals(8, substationsGeoData2.stream().filter(s -> s.getId().equals("P5")).toList().get(0).getCoordinate().getLongitude(), 0);

        List<SubstationGeoData> substationsGeoData3 = geoDataService.getSubstationsByCountries(network, new HashSet<>(Collections.singletonList(Country.BE)));

        assertEquals(1, substationsGeoData3.size());

        assertEquals(4, substationsGeoData3.stream().filter(s -> s.getId().equals("P5")).toList().get(0).getCoordinate().getLatitude(), 0);
        assertEquals(8, substationsGeoData3.stream().filter(s -> s.getId().equals("P5")).toList().get(0).getCoordinate().getLongitude(), 0);

        List<SubstationGeoData> substationsGeoData4 = geoDataService.getSubstationsByCountries(network, new HashSet<>(Collections.singletonList(Country.DE)));

        assertEquals(2, substationsGeoData4.size());

        SubstationGeoData p6 = substationsGeoData4.stream().filter(s -> s.getId().equals("P6")).toList().get(0);
        SubstationGeoData p7 = substationsGeoData4.stream().filter(s -> s.getId().equals("P7")).toList().get(0);
        assertEquals(0.002, Math.abs(p6.getCoordinate().getLatitude()) - p7.getCoordinate().getLatitude(), 0.0001);
        assertEquals(0.007, Math.abs(p6.getCoordinate().getLongitude()) - p7.getCoordinate().getLongitude(), 0.0001);

        List<SubstationGeoData> substationsGeoData5 = geoDataService.getSubstationsByCountries(network, new HashSet<>(ImmutableList.of(Country.DE, Country.BE)));

        assertEquals(3, substationsGeoData5.size());

        assertEquals(4, substationsGeoData5.stream().filter(s -> s.getId().equals("P5")).toList().get(0).getCoordinate().getLatitude(), 0);
        assertEquals(8, substationsGeoData5.stream().filter(s -> s.getId().equals("P5")).toList().get(0).getCoordinate().getLongitude(), 0);
        assertEquals(8, substationsGeoData5.stream().filter(s -> s.getId().equals("P6")).toList().get(0).getCoordinate().getLatitude(), 0.002);
        assertEquals(12, substationsGeoData5.stream().filter(s -> s.getId().equals("P6")).toList().get(0).getCoordinate().getLongitude(), 0.007);
    }

    @Test
    void testCgmesCase() {
        Network network = createCgmesGeoDataNetwork();

        List<SubstationGeoData> substationsGeoData = geoDataService.getSubstationsByCountries(network, new HashSet<>(Collections.singletonList(Country.FR)));

        assertEquals(2, substationsGeoData.size());
        assertEquals(1, substationsGeoData.stream().filter(s -> s.getId().equals("SubstationS1")).toList().get(0).getCoordinate().getLatitude(), 0);
        assertEquals(1, substationsGeoData.stream().filter(s -> s.getId().equals("SubstationS1")).toList().get(0).getCoordinate().getLongitude(), 0);
        assertEquals(3, substationsGeoData.stream().filter(s -> s.getId().equals("SubstationS2")).toList().get(0).getCoordinate().getLatitude(), 0);
        assertEquals(1, substationsGeoData.stream().filter(s -> s.getId().equals("SubstationS2")).toList().get(0).getCoordinate().getLongitude(), 0);
    }

    @Test
    void testNonExisting() {
        Network network = EurostagTutorialExample1Factory.create();
        Substation notexistsub1 = network.newSubstation()
                .setId("NOTEXISTSUB1")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();
        VoltageLevel notexistvl1 = notexistsub1.newVoltageLevel()
                .setId("NOTEXISTVL1")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus notexistbus1 = notexistvl1.getBusBreakerView().newBus()
                .setId("NOTEXISTBUS1")
                .add();

        Substation notexistsub2 = network.newSubstation()
                .setId("NOTEXISTSUB2")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();
        VoltageLevel notexistvl2 = notexistsub2.newVoltageLevel()
                .setId("NOTEXISTVL2")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        Bus notexistbus2 = notexistvl2.getBusBreakerView().newBus()
                .setId("NOTEXISTBUS2")
                .add();

        Line notexistline = network.newLine()
                .setId("NOTEXISTLINE1")
                .setVoltageLevel1(notexistvl1.getId())
                .setBus1(notexistbus1.getId())
                .setConnectableBus1(notexistbus1.getId())
                .setVoltageLevel2(notexistvl2.getId())
                .setBus2(notexistbus2.getId())
                .setConnectableBus2(notexistbus2.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();
        List<SubstationGeoData> substationsGeoData = geoDataService.getSubstationsByCountries(network, new HashSet<>(Collections.singletonList(Country.FR)));
        assertFalse(substationsGeoData.stream().anyMatch(Objects::isNull), "Must not contain nulls");
        assertFalse(substationsGeoData.stream().anyMatch(s -> notexistsub1.getId().equals(s.getId())),
                "Must not contain unknown substation " + notexistsub1.getId());
        assertFalse(substationsGeoData.stream().anyMatch(s -> notexistsub2.getId().equals(s.getId())),
                "Must not contain unknown substation " + notexistsub2.getId());

        List<LineGeoData> linesGeoData = geoDataService.getLinesByCountries(network, new HashSet<>(Collections.singletonList(Country.FR)));
        assertFalse(linesGeoData.stream().anyMatch(Objects::isNull), "Must not contain nulls");
        assertFalse(linesGeoData.stream().anyMatch(s -> notexistline.getId().equals(s.getId())),
                "Must not contain unknown lines " + notexistline.getId());
    }

    @Test
    void testSimilarNeighborhoodOffset() {
        Network network = EurostagTutorialExample1Factory.create();
        Substation p4 = network.newSubstation()
                .setId("P4")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();
        VoltageLevel vl4 = p4.newVoltageLevel()
                .setId("VLHV4")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl4.getBusBreakerView().newBus()
                .setId("NHV4")
                .add();

        network.newLine()
                .setId("LINE1_4")
                .setVoltageLevel1("VLHV1")
                .setBus1("NHV1")
                .setConnectableBus1("NHV1")
                .setVoltageLevel2("VLHV4")
                .setBus2("NHV4")
                .setConnectableBus2("NHV4")
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        network.newLine()
                .setId("LINE2_4")
                .setVoltageLevel1("VLHV2")
                .setBus1("NHV2")
                .setConnectableBus1("NHV2")
                .setVoltageLevel2("VLHV4")
                .setBus2("NHV4")
                .setConnectableBus2("NHV4")
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        Substation p5 = network.newSubstation()
                .setId("P5")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();
        VoltageLevel vl5 = p5.newVoltageLevel()
                .setId("VLHV5")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl5.getBusBreakerView().newBus()
                .setId("NHV5")
                .add();

        network.newLine()
                .setId("LINE1_5")
                .setVoltageLevel1("VLHV1")
                .setBus1("NHV1")
                .setConnectableBus1("NHV1")
                .setVoltageLevel2("VLHV5")
                .setBus2("NHV5")
                .setConnectableBus2("NHV5")
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        network.newLine()
                .setId("LINE2_5")
                .setVoltageLevel1("VLHV2")
                .setBus1("NHV2")
                .setConnectableBus1("NHV2")
                .setVoltageLevel2("VLHV5")
                .setBus2("NHV5")
                .setConnectableBus2("NHV5")
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        List<SubstationGeoData> substationsGeoData = geoDataService.getSubstationsByCountries(network, new HashSet<>(Collections.singletonList(Country.FR)));

        SubstationGeoData pgd4 = substationsGeoData.stream().filter(s -> s.getId().equals("P4")).toList().get(0);
        SubstationGeoData pgd5 = substationsGeoData.stream().filter(s -> s.getId().equals("P5")).toList().get(0);
        assertEquals(GeoDataService.CALCULATED_SUBSTATION_OFFSET, Math.abs(pgd4.getCoordinate().getLongitude() - pgd5.getCoordinate().getLongitude()), 0.0001);
    }

    @Test
    void testCalculatedDefaultSubstations() {
        Network network = EurostagTutorialExample1Factory.create();
        Substation p4 = network.newSubstation()
                .setId("P4")
                .setCountry(Country.BE)
                .setTso("RTE")
                .add();
        VoltageLevel vl4 = p4.newVoltageLevel()
                .setId("VLHV4")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl4.getBusBreakerView().newBus()
                .setId("NHV4")
                .add();

        Substation p5 = network.newSubstation()
                .setId("P5")
                .setCountry(Country.BE)
                .setTso("RTE")
                .add();
        VoltageLevel vl5 = p5.newVoltageLevel()
                .setId("VLHV5")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl5.getBusBreakerView().newBus()
                .setId("NHV5")
                .add();

        Substation p6 = network.newSubstation()
                .setId("P6")
                .setCountry(Country.BE)
                .setTso("RTE")
                .add();
        VoltageLevel vl6 = p6.newVoltageLevel()
                .setId("VLHV6")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        vl6.getBusBreakerView().newBus()
                .setId("NHV6")
                .add();

        network.newLine()
                .setId("LINE4_5")
                .setVoltageLevel1("VLHV4")
                .setBus1("NHV4")
                .setConnectableBus1("NHV4")
                .setVoltageLevel2("VLHV5")
                .setBus2("NHV5")
                .setConnectableBus2("NHV5")
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        network.newLine()
                .setId("LINE4_6")
                .setVoltageLevel1("VLHV4")
                .setBus1("NHV4")
                .setConnectableBus1("NHV4")
                .setVoltageLevel2("VLHV6")
                .setBus2("NHV6")
                .setConnectableBus2("NHV6")
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        //Extra substations in order to attain the trigger threshold
        network.newSubstation()
                .setId("P7")
                .setCountry(Country.BE)
                .setTso("RTE")
                .add();

        network.newSubstation()
                .setId("P8")
                .setCountry(Country.BE)
                .setTso("RTE")
                .add();

        network.newSubstation()
            .setId("P9")
            .setCountry(Country.BE)
            .setTso("RTE")
            .add();

        network.newSubstation()
            .setId("P10")
            .setCountry(Country.DE)
            .setTso("RTE")
            .add();

        network.newSubstation()
            .setId("P11")
            .setCountry(Country.DE)
            .setTso("RTE")
            .add();

        Substation alienSubstation = network.newSubstation()
            .setId("sidious")
            .add();

        VoltageLevel alienVoltageLevel = alienSubstation.newVoltageLevel()
            .setId("agent")
            .setNominalV(380)
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .add();
        alienVoltageLevel.getBusBreakerView().newBus()
            .setId("schwartzy")
            .add();

        network.newLine()
            .setId("LINE4_O")
            .setVoltageLevel1("VLHV4")
            .setBus1("NHV4")
            .setConnectableBus1("NHV4")
            .setVoltageLevel2("agent")
            .setBus2("schwartzy")
            .setConnectableBus2("schwartzy")
            .setR(3.0)
            .setX(33.0)
            .setG1(0.0)
            .setB1(386E-6 / 2)
            .setG2(0.0)
            .setB2(386E-6 / 2)
            .add();

        List<SubstationGeoData> substationsGeoData = geoDataService.getSubstationsByCountries(network, Set.of(Country.BE));
        DefaultSubstationGeoParameter defaultSubstationGeoParameter = new DefaultSubstationGeoParameter(0.0, 0.0, defaultSubstationsGeoData.get("BE").getCoordinate());

        SubstationGeoData pg4 = substationsGeoData.stream().filter(s -> s.getId().equals("P4")).toList().get(0);
        SubstationGeoData pg5 = substationsGeoData.stream().filter(s -> s.getId().equals("P5")).toList().get(0);
        SubstationGeoData pg6 = substationsGeoData.stream().filter(s -> s.getId().equals("P6")).toList().get(0);

        assertEquals(defaultSubstationGeoParameter.getCurrentCoordinate(), pg4.getCoordinate());
        defaultSubstationGeoParameter.incrementDefaultSubstationGeoParameters();
        assertEquals(defaultSubstationGeoParameter.getCurrentCoordinate(), pg5.getCoordinate());
        defaultSubstationGeoParameter.incrementDefaultSubstationGeoParameters();
        assertEquals(defaultSubstationGeoParameter.getCurrentCoordinate(), pg6.getCoordinate());
    }

    @Test
    void testLineCoordinatesError() {
        LineEntity lineEntity = LineEntity.create(LineGeoData.builder()
                .id("idLine")
                .country1(Country.FR)
                .country2(Country.BE)
                .substationStart("substation1")
                .substationEnd("substation2")
                .build(), true, "coordinates_error");

        assertThrows(GeoDataException.class, () ->
            geoDataService.toDto(lineEntity));
    }

    private static Network createGeoDataNetwork() {
        Network network = EurostagTutorialExample1Factory.create();

        Substation p3 = network.newSubstation()
                .setId("P3")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();

        VoltageLevel vlhv3 = p3.newVoltageLevel()
                .setId("VLHV3")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        Bus nhv3 = vlhv3.getBusBreakerView().newBus()
                .setId("NHV3")
                .add();

        network.newLine()
                .setId("NHV1_NHV3")
                .setVoltageLevel1("VLHV1")
                .setBus1("NHV1")
                .setConnectableBus1("NHV1")
                .setVoltageLevel2(vlhv3.getId())
                .setBus2(nhv3.getId())
                .setConnectableBus2(nhv3.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        network.newLine()
                .setId("NHV2_NHV3")
                .setVoltageLevel1("VLHV2")
                .setBus1("NHV2")
                .setConnectableBus1("NHV2")
                .setVoltageLevel2(vlhv3.getId())
                .setBus2(nhv3.getId())
                .setConnectableBus2(nhv3.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        network.newLine()
            .setId("NHV2_NHV3_inverted")
            .setVoltageLevel1(vlhv3.getId())
            .setBus1(nhv3.getId())
            .setConnectableBus1(nhv3.getId())
            .setVoltageLevel2("VLHV2")
            .setBus2("NHV2")
            .setConnectableBus2("NHV2")
            .setR(3.0)
            .setX(33.0)
            .setG1(0.0)
            .setB1(386E-6 / 2)
            .setG2(0.0)
            .setB2(386E-6 / 2)
            .add();

        network.newLine()
            .setId("WRONG_CONFIG")
            .setVoltageLevel1(vlhv3.getId())
            .setBus1(nhv3.getId())
            .setConnectableBus1(nhv3.getId())
            .setVoltageLevel2("VLHV2")
            .setBus2("NHV2")
            .setConnectableBus2("NHV2")
            .setR(3.0)
            .setX(33.0)
            .setG1(0.0)
            .setB1(386E-6 / 2)
            .setG2(0.0)
            .setB2(386E-6 / 2)
            .add();

        Substation p4 = network.newSubstation()
                .setId("P4")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();

        VoltageLevel vlhv4 = p4.newVoltageLevel()
                .setId("VLHV4")
                .setNominalV(380.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        Bus nhv4 = vlhv4.getBusBreakerView().newBus()
                .setId("NHV4")
                .add();

        network.newLine()
                .setId("NHV1_NHV4")
                .setVoltageLevel1("VLHV1")
                .setBus1("NHV1")
                .setConnectableBus1("NHV1")
                .setVoltageLevel2(vlhv4.getId())
                .setBus2(nhv4.getId())
                .setConnectableBus2(nhv4.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        network.newLine()
                .setId("NHV2_NHV4")
                .setVoltageLevel1("VLHV2")
                .setBus1("NHV2")
                .setConnectableBus1("NHV2")
                .setVoltageLevel2(vlhv4.getId())
                .setBus2(nhv4.getId())
                .setConnectableBus2(nhv4.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        network.newLine()
                .setId("NHV3_NHV4")
                .setVoltageLevel1("VLHV3")
                .setBus1("NHV3")
                .setConnectableBus1("NHV3")
                .setVoltageLevel2(vlhv4.getId())
                .setBus2(nhv4.getId())
                .setConnectableBus2(nhv4.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        Substation p5 = network.newSubstation()
                .setId("P5")
                .setCountry(Country.BE)
                .setTso("RTE")
                .add();

        VoltageLevel vlhv5 = p5.newVoltageLevel()
                .setId("VLHV5")
                .setNominalV(380.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        Bus nhv5 = vlhv5.getBusBreakerView().newBus()
                .setId("NHV5")
                .add();

        network.newLine()
                .setId("NHV1_NHV5")
                .setVoltageLevel1("VLHV1")
                .setBus1("NHV1")
                .setConnectableBus1("NHV1")
                .setVoltageLevel2(vlhv5.getId())
                .setBus2(nhv5.getId())
                .setConnectableBus2(nhv5.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        network.newLine()
                .setId("NHV2_NHV5")
                .setVoltageLevel1("VLHV2")
                .setBus1("NHV2")
                .setConnectableBus1("NHV2")
                .setVoltageLevel2(vlhv5.getId())
                .setBus2(nhv5.getId())
                .setConnectableBus2(nhv5.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        Substation p6 = network.newSubstation()
                .setId("P6")
                .setCountry(Country.DE)
                .setTso("D4")
                .add();

        VoltageLevel vlhv6 = p6.newVoltageLevel()
                .setId("VLHV6")
                .setNominalV(380.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        Bus nhv6 = vlhv6.getBusBreakerView().newBus()
                .setId("NHV6")
                .add();

        Substation p7 = network.newSubstation()
                .setId("P7")
                .setCountry(Country.DE)
                .setTso("D4")
                .add();

        VoltageLevel vlhv7 = p7.newVoltageLevel()
                .setId("VLHV7")
                .setNominalV(380.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        Bus nhv7 = vlhv7.getBusBreakerView().newBus()
                .setId("NHV7")
                .add();

        network.newLine()
                .setId("NHV6_NHV7")
                .setVoltageLevel1("VLHV6")
                .setBus1("NHV6")
                .setConnectableBus1("NHV6")
                .setVoltageLevel2(vlhv7.getId())
                .setBus2(nhv7.getId())
                .setConnectableBus2(nhv7.getId())
                .setR(6.0)
                .setX(66.0)
                .setG1(0.0)
                .setB1(284E-6 / 2)
                .setG2(0.0)
                .setB2(288E-6 / 2)
                .add();

        network.newLine()
                .setId("NHV5_NHV6")
                .setVoltageLevel1("VLHV5")
                .setBus1("NHV5")
                .setConnectableBus1("NHV5")
                .setVoltageLevel2(vlhv6.getId())
                .setBus2(nhv6.getId())
                .setConnectableBus2(nhv6.getId())
                .setR(6.0)
                .setX(66.0)
                .setG1(0.0)
                .setB1(284E-6 / 2)
                .setG2(0.0)
                .setB2(288E-6 / 2)
                .add();

        vlhv7.newVscConverterStation()
            .setId("C1")
            .setName("Converter1")
            .setConnectableBus("NHV7")
            .setBus("NHV7")
            .setLossFactor(1.1f)
            .setVoltageRegulatorOn(false)
            .setReactivePowerSetpoint(100.)
            .add();
        vlhv6.newVscConverterStation()
            .setId("C2")
            .setName("Converter2")
            .setConnectableBus("NHV6")
            .setBus("NHV6")
            .setLossFactor(1.1f)
            .setReactivePowerSetpoint(123)
            .setVoltageRegulatorOn(false)
            .add();
        network.newHvdcLine()
            .setId("L")
            .setName("HVDC")
            .setConverterStationId1("C1")
            .setConverterStationId2("C2")
            .setR(1)
            .setNominalV(400)
            .setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER)
            .setMaxP(300.0)
            .setActivePowerSetpoint(280)
            .add();

        vlhv4.newDanglingLine()
            .setId("DL1")
            .setBus("NHV4")
            .setP0(0.0)
            .setQ0(0.0)
            .setR(1.)
            .setX(1.)
            .setB(1.)
            .setG(1.)
            .setPairingKey("ucte")
            .add();
        vlhv5.newDanglingLine()
            .setId("DL2")
            .setBus("NHV5")
            .setP0(0.0)
            .setQ0(0.0)
            .setR(1.)
            .setX(1.)
            .setB(1.)
            .setG(1.)
            .add();
        network.newTieLine()
                .setId("TL1")
                .setDanglingLine1("DL1")
                .setDanglingLine2("DL2")
                .add();

        return network;
    }

    private static Network createCgmesGeoDataNetwork() {
        Network network = NoEquipmentNetworkFactory.create();

        Substation s1 = network.newSubstation()
                .setId("SubstationS1")
                .setName("P1")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();

        VoltageLevel vlhv1 = s1.newVoltageLevel()
                .setId("VLHV1")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        Bus nhv1 = vlhv1.getBusBreakerView().newBus()
                .setId("NHV1")
                .add();

        Substation s2 = network.newSubstation()
                .setId("SubstationS2")
                .setName("P2")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();

        VoltageLevel vlhv2 = s2.newVoltageLevel()
                .setId("VLHV2")
                .setNominalV(380.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        Bus nhv2 = vlhv2.getBusBreakerView().newBus()
                .setId("NHV2")
                .add();

        network.newLine()
                .setId("NHV1_NHV2")
                .setVoltageLevel1(vlhv1.getId())
                .setBus1(nhv1.getId())
                .setConnectableBus1(nhv1.getId())
                .setVoltageLevel2(vlhv2.getId())
                .setBus2(nhv2.getId())
                .setConnectableBus2(nhv2.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        return network;
    }

    @Test
    void testGetSubstationsGeodataById() {
        Network network = createGeoDataNetwork();
        List<SubstationGeoData> substationsGeoData = geoDataService.getSubstationsByIds(network, Set.of("P1", "P3"));

        assertEquals(2, substationsGeoData.size());
        assertEquals(1, substationsGeoData.stream().filter(s -> s.getId().equals("P1")).toList().get(0).getCoordinate().getLatitude(), 0);
        assertEquals(7, substationsGeoData.stream().filter(s -> s.getId().equals("P3")).toList().get(0).getCoordinate().getLongitude(), 0);

        Substation p8 = network.newSubstation()
                .setId("P8")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();

        VoltageLevel vlhv8 = p8.newVoltageLevel()
                .setId("VLHV8")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        Bus nhv8 = vlhv8.getBusBreakerView().newBus()
                .setId("NHV8")
                .add();

        network.newLine()
                .setId("NHV4_NHV8")
                .setVoltageLevel1("VLHV4")
                .setBus1("NHV4")
                .setConnectableBus1("NHV4")
                .setVoltageLevel2(vlhv8.getId())
                .setBus2(nhv8.getId())
                .setConnectableBus2(nhv8.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        network.newLine()
                .setId("NHV8_NHV5")
                .setVoltageLevel1("VLHV8")
                .setBus1("NHV8")
                .setConnectableBus1("NHV8")
                .setVoltageLevel2("VLHV5")
                .setBus2("NHV5")
                .setConnectableBus2("NHV5")
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        Substation p12 = network.newSubstation()
                .setId("P12")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();

        VoltageLevel vlhv12 = p12.newVoltageLevel()
                .setId("VLHV12")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        vlhv12.getBusBreakerView().newBus()
                .setId("NHV12")
                .add();

        Substation p9 = network.newSubstation()
                .setId("P9")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();

        VoltageLevel vlhv9 = p9.newVoltageLevel()
                .setId("VLHV9")
                .setNominalV(380)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();

        Bus nhv9 = vlhv9.getBusBreakerView().newBus()
                .setId("NHV9")
                .add();

        network.newLine()
                .setId("NHV12_NHV9")
                .setVoltageLevel1("VLHV12")
                .setBus1("NHV12")
                .setConnectableBus1("NHV12")
                .setVoltageLevel2(vlhv9.getId())
                .setBus2(nhv9.getId())
                .setConnectableBus2(nhv9.getId())
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();

        substationsGeoData = geoDataService.getSubstationsByIds(network, Set.of("P4", "P3", "P9"));

        assertEquals(3, substationsGeoData.stream().filter(s -> s.getId().equals("P4")).toList().get(0).getCoordinate().getLongitude(), 0);
        assertEquals(2, substationsGeoData.stream().filter(s -> s.getId().equals("P4")).toList().get(0).getCoordinate().getLatitude(), 0);
        assertEquals(7, substationsGeoData.stream().filter(s -> s.getId().equals("P3")).toList().get(0).getCoordinate().getLongitude(), 0);
        assertEquals(2, substationsGeoData.stream().filter(s -> s.getId().equals("P3")).toList().get(0).getCoordinate().getLatitude(), 0);
        assertEquals(19.993, substationsGeoData.stream().filter(s -> s.getId().equals("P9")).toList().get(0).getCoordinate().getLongitude(), 0);
        assertEquals(9.998, substationsGeoData.stream().filter(s -> s.getId().equals("P9")).toList().get(0).getCoordinate().getLatitude(), 0);
    }

    @Test
    void testGetLinesGeodataById() {
        Network network = createGeoDataNetwork();

        List<LineGeoData> linesGeoData = geoDataService.getLinesByIds(network, Set.of("NHV2_NHV5", "NHV1_NHV2_1"));

        assertEquals(3, linesGeoData.stream().filter(s -> s.getId().equals("NHV2_NHV5")).toList().get(0).getCoordinates().get(0).getLatitude(), 0);
        assertEquals(1, linesGeoData.stream().filter(s -> s.getId().equals("NHV2_NHV5")).toList().get(0).getCoordinates().get(0).getLongitude(), 0);

        assertEquals(2, linesGeoData.stream().filter(s -> s.getId().equals("NHV2_NHV5")).toList().get(0).getCoordinates().get(1).getLatitude(), 0);
        assertEquals(1, linesGeoData.stream().filter(s -> s.getId().equals("NHV2_NHV5")).toList().get(0).getCoordinates().get(1).getLongitude(), 0);

        assertEquals(2.5, linesGeoData.stream().filter(s -> s.getId().equals("NHV2_NHV5")).toList().get(0).getCoordinates().get(2).getLatitude(), 0);
        assertEquals(1, linesGeoData.stream().filter(s -> s.getId().equals("NHV2_NHV5")).toList().get(0).getCoordinates().get(2).getLongitude(), 0);

        assertEquals(3, linesGeoData.stream().filter(s -> s.getId().equals("NHV2_NHV5")).toList().get(0).getCoordinates().get(3).getLatitude(), 0);
        assertEquals(1, linesGeoData.stream().filter(s -> s.getId().equals("NHV2_NHV5")).toList().get(0).getCoordinates().get(3).getLongitude(), 0);

        assertEquals(4, linesGeoData.stream().filter(s -> s.getId().equals("NHV2_NHV5")).toList().get(0).getCoordinates().get(4).getLatitude(), 0);
        assertEquals(8, linesGeoData.stream().filter(s -> s.getId().equals("NHV2_NHV5")).toList().get(0).getCoordinates().get(4).getLongitude(), 0);

        assertEquals(1, linesGeoData.stream().filter(s -> s.getId().equals("NHV1_NHV2_1")).toList().get(0).getCoordinates().get(0).getLatitude(), 0);
        assertEquals(1, linesGeoData.stream().filter(s -> s.getId().equals("NHV1_NHV2_1")).toList().get(0).getCoordinates().get(0).getLongitude(), 0);

        assertEquals(3, linesGeoData.stream().filter(s -> s.getId().equals("NHV1_NHV2_1")).toList().get(0).getCoordinates().get(1).getLatitude(), 0);
        assertEquals(1, linesGeoData.stream().filter(s -> s.getId().equals("NHV1_NHV2_1")).toList().get(0).getCoordinates().get(1).getLongitude(), 0);
    }
}
