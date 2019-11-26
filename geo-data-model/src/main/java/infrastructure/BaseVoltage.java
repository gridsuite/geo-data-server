/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package infrastructure;

import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public enum BaseVoltage {
    VL_400_KV(Color.rgb(255, 0, 0), 0, 400),
    VL_225_KV(Color.rgb(34, 139, 34), 1, 225),
    VL_150_KV(Color.rgb(1, 175, 175), 2, 150),
    VL_90_KV(Color.rgb(204, 85, 0), 3, 90),
    VL_63_KV(Color.rgb(160, 32, 240), 4, 63),
    VL_45_KV(Color.rgb(255, 130, 144), 5, 45),
    VL_INF_45_KV(Color.rgb(171, 175, 40), 6, -1),
    VL_OFF(Color.BLACK, 7, 0),
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
