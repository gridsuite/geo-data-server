/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionXmlSerializer;
import com.powsybl.commons.xml.XmlReaderContext;
import com.powsybl.commons.xml.XmlUtil;
import com.powsybl.commons.xml.XmlWriterContext;
import com.powsybl.iidm.network.Line;
import infrastructure.Coordinate;
import infrastructure.LineGraphic;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@AutoService(ExtensionXmlSerializer.class)
public class LineGeoDataXmlSerializer implements ExtensionXmlSerializer<Line, LineGeoData> {

    @Override
    public String getExtensionName() {
        return  "line-geo-data";
    }

    @Override
    public String getCategoryName() {
        return "network";
    }

    @Override
    public Class<? super LineGeoData> getExtensionClass() {
        return LineGeoData.class;
    }

    @Override
    public boolean hasSubElements() {
        return true;
    }

    @Override
    public InputStream getXsdAsStream() {
        return getClass().getResourceAsStream("/xsd/lineGeoData.xsd");
    }

    @Override
    public String getNamespaceUri() {
        return "http://www.itesla_project.eu/schema/iidm/ext/lineGeoData/1_0";
    }

    @Override
    public String getNamespacePrefix() {
        return "lgd";
    }

    private void writeCoordinate(Coordinate coordinate, XmlWriterContext context) throws XMLStreamException {
        context.getExtensionsWriter().writeEmptyElement(getNamespaceUri(), "coordinate");
        XmlUtil.writeDouble("lon", coordinate.getLon(), context.getExtensionsWriter());
        XmlUtil.writeDouble("lat", coordinate.getLat(), context.getExtensionsWriter());
    }

    @Override
    public void write(LineGeoData lineGeoData, XmlWriterContext context) throws XMLStreamException {
        XmlUtil.writeInt("coordinatesCount", lineGeoData.getLineGraphic().getCoordinates().size(), context.getExtensionsWriter());
        for (Coordinate coordinate : lineGeoData.getLineGraphic().getCoordinates()) {
            writeCoordinate(coordinate, context);
        }
    }

    private Coordinate readCoordinate(XmlReaderContext context) {
        double lon = XmlUtil.readDoubleAttribute(context.getReader(), "lon");
        double lat = XmlUtil.readDoubleAttribute(context.getReader(), "lat");
        return new Coordinate(lat, lon);
    }

    @Override
    public LineGeoData read(Line line, XmlReaderContext context) throws XMLStreamException {
        LineGeoData lineGeoData = new LineGeoData(null, new LineGraphic());

        XmlUtil.readUntilEndElement(getExtensionName(), context.getReader(), () -> {
            if ("coordinate".equals(context.getReader().getLocalName())) {
                lineGeoData.getLineGraphic().getCoordinates().add(readCoordinate(context));
            } else {
                throw new AssertionError();
            }
        });
        return lineGeoData;
    }
}
