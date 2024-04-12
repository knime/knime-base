/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   20 Mar 2024 (jasper): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.io.IOException;
import java.util.Optional;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.xml.sax.SAXException;

/**
 * Node factory for the Row Filter 2 (Labs) node.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // Web UI not API yet
public final class RowFilter3NodeFactory extends ConfigurableNodeFactory<RowFilter3NodeModel>
    implements NodeDialogFactory {

    private static final String INPUT = "Input table";

    private static final String MATCHES = "Included rows";

    private static final String NON_MATCHES = "Excluded rows";

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder() //
        .name("Row Filter 2 (Labs)") //
        .icon("./rowfilter.png") //
        .shortDescription(
            "Allows filtering of data rows by certain criteria, such as RowID, attribute value, and row number range.")
        .fullDescription("""
                The node allows for row filtering according to certain criteria.
                It can include or exclude rows by either matching on the row number, the RowID or any cell in the row.
                Note: The node doesn't change the domain of the data table, i. e. the upper and lower bounds or the
                possible values in the table spec are not changed, even if one of the bounds or one value is fully
                filtered out.
                """) //
        .modelSettingsClass(RowFilter3NodeSettings.class) //
        .addInputTable(INPUT, "Data table from which to filter rows") //
        .addOutputTable(MATCHES, "Data table with rows meeting the specified criterion") //
        .addOutputTable(NON_MATCHES, "Data table with rows not meeting the specified criterion", true) //
        .nodeType(NodeType.Manipulator) //
        .keywords("Row", "Filter", "Rowfilter", "Condition", "Predicate", "where") //
        .sinceVersion(5, 3, 0) //
        .build();


    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription(CONFIG);
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, RowFilter3NodeSettings.class);
    }

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        b.addFixedInputPortGroup(INPUT, BufferedDataTable.TYPE);
        b.addFixedOutputPortGroup(MATCHES, BufferedDataTable.TYPE);
        b.addOptionalOutputPortGroup(NON_MATCHES, BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected RowFilter3NodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portsConfig = creationConfig.getPortConfig().orElseThrow();
        return new RowFilter3NodeModel(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return null;
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<RowFilter3NodeModel> createNodeView(final int viewIndex, final RowFilter3NodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

}
