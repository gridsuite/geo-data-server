/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.powsybl.iidm.network.test.NoEquipmentNetworkFactory;
import com.powsybl.iidm.network.extensions.Coordinate;
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
import org.gridsuite.geodata.server.repositories.*;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@RunWith(SpringRunner.class)
@ContextHierarchy({
    @ContextConfiguration(classes = GeoDataApplication.class)
    })
public class GeoDataServiceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubstationRepository substationRepository;

    @Autowired
    private LineRepository lineRepository;

    @Autowired
    GeoDataService geoDataService;

    @Autowired
    private DefaultSubstationGeoDataByCountry defaultSubstationsGeoData;

    @Before
    public void setUp() throws JsonProcessingException {
        lineRepository.deleteAll();
        substationRepository.deleteAll();
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

        substationRepository.saveAll(substationEntities);

        List<LineEntity> lineEntities = new ArrayList<>();

        lineEntities.add(LineEntity.builder()
                .id("NHV2_NHV5")
                .country("BE")
                .otherCountry("FR")
                .coordinates(objectMapper.writeValueAsString(Arrays.asList(new CoordinateEmbeddable(2, 1), new CoordinateEmbeddable(2.5, 1), new CoordinateEmbeddable(3, 1))))
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

    static LineGeoData getFromList(List<LineGeoData> list, String id) {
        Optional<LineGeoData> res = list.stream().filter(l -> l.getId().equals(id)).findAny();
        assertTrue(res.isPresent());
        return res.get();
    }

    @Test
    public void test() {
        Network network = createGeoDataNetwork();
        
        List<SubstationGeoData> substationsGeoData = geoDataService.getSubstations(network, new HashSet<>(Collections.singletonList(Country.FR)));

        assertEquals(4, substationsGeoData.size());
        assertEquals(2, substationsGeoData.stream().filter(s -> s.getId().equals("P4")).collect(Collectors.toList()).get(0).getCoordinate().getLatitude(), 0);
        assertEquals(3, substationsGeoData.stream().filter(s -> s.getId().equals("P4")).collect(Collectors.toList()).get(0).getCoordinate().getLongitude(), 0);

        List<LineGeoData> linesGeoData = geoDataService.getLines(network, new HashSet<>(List.of(Country.FR)));

        assertEquals(13, linesGeoData.size());
        assertEquals(2, getFromList(linesGeoData, "NHV1_NHV2_1").getCoordinates().size()); // line with no coordinate, so [substation1, substation2]
        List<Coordinate> lineNHV2 = getFromList(linesGeoData, "NHV2_NHV3").getCoordinates();
        List<Coordinate> lineNHV3 = new ArrayList<>(getFromList(linesGeoData, "NHV2_NHV3_inverted").getCoordinates());
        Collections.reverse(lineNHV3);
        assertEquals(lineNHV2, lineNHV3); // should be the same path
        assertEquals(5, lineNHV2.size()); // line with 3 coordinate, so [substation1, c1, c2, c3, substation2]

        List<Coordinate> wrong = getFromList(linesGeoData, "WRONG_CONFIG").getCoordinates();
        assertEquals(2, wrong.size()); // wrong substation origin/end, so only (sub1, sub2)

        List<SubstationGeoData> substationsGeoData2 = geoDataService.getSubstations(network, new HashSet<>(ImmutableList.of(Country.FR, Country.BE)));

        assertEquals(5, substationsGeoData2.size());
        assertEquals(2, substationsGeoData2.stream().filter(s -> s.getId().equals("P4")).collect(Collectors.toList()).get(0).getCoordinate().getLatitude(), 0);
        assertEquals(3, substationsGeoData2.stream().filter(s -> s.getId().equals("P4")).collect(Collectors.toList()).get(0).getCoordinate().getLongitude(), 0);

        assertEquals(4, substationsGeoData2.stream().filter(s -> s.getId().equals("P5")).collect(Collectors.toList()).get(0).getCoordinate().getLatitude(), 0);
        assertEquals(8, substationsGeoData2.stream().filter(s -> s.getId().equals("P5")).collect(Collectors.toList()).get(0).getCoordinate().getLongitude(), 0);

        List<SubstationGeoData> substationsGeoData3 = geoDataService.getSubstations(network, new HashSet<>(Collections.singletonList(Country.BE)));

        assertEquals(1, substationsGeoData3.size());

        assertEquals(4, substationsGeoData3.stream().filter(s -> s.getId().equals("P5")).collect(Collectors.toList()).get(0).getCoordinate().getLatitude(), 0);
        assertEquals(8, substationsGeoData3.stream().filter(s -> s.getId().equals("P5")).collect(Collectors.toList()).get(0).getCoordinate().getLongitude(), 0);

        List<SubstationGeoData> substationsGeoData4 = geoDataService.getSubstations(network, new HashSet<>(Collections.singletonList(Country.DE)));

        assertEquals(2, substationsGeoData4.size());

        SubstationGeoData p6 = substationsGeoData4.stream().filter(s -> s.getId().equals("P6")).collect(Collectors.toList()).get(0);
        SubstationGeoData p7 = substationsGeoData4.stream().filter(s -> s.getId().equals("P7")).collect(Collectors.toList()).get(0);
        assertEquals(0.002, Math.abs(p6.getCoordinate().getLatitude()) - p7.getCoordinate().getLatitude(), 0.0001);
        assertEquals(0.007, Math.abs(p6.getCoordinate().getLongitude()) - p7.getCoordinate().getLongitude(), 0.0001);

        List<SubstationGeoData> substationsGeoData5 = geoDataService.getSubstations(network, new HashSet<>(ImmutableList.of(Country.DE, Country.BE)));

        assertEquals(3, substationsGeoData5.size());

        assertEquals(4, substationsGeoData5.stream().filter(s -> s.getId().equals("P5")).collect(Collectors.toList()).get(0).getCoordinate().getLatitude(), 0);
        assertEquals(8, substationsGeoData5.stream().filter(s -> s.getId().equals("P5")).collect(Collectors.toList()).get(0).getCoordinate().getLongitude(), 0);
        assertEquals(8, substationsGeoData5.stream().filter(s -> s.getId().equals("P6")).collect(Collectors.toList()).get(0).getCoordinate().getLatitude(), 0.002);
        assertEquals(12, substationsGeoData5.stream().filter(s -> s.getId().equals("P6")).collect(Collectors.toList()).get(0).getCoordinate().getLongitude(), 0.007);
    }

    @Test
    public void testCgmesCase() {
        Network network = createCgmesGeoDataNetwork();

        List<SubstationGeoData> substationsGeoData = geoDataService.getSubstations(network, new HashSet<>(Collections.singletonList(Country.FR)));

        assertEquals(2, substationsGeoData.size());
        assertEquals(1, substationsGeoData.stream().filter(s -> s.getId().equals("SubstationS1")).collect(Collectors.toList()).get(0).getCoordinate().getLatitude(), 0);
        assertEquals(1, substationsGeoData.stream().filter(s -> s.getId().equals("SubstationS1")).collect(Collectors.toList()).get(0).getCoordinate().getLongitude(), 0);
        assertEquals(3, substationsGeoData.stream().filter(s -> s.getId().equals("SubstationS2")).collect(Collectors.toList()).get(0).getCoordinate().getLatitude(), 0);
        assertEquals(1, substationsGeoData.stream().filter(s -> s.getId().equals("SubstationS2")).collect(Collectors.toList()).get(0).getCoordinate().getLongitude(), 0);
    }

    @Test
    public void testNonExisting() {
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
        List<SubstationGeoData> substationsGeoData = geoDataService.getSubstations(network, new HashSet<>(Collections.singletonList(Country.FR)));
        assertFalse("Must not contain nulls", substationsGeoData.stream().anyMatch(Objects::isNull));
        assertFalse("Must not contain unknown substation " + notexistsub1.getId(),
                substationsGeoData.stream().anyMatch(s -> notexistsub1.getId().equals(s.getId())));
        assertFalse("Must not contain unknown substation " + notexistsub2.getId(),
                substationsGeoData.stream().anyMatch(s -> notexistsub2.getId().equals(s.getId())));

        List<LineGeoData> linesGeoData = geoDataService.getLines(network, new HashSet<>(Collections.singletonList(Country.FR)));
        assertFalse("Must not contain nulls", linesGeoData.stream().anyMatch(Objects::isNull));
        assertFalse("Must not contain unknown lines " + notexistline.getId(),
                linesGeoData.stream().anyMatch(s -> notexistline.getId().equals(s.getId())));
    }

    @Test
    public void testSimilarNeighborhoodOffset() {
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

        List<SubstationGeoData> substationsGeoData = geoDataService.getSubstations(network, new HashSet<>(Collections.singletonList(Country.FR)));

        SubstationGeoData pgd4 = substationsGeoData.stream().filter(s -> s.getId().equals("P4")).collect(Collectors.toList()).get(0);
        SubstationGeoData pgd5 = substationsGeoData.stream().filter(s -> s.getId().equals("P5")).collect(Collectors.toList()).get(0);
        assertEquals(GeoDataService.CALCULATED_SUBSTATION_OFFSET, Math.abs(pgd4.getCoordinate().getLongitude() - pgd5.getCoordinate().getLongitude()), 0.0001);
    }

    @Test
    public void testCalculatedDefaultSubstations() {
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
        Substation p7 = network.newSubstation()
                .setId("P7")
                .setCountry(Country.BE)
                .setTso("RTE")
                .add();

        Substation p8 = network.newSubstation()
                .setId("P8")
                .setCountry(Country.BE)
                .setTso("RTE")
                .add();

        Substation p9 = network.newSubstation()
                .setId("P9")
                .setCountry(Country.BE)
                .setTso("RTE")
                .add();

        List<SubstationGeoData> substationsGeoData = geoDataService.getSubstations(network, new HashSet<>(Collections.singletonList(Country.BE)));
        DefaultSubstationGeoParameter defaultSubstationGeoParameter = new DefaultSubstationGeoParameter(0.0, 0.0, defaultSubstationsGeoData.get("BE").getCoordinate());

        SubstationGeoData pg4 = substationsGeoData.stream().filter(s -> s.getId().equals("P4")).collect(Collectors.toList()).get(0);
        SubstationGeoData pg5 = substationsGeoData.stream().filter(s -> s.getId().equals("P5")).collect(Collectors.toList()).get(0);
        SubstationGeoData pg6 = substationsGeoData.stream().filter(s -> s.getId().equals("P6")).collect(Collectors.toList()).get(0);

        assertEquals(defaultSubstationGeoParameter.getCurrentCoordinates(), pg4.getCoordinate());
        defaultSubstationGeoParameter.incrementDefaultSubstationGeoParameters();
        assertEquals(defaultSubstationGeoParameter.getCurrentCoordinates(), pg5.getCoordinate());
        defaultSubstationGeoParameter.incrementDefaultSubstationGeoParameters();
        assertEquals(defaultSubstationGeoParameter.getCurrentCoordinates(), pg6.getCoordinate());
    }

    @Test
    public void testLineCoordinatesError() {
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

    private Network createGeoDataNetwork() {
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

        return network;
    }

    private Network createCgmesGeoDataNetwork() {
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
}
