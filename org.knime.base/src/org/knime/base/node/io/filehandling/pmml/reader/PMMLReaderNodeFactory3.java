/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.filehandling.pmml.reader;

import java.util.Optional;

import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.node.portobject.reader.PortObjectReaderNodeConfig;
import org.knime.filehandling.core.node.portobject.reader.PortObjectReaderNodeDialog;
import org.knime.filehandling.core.node.portobject.reader.PortObjectReaderNodeFactory;

/**
 * Node factory of the PMML reader node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public final class PMMLReaderNodeFactory3
    extends PortObjectReaderNodeFactory<PMMLReaderNodeModel3, PortObjectReaderNodeDialog<PortObjectReaderNodeConfig>> {

    private static final String URL_TIMEOUT = "1000";

    /** File chooser history Id. */
    private static final String HISTORY_ID = "pmml_model_reader_writer";

    /** The pmml file extension/suffix. */
    private static final String[] PMML_SUFFIX = new String[]{".pmml"};

    @Override
    protected PMMLReaderNodeModel3 createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new PMMLReaderNodeModel3(creationConfig, getConfig(creationConfig));
    }

    @Override
    protected PortObjectReaderNodeDialog<PortObjectReaderNodeConfig>
        createDialog(final NodeCreationConfiguration creationConfig) {
        return new PortObjectReaderNodeDialog<>(getConfig(creationConfig), HISTORY_ID);
    }

    @Override
    protected PortType getOutputPortType() {
        return PMMLPortObject.TYPE;
    }

    /**
     * Returns the port object reader node configuration.
     *
     * @param creationConfig {@link NodeCreationConfiguration} of the corresponding KNIME node
     * @return the reader configuration
     */
    private static PortObjectReaderNodeConfig getConfig(final NodeCreationConfiguration creationConfig) {
        final Optional<? extends URLConfiguration> urlConfig = creationConfig.getURLConfig();
        final PortObjectReaderNodeConfig cfg = PortObjectReaderNodeConfig.builder(creationConfig)//
            .withFileSuffixes(PMML_SUFFIX)//
            .build();//
        if (urlConfig.isPresent()) {
            cfg.getFileChooserModel()
                .setLocation(new FSLocation(FSCategory.CUSTOM_URL, URL_TIMEOUT, urlConfig.get().getUrl().toString()));
        }
        return cfg;
    }
}
