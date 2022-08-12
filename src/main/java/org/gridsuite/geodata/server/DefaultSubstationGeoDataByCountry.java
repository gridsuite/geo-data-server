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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Nicolas Noir <nicolas.noir at rte-france.com>
 */
@Service
public class DefaultSubstationGeoDataByCountry {

    private Map<String, SubstationGeoData> substationsGeoDataByCountry;

    @Autowired
    private ObjectMapper mapper;

    @PostConstruct
    public void init() {
        substationsGeoDataByCountry = new HashMap<>();
        try {
            InputStream configStream = getClass().getResourceAsStream("/config/substationGeoDataByCountry.json");
            substationsGeoDataByCountry = mapper.readValue(configStream, new TypeReference<HashMap<String, SubstationGeoData>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public SubstationGeoData get(String countryName) {
        return substationsGeoDataByCountry.get(countryName);
    }

    public Set<Map.Entry<String, SubstationGeoData>> getEntrySet() {
        return substationsGeoDataByCountry.entrySet();
    }
}
