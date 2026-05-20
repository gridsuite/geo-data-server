/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.network.extensions.Coordinate;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.network.store.client.RestClientImpl;
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
import org.gridsuite.geodata.server.repositories.LineRepository;
import org.gridsuite.geodata.server.repositories.SubstationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import static com.powsybl.network.store.model.NetworkStoreApi.VERSION;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Radouane Khouadri <redouane.khouadri_externe at rte-france.com>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SupervisionControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NetworkStoreService service;

    @MockitoBean
    private SubstationRepository substationRepository;

    @MockitoBean
    private LineRepository lineRepository;

    @MockitoBean
    private GeoDataObserver geoDataObserver;

    @MockitoBean
    private RestClientImpl restClient;

    private static final String GEO_DATA_SUBSTATIONS = "/geo_data_substations.json";
    private static final String GEO_DATA_LINES = "/geo_data_lines.json";

    private static final String VARIANT_ID = "First_variant";

    private static String toString(String resourceName) throws IOException {
        return new String(ByteStreams.toByteArray(Objects.requireNonNull(SupervisionControllerTest.class.getResourceAsStream(resourceName))), StandardCharsets.UTF_8);
    }

    @Test
    void test() throws Exception {
        UUID networkUuid = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");

        Network testNetwork = EurostagTutorialExample1Factory.create();
        testNetwork.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_ID);
        given(service.getNetwork(networkUuid)).willReturn(testNetwork);
        given(service.getNetwork(networkUuid, PreloadingStrategy.NONE)).willReturn(testNetwork);
        given(service.getNetwork(networkUuid, PreloadingStrategy.COLLECTION)).willReturn(testNetwork);

        String substationJson = objectMapper.writeValueAsString(Collections.singleton(
                SubstationGeoData.builder()
                        .id("testID")
                        .country(Country.FR)
                        .coordinate(new Coordinate(1, 1))
                        .build()));

        mvc.perform(post("/" + VERSION + "/supervision/substations")
                .contentType(APPLICATION_JSON)
                .content(substationJson))
                .andExpect(status().isOk());

        mvc.perform(post("/" + VERSION + "/supervision/lines")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singleton(
                        LineGeoData.builder()
                                .country1(Country.FR)
                                .country2(Country.BE)
                                .substationStart("subFR")
                                .substationEnd("subBE")
                                .coordinates(new ArrayList<>())
                                .build()))))
                .andExpect(status().isOk());

        mvc.perform(post("/" + VERSION + "/supervision/substations")
                .contentType(APPLICATION_JSON)
                .content(toString(GEO_DATA_SUBSTATIONS)))
                .andExpect(status().isOk());

        mvc.perform(post("/" + VERSION + "/supervision/lines")
                .contentType(APPLICATION_JSON)
                .content(toString(GEO_DATA_LINES)))
                .andExpect(status().isOk());
    }
}
