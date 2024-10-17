/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@Repository
public interface LineRepository extends JpaRepository<LineEntity, String> {

    default List<LineEntity> findByCountryInOrOtherCountryIn(Collection<String> countries) {
        return findByCountryInOrOtherCountryIn(countries, countries);
    }

    List<LineEntity> findByCountryInOrOtherCountryIn(Collection<String> countries, Collection<String> countries2);
}
