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
import com.powsybl.iidm.network.Substation;
import infrastructure.Coordinate;
import infrastructure.SubstationGraphic;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@AutoService(ExtensionXmlSerializer.class)
public class SubstationGeoDataXmlSerializer implements ExtensionXmlSerializer<Substation, SubstationGeoData> {

    @Override
    public boolean hasSubElements() {
        return false;
    }

    @Override
    public InputStream getXsdAsStream() {
        return getClass().getResourceAsStream("/xsd/substationGeoData.xsd");
    }

    @Override
    public String getNamespaceUri() {
        return "http://www.itesla_project.eu/schema/iidm/ext/substationGeoData/1_0";
    }

    @Override
    public String getNamespacePrefix() {
        return "sgd";
    }

    @Override
    public void write(SubstationGeoData substationGeoData, XmlWriterContext context) throws XMLStreamException {
        context.getExtensionsWriter().writeAttribute("id", substationGeoData.getSubstationGraphic().getId());
        XmlUtil.writeDouble("lat", substationGeoData.getSubstationGraphic().getPosition().getLat(), context.getExtensionsWriter());
        XmlUtil.writeDouble("lon", substationGeoData.getSubstationGraphic().getPosition().getLon(), context.getExtensionsWriter());
    }

    @Override
    public SubstationGeoData read(Substation substation, XmlReaderContext context) throws XMLStreamException {
        String id = context.getReader().getAttributeValue(null, "id");
        double lat = XmlUtil.readDoubleAttribute(context.getReader(), "lat");
        double lon = XmlUtil.readDoubleAttribute(context.getReader(), "lon");
        SubstationGraphic substationGraphic = new SubstationGraphic(id, new Coordinate(lat, lon));
        return new SubstationGeoData(substation, substationGraphic);
    }

    @Override
    public String getExtensionName() {
        return "substation-geo-data";
    }

    @Override
    public String getCategoryName() {
        return "network";
    }

    @Override
    public Class<? super SubstationGeoData> getExtensionClass() {
        return SubstationGeoData.class;
    }
}

