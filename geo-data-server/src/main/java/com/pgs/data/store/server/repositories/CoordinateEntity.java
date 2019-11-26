/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.pgs.data.store.server.repositories;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@UserDefinedType("coordinate")
@AllArgsConstructor
@Data
@Builder
public class CoordinateEntity {
    private double lat;

    private double lon;
}
