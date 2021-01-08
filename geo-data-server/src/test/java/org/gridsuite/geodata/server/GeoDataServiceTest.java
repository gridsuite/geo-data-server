/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@RunWith(SpringRunner.class)
@ContextHierarchy({
    @ContextConfiguration(classes = GeoDataApplication.class)
    })
public class GeoDataServiceTest extends AbstractEmbeddedCassandraSetup  {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubstationRepository substationRepository;

    @Autowired
    private LineRepository lineRepository;

    @Autowired
    GeoDataService geoDataService;

    @Before
    public void setUp() {
        List<SubstationEntity> substationEntities = new ArrayList<>();

        substationEntities.add(SubstationEntity.builder()
                .id("P1")
                .country("FR")
                .coordinate(new CoordinateEntity(1, 1))
                .build());

        substationEntities.add(SubstationEntity.builder()
                .id("P2")
                .country("FR")
                .coordinate(new CoordinateEntity(3, 1))
                .build());

        substationEntities.add(SubstationEntity.builder()
                .id("P3")
                .country("FR")
                .coordinate(new CoordinateEntity(2, 7))
                .build());

        substationRepository.saveAll(substationEntities);

        List<LineEntity> lineEntities = new ArrayList<>();

        lineEntities.add(LineEntity.builder()
                .id("NHV2_NHV5")
                .country("BE")
                .otherCountry("FR")
                .coordinates(Arrays.asList(new CoordinateEntity(2, 1), new CoordinateEntity(2.5, 1), new CoordinateEntity(3, 1)))
                .build());

        lineEntities.add(LineEntity.builder()
                .id("NHV2_NHV3")
                .country("FR")
                .otherCountry("FR")
                .coordinates(Arrays.asList(new CoordinateEntity(3, 1), new CoordinateEntity(5, 6), new CoordinateEntity(2, 7)))
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
        assertEquals(2, substationsGeoData.stream().filter(s -> s.getId().equals("P4")).collect(Collectors.toList()).get(0).getCoordinate().getLat(), 0);
        assertEquals(3, substationsGeoData.stream().filter(s -> s.getId().equals("P4")).collect(Collectors.toList()).get(0).getCoordinate().getLon(), 0);

        List<LineGeoData> linesGeoData = geoDataService.getLines(network, new HashSet<>(Collections.singletonList(Country.FR)));

        assertEquals(9, linesGeoData.size());
        assertEquals(2, getFromList(linesGeoData, "NHV1_NHV2_1").getCoordinates().size()); // line with no coordinate, so [substation1, substation2]
        assertEquals(5, getFromList(linesGeoData, "NHV2_NHV3").getCoordinates().size()); // line with 3 coordinate, so [substation1, c1, c2, c3, substation2]

        List<SubstationGeoData> substationsGeoData2 = geoDataService.getSubstations(network, new HashSet<>(ImmutableList.of(Country.FR, Country.BE)));

        assertEquals(5, substationsGeoData2.size());
        assertEquals(2, substationsGeoData2.stream().filter(s -> s.getId().equals("P4")).collect(Collectors.toList()).get(0).getCoordinate().getLat(), 0);
        assertEquals(3, substationsGeoData2.stream().filter(s -> s.getId().equals("P4")).collect(Collectors.toList()).get(0).getCoordinate().getLon(), 0);

        assertEquals(2, substationsGeoData2.stream().filter(s -> s.getId().equals("P5")).collect(Collectors.toList()).get(0).getCoordinate().getLat(), 0);
        assertEquals(1, substationsGeoData2.stream().filter(s -> s.getId().equals("P5")).collect(Collectors.toList()).get(0).getCoordinate().getLon(), 0);
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

        return network;
    }
}
