/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import org.springframework.http.HttpStatus;

import java.util.Objects;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class GeoDataException extends RuntimeException {

    public enum Type {
        PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

        public final HttpStatus status;

        HttpStatus getStatus() {
            return status;
        }

        Type(HttpStatus status) {
            this.status = status;
        }
    }

    private final Type type;

    public GeoDataException(Type type, Exception cause) {
        super(Objects.requireNonNull(type.name()) + " : " + ((cause.getMessage() == null) ? cause.getClass().getName() : cause.getMessage()), cause);
        this.type = type;
    }

    Type getType() {
        return type;
    }
}
