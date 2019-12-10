/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geodata.server.repositories;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.powsybl.geodata.extensions.Coordinate;
import com.powsybl.geodata.server.dto.LineGeoData;
import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Component
public  class LinesCustomRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinesCustomRepository.class);

    @Autowired
    private Session session;

    public Map<String, LineGeoData> getAllLines() {
        LOGGER.info("BEGIN getAllLines");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String query = "select * from lines";
        Map<String, LineGeoData> linesGeoDataDB = getLines(query);
        LOGGER.info("{}", linesGeoDataDB.size());
        LOGGER.info("END getAllLines from DB in {} ms,", stopWatch.getTime());
        return linesGeoDataDB;
    }

    private Map<String, LineGeoData> getLines(String query) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        ResultSet result = session.execute(query);
        List<Row> rows = result.all();

        return rows.stream()
                .map(r -> LineGeoData.builder()
                        .id(r.get("lineId", String.class))
                        .aerial(r.get("aerial", Boolean.class))
                        .ordered(r.get("ordered", Boolean.class))
                        .voltage(r.getInt("voltage"))
                        .country(Country.valueOf(r.getString("country")))
                        .coordinates(new ArrayDeque<>(r.getList("coordinates", Coordinate.class)))
                        .build())
                .collect(Collectors.toMap(LineGeoData::getId, Function.identity()));
    }

    public Map<String, LineGeoData> getLines(String country, int voltage) {
        LOGGER.info("BEGIN getLines");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String query = "select * from lines where country ='" + country + "' AND voltage =" + voltage;
        Map<String, LineGeoData> linesGeoDataDB = getLines(query);
        LOGGER.info("{}", linesGeoDataDB.size());
        LOGGER.info("END getLines from DB in {} ms,", stopWatch.getTime());
        return linesGeoDataDB;
    }
}



