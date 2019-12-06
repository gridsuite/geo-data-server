/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.geo.data.server.dto;

import java.awt.*;
import java.util.Objects;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public enum BaseVoltage {
    VL_400_KV(Color.RED, 0, 400),
    VL_225_KV(Color.GREEN, 1, 225),
    VL_150_KV(Color.BLUE, 2, 150),
    VL_90_KV(Color.PINK, 3, 90),
    VL_63_KV(Color.BLACK, 4, 63),
    VL_45_KV(Color.GRAY, 5, 45),
    VL_INF_45_KV(Color.DARK_GRAY, 6, -1),
    VL_OFF(Color.WHITE, 7, 0),
    VL_CONTINOUS_CURRENT(Color.YELLOW, 8, -99);

    private final Color color;

    private final int voltage;

    private final int order;

    BaseVoltage(Color color, int order, int voltage) {
        this.color = Objects.requireNonNull(color);
        this.order = order;
        this.voltage = voltage;
    }

    public Color getColor() {
        return color;
    }

    public int getOrder() {
        return order;
    }

    public int getVoltage() {
        return voltage;
    }
}
