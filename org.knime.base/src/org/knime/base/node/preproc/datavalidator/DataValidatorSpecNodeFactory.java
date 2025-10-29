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
package org.knime.base.node.preproc.datavalidator;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * Factory to create all node classes for a node that handles missing values in a table and replaces it by some
 * meaningful values.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Gabor Bakos
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 * @since 2.8
 */
@SuppressWarnings("restriction")
public class DataValidatorSpecNodeFactory extends NodeFactory<DataValidatorSpecNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private static final String NODE_NAME = "Table Validator (Reference)";

    private static final String NODE_ICON = "./validate.png";

    private static final String SHORT_DESCRIPTION = """
            Validates the input data according to a reference data table specification give as a second input to the
                node.
            """;

    private static final String FULL_DESCRIPTION = """
            This node ensures a certain table structure and table content using the table structure and table
                content of a second table as reference. The table to validate should be connected to the first input
                port and the reference table to the second input port. Contrary to the <i>Table Validator</i> node, the
                performed validation checks will applied on the bases of all columns of the reference table and can't be
                adjusted per column. It is ensured that the result table structure is mostly identical to the reference
                specification. That is done by resorting of columns, the insertion of missing columns (filled with
                missing values) and optional removal of additional columns. You can also choose if each column is
                required and if the data type or the domain should be checked/converted. If the validation succeeds,
                data gets output to the first port (potentially renamed, sorted according to the reference specification
                and with converted types). If the validation fails, the first port is inactive and the second port
                contains a table that lists all conflicts or the node fails. All options mentioned below marked with
                <i>Data</i> forces also a traversal of the input data.
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(fixedPort("Table to validate", """
            Table to be validated.
            """), fixedPort("Reference Table", """
            Table that is used as the reference.
            """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("Validated table", """
            Table with corrected and validated structure. Depending on the validation result and the <i>Behavior if
            validation fails</i> settings, this port may be inactive.
            """), fixedPort("Validation fault table", """
            Table where missing values have been handled. Depending on the validation result and the <i>Behavior if
            validation fails</i> settings, this port may be inactive.
            """));

    /**
     * {@inheritDoc}
     */
    @Override
    public DataValidatorSpecNodeModel createNodeModel() {
        return new DataValidatorSpecNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<DataValidatorSpecNodeModel> createNodeView(final int viewIndex,
        final DataValidatorSpecNodeModel model) {
        throw new IndexOutOfBoundsException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, DataValidatorSpecNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
            SHORT_DESCRIPTION, FULL_DESCRIPTION, List.of(), DataValidatorSpecNodeParameters.class, null,
            NodeType.Manipulator, List.of(), null);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, DataValidatorSpecNodeParameters.class));
    }
}
