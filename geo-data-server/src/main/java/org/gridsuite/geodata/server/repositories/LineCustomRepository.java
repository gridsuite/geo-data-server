/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.repositories;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.gridsuite.geodata.extensions.Coordinate;
import org.gridsuite.geodata.server.dto.LineGeoData;
import com.powsybl.iidm.network.Country;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Repository
public class LineCustomRepository {

    @Autowired
    private Session session;

    private static LineGeoData rowToLineGeoData(Row row) {
        String id = row.getString("id");
        boolean side1 = row.getBool("side1");
        Country country = Country.valueOf(row.getString("country"));
        Country otherCountry = Country.valueOf(row.getString("otherCountry"));
        String substationStart = row.getString("substationStart");
        List<Coordinate> coordinates = row.getList("coordinates", Coordinate.class);
        return LineGeoData.builder()
                .id(id)
                .country1(side1 ? country : otherCountry)
                .country2(side1 ? otherCountry : country)
                .substationStart(substationStart)
                .coordinates(coordinates)
                .build();
    }

    public Map<String, LineGeoData> getLines() {
        ResultSet result = session.execute("select * from lines");
        List<Row> rows = result.all();
        return rows.stream()
                .map(LineCustomRepository::rowToLineGeoData)
                .collect(Collectors.toMap(LineGeoData::getId, Function.identity()));
    }
}
