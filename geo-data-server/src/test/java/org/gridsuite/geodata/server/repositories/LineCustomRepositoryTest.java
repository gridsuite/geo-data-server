/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.repositories;

import org.gridsuite.geodata.server.AbstractEmbeddedCassandraSetup;
import org.gridsuite.geodata.server.GeoDataApplication;
import org.gridsuite.geodata.server.dto.LineGeoData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@RunWith(SpringRunner.class)
@ContextHierarchy({
        @ContextConfiguration(classes = {GeoDataApplication.class})
    })
public class LineCustomRepositoryTest extends AbstractEmbeddedCassandraSetup {

    @Autowired
    private LineCustomRepository lineCustomRepository;

    @Autowired
    private LineRepository lineRepository;

    @Test
    public void test() {
        lineRepository.save(LineEntity.builder()
                .id("testId")
                .country("FR")
                .otherCountry("BE")
                .substationStart("subBE")
                .build());

        lineRepository.save(LineEntity.builder()
                .id("testId2")
                .country("FR")
                .otherCountry("FR")
                .substationStart("subFR")
                .build());

        lineRepository.save(LineEntity.builder()
                .id("testId3")
                .country("FR")
                .otherCountry("GE")
                .substationStart("subGE")
                .build());

        Map<String, LineGeoData> lines = lineCustomRepository.getLines();

        assertEquals(3, lines.size());

        assertEquals("testId3", new ArrayList<>(lines.values()).get(0).getId());
        assertEquals("GE", new ArrayList<>(lines.values()).get(0).getCountry1().toString());
        assertEquals("FR", new ArrayList<>(lines.values()).get(0).getCountry2().toString());
        assertEquals("subGE", new ArrayList<>(lines.values()).get(0).getSubstationStart());

        assertEquals("testId", new ArrayList<>(lines.values()).get(2).getId());
        assertEquals("BE", new ArrayList<>(lines.values()).get(2).getCountry1().toString());
        assertEquals("FR", new ArrayList<>(lines.values()).get(2).getCountry2().toString());
        assertEquals("subBE", new ArrayList<>(lines.values()).get(2).getSubstationStart());
    }
}
