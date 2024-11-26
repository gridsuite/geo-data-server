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
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
import org.gridsuite.geodata.server.repositories.LineRepository;
import org.gridsuite.geodata.server.repositories.SubstationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import static com.powsybl.network.store.model.NetworkStoreApi.VERSION;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@WebMvcTest(GeoDataController.class)
class GeoDataControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NetworkStoreService service;

    @MockBean
    private SubstationRepository substationRepository;

    @MockBean
    private LineRepository lineRepository;

    private static final String GEO_DATA_SUBSTATIONS = "/geo_data_substations.json";
    private static final String GEO_DATA_LINES = "/geo_data_lines.json";

    private static final String VARIANT_ID = "First_variant";
    private static final String WRONG_VARIANT_ID = "Wrong_variant";

    private static String toString(String resourceName) throws IOException {
        return new String(ByteStreams.toByteArray(Objects.requireNonNull(GeoDataControllerTest.class.getResourceAsStream(resourceName))), StandardCharsets.UTF_8);
    }

    @Test
    void test() throws Exception {
        UUID networkUuid = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");

        Network testNetwork = EurostagTutorialExample1Factory.create();
        testNetwork.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_ID);
        given(service.getNetwork(networkUuid)).willReturn(testNetwork);
        given(service.getNetwork(networkUuid, PreloadingStrategy.NONE)).willReturn(testNetwork);
        given(service.getNetwork(networkUuid, PreloadingStrategy.COLLECTION)).willReturn(testNetwork);

        mvc.perform(get("/" + VERSION + "/substations?networkUuid=" + networkUuid)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/" + VERSION + "/substations?networkUuid=" + networkUuid + "&variantId=" + VARIANT_ID)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/" + VERSION + "/substations?networkUuid=" + networkUuid + "&variantId=" + WRONG_VARIANT_ID)
                .contentType(APPLICATION_JSON))
                .andExpect(content().string("Variant '" + WRONG_VARIANT_ID + "' not found"))
                .andExpect(status().isInternalServerError());

        mvc.perform(get("/" + VERSION + "/lines?networkUuid=" + networkUuid)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/" + VERSION + "/lines?networkUuid=" + networkUuid + "&variantId=" + VARIANT_ID)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/" + VERSION + "/lines?networkUuid=" + networkUuid + "&variantId=" + WRONG_VARIANT_ID)
                .contentType(APPLICATION_JSON))
                .andExpect(content().string("Variant '" + WRONG_VARIANT_ID + "' not found"))
                .andExpect(status().isInternalServerError());

        String substationJson = objectMapper.writeValueAsString(Collections.singleton(
                SubstationGeoData.builder()
                        .id("testID")
                        .country(Country.FR)
                        .coordinate(new Coordinate(1, 1))
                        .build()));

        mvc.perform(post("/" + VERSION + "/substations")
                .contentType(APPLICATION_JSON)
                .content(substationJson))
                .andExpect(status().isOk());

        mvc.perform(post("/" + VERSION + "/lines")
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

        mvc.perform(post("/" + VERSION + "/substations")
                .contentType(APPLICATION_JSON)
                .content(toString(GEO_DATA_SUBSTATIONS)))
                .andExpect(status().isOk());

        mvc.perform(post("/" + VERSION + "/lines")
                .contentType(APPLICATION_JSON)
                .content(toString(GEO_DATA_LINES)))
                .andExpect(status().isOk());

        mvc.perform(get("/" + VERSION + "/substations?networkUuid=" + networkUuid + "&variantId=" + VARIANT_ID + "&substationId=P1&substationId=P2&country=" + Country.FR)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/" + VERSION + "/substations?networkUuid=" + networkUuid + "&variantId=" + VARIANT_ID + "&substationId=P1&substationId=P2")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/" + VERSION + "/substations?networkUuid=" + networkUuid + "&variantId=" + VARIANT_ID + "&substationId=notExistsId&substationId=P2")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/" + VERSION + "/substations?networkUuid=" + networkUuid + "&variantId=" + VARIANT_ID + "&substationId=notExistsId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/" + VERSION + "/lines?networkUuid=" + networkUuid + "&variantId=" + VARIANT_ID + "&lineId=NHV1_NHV2_2&lineId=NHV1_NHV2_1&country=" + Country.FR)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/" + VERSION + "/lines?networkUuid=" + networkUuid + "&variantId=" + VARIANT_ID + "&lineId=NHV1_NHV2_2&lineId=NHV1_NHV2_1")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/" + VERSION + "/lines?networkUuid=" + networkUuid + "&variantId=" + VARIANT_ID + "&lineId=NHV1_NHV2_2&lineId=notExistsId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/" + VERSION + "/lines?networkUuid=" + networkUuid + "&variantId=" + VARIANT_ID + "&lineId=notExistsId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetLinesError() throws Exception {
        UUID networkUuid = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        Network testNetwork = EurostagTutorialExample1Factory.create();
        given(service.getNetwork(networkUuid)).willReturn(testNetwork);
        given(service.getNetwork(networkUuid, PreloadingStrategy.COLLECTION)).willReturn(testNetwork);
        given(lineRepository.findAllById(any())).willThrow(new GeoDataException(GeoDataException.Type.PARSING_ERROR, new RuntimeException("Error parsing")));

        mvc.perform(get("/" + VERSION + "/lines?networkUuid=" + networkUuid)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isInternalServerError());
    }
}
