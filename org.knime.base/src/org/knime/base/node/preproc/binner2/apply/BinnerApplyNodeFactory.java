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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.binner2.apply;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * @author Paul Baernreuther, KNIME GmbH, Germany
 */
public final class BinnerApplyNodeFactory extends NodeFactory<BinnerApplyNodeModel> {
    /**
     * {@inheritDoc}
     */
    @Override
    public BinnerApplyNodeModel createNodeModel() {
        return new BinnerApplyNodeModel();
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
    public NodeView<BinnerApplyNodeModel> createNodeView(final int viewIndex, final BinnerApplyNodeModel nodeModel) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return false;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return null;
    }

    private static final String NODE_NAME = "Binner (Apply)";

    private static final String NODE_ICON = "./binnerapply.png";

    private static final String SHORT_DESCRIPTION = """
            This node allows to group numeric data in intervals - called bins.
            """;

    private static final String FULL_DESCRIPTION = """
            This node allows to group numeric data in intervals - called bins. The bins are defined in the PMML
                Model. This node is typically used when test data shall be binned the same way the training data has
                been binned (using the "Binner" node).
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(//
        fixedPort("PMML Processing Fragment", """
                The PMML Model fragment containing information how to bin
                """), //
        fixedPort("Input Data", """
                Data to be categorized
                """)//
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("Binned Data", """
            Data with bins defined
            """));

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
            SHORT_DESCRIPTION, FULL_DESCRIPTION, List.of(), null, null, NodeType.Manipulator, List.of(), null);
    }

}
