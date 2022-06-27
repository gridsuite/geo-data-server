/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.geodata.server;

import com.powsybl.commons.PowsyblException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler {

    @ExceptionHandler(value = {GeoDataException.class, PowsyblException.class})
    protected ResponseEntity<Object> handleException(Exception exception) {
        if (exception instanceof GeoDataException) {
            GeoDataException geoDataException = (GeoDataException) exception;
            return ResponseEntity
                    .status(geoDataException.getType().getStatus())
                    .body(geoDataException.getMessage());
        }
        return ResponseEntity.internalServerError().body(exception.getMessage());
    }
}
