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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.pmml.columntrans2;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.base.node.preproc.columntrans2.One2ManyCol2NodeParameters;
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

/**
 * Factory for the "One to Many (PMML)" node.
 *
 * @author Dominik Morent, University of Konstanz
 */
@SuppressWarnings("restriction")
public class One2ManyCol2PMMLNodeFactory2 extends NodeFactory<One2ManyCol2PMMLNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    /**
     * {@inheritDoc}
     *
     * @since 2.8
     */
    @Override
    public One2ManyCol2PMMLNodeModel createNodeModel() {
        return new One2ManyCol2PMMLNodeModel(true, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.8
     */
    @Override
    public NodeView<One2ManyCol2PMMLNodeModel> createNodeView(final int viewIndex,
        final One2ManyCol2PMMLNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, One2ManyCol2NodeParameters.class);
    }

    private static final String FULL_DESCRIPTION = """
            Transforms all possible values in a selected column each
            into a new column. The value is set as the new column's name,
            the cell values in that column are either 1, if that row
            contains this possible value, or 0 if not.<br />
            The node appends as many columns as possible values are
            defined for the selected column(s).<br />
            If a row contains a missing value in a selected column all
            corresponding new columns contain the value 0.<br />
            To avoid duplicate column names with identical possible values
            in different selected columns, the generated column name
            includes the original column name in this case (i. e.
            the name looks like possibleValue_originalColumnName).<br />
            The dialog of the node allows you only to select columns with
            nominal values. If no column name appears in the dialog but your
            input table contains nominal columns, you could use the DomainCalculator
            node and connect its output to this node.
            """;

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription("One to Many (PMML)", //
            "./one2many.png", //
            List.of(fixedPort("Data", "Data to process.")), //
            List.of(fixedPort("Processed data", "Data with transformed columns"),
                fixedPort("Transformed PMML input", "PMML port object that includes the performed operations.")), //
            "Transforms the values of one column into appended columns.", //
            FULL_DESCRIPTION, //
            List.of(), //
            One2ManyCol2NodeParameters.class, //
            null, //
            NodeType.Manipulator, //
            List.of(), //
            null);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, One2ManyCol2NodeParameters.class));
    }
}
