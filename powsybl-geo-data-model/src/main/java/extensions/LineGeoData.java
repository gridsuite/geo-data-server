/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package extensions;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Line;
import infrastructure.LineGraphic;

/**
 *
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
public class LineGeoData extends AbstractExtension<Line> {

    static final String NAME = "line-geo-data";

    private LineGraphic lineGraphic;

    private LineGeoData(Line line) {
        super(line);
    }

    public LineGeoData(Line line, LineGraphic lineGraphic) {
        this(line);
        this.lineGraphic = lineGraphic;
    }

    public LineGraphic getLineGraphic() {
        return lineGraphic;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
