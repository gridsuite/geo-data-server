/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.geodata.server.dto.SubstationGeoData;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nicolas Noir <nicolas.oir at rte-france.com>
 */
@Service
public class DefaultSubstationGeoDataByCountry {
    private Map<String, SubstationGeoData> substationsGeoDataByCountry;

    private ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        substationsGeoDataByCountry = new HashMap<>();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream configStream = classLoader.getResourceAsStream("config/substationGeoDataByCountry.json");
            substationsGeoDataByCountry = mapper.readValue(configStream, new TypeReference<HashMap<String, SubstationGeoData>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public SubstationGeoData get(String countryName) {
        return substationsGeoDataByCountry.get(countryName);
    }
}
