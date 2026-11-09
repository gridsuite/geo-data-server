/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server.error;

import com.powsybl.ws.commons.error.AbstractBusinessException;
import lombok.NonNull;

import java.util.Objects;
import java.util.UUID;

public class GeoDataException extends AbstractBusinessException {

    private static final String NOT_FOUND_SUFFIX = "' not found";

    private final GeoDataBusinessErrorCode errorCode;

    public GeoDataException(GeoDataBusinessErrorCode errorCode, String message) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public static GeoDataException createNetworkNotFound(@NonNull UUID networkUuid) {
        return new GeoDataException(GeoDataBusinessErrorCode.NETWORK_NOT_FOUND,
                "Network '" + networkUuid + NOT_FOUND_SUFFIX);
    }

    public static GeoDataException createVariantNotFound(@NonNull String variantId) {
        return new GeoDataException(GeoDataBusinessErrorCode.VARIANT_NOT_FOUND,
                "Variant '" + variantId + NOT_FOUND_SUFFIX);
    }

    public static GeoDataException createInvalidCountry(@NonNull String country) {
        return new GeoDataException(GeoDataBusinessErrorCode.INVALID_COUNTRY,
                "Invalid country '" + country + "'");
    }

    @NonNull
    @Override
    public GeoDataBusinessErrorCode getBusinessErrorCode() {
        return errorCode;
    }
}
