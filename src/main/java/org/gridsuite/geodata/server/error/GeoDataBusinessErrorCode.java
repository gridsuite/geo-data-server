/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.error;

import com.powsybl.ws.commons.error.BusinessErrorCode;

/**
 * Business error codes emitted by the geo-data service.
 */
public enum GeoDataBusinessErrorCode implements BusinessErrorCode {
    NETWORK_NOT_FOUND("geoData.networkNotFound"),
    VARIANT_NOT_FOUND("geoData.variantNotFound"),
    INVALID_COUNTRY("geoData.invalidCountry");

    private final String code;

    GeoDataBusinessErrorCode(String code) {
        this.code = code;
    }

    public String value() {
        return code;
    }
}
