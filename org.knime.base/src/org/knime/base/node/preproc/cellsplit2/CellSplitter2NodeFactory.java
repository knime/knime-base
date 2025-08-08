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
 * History
 *   Jun 19, 2007 (ohl): created
 */
package org.knime.base.node.preproc.cellsplit2;

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
 * The cell splitter node factory.
 * <p>
 * Note: This class replaces the (deprecated) CellSplitterNodeFactory.
 * </p>
 *
 * @author ohl, University of Konstanz
 */
@SuppressWarnings("restriction")
public final class CellSplitter2NodeFactory extends NodeFactory<CellSplitter2NodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, CellSplitter2NodeParameters.class);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, CellSplitter2NodeParameters.class));
    }

    // --- Node Description (migrated from XML) ---
    private static final String NODE_NAME = "Cell Splitter";

    private static final String NODE_ICON = "./cellsplitter.png";

    private static final String SHORT_DESCRIPTION = """
            Splits the string representation of cells in one column of the table
            into separate columns or into one column containing a collection of
            cells, based on a specified delimiter.
            """;

    private static final String FULL_DESCRIPTION = """
            This node uses a user-specified delimiter character to
            split the content of a selected column into parts. It appends either a
            fixed number of columns to the input table, each carrying one part of the
            original column, or a single column containing a collection (list or
            set) of cells with the split output. It can be specified whether the
            output consists of one or more columns, only one column containing
            list cells, or only one column containing set cells in which duplicates
            are removed.
            <br />
            If the column contains more delimiters than needed
            (leading to more parts than appended columns are available) the
            additional delimiters are ignored (resulting in the last column containing
            the unsplit rest of the column).
            <br />
            If the selected column contains too
            few delimiters (leading to less parts than expected), empty columns
            will be created in that row.
            <br />
            Based on the delimiters and the resulting parts the collection cells
            can have different sizes. The content of the new columns will be trimmed if specified
            (i.e. leading and trailing spaces will be deleted).
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(fixedPort("Input Table", """
            Input data table with column containing the cells to split
            """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("Output Table", """
            Output data table with additional columns.
            """));

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription( //
            NODE_NAME, //
            NODE_ICON, //
            INPUT_PORTS, //
            OUTPUT_PORTS, //
            SHORT_DESCRIPTION, //
            FULL_DESCRIPTION, //
            List.of(), // no views
            CellSplitter2NodeParameters.class, //
            null, // no view settings
            NodeType.Manipulator, //
            List.of(), // no filters
            null // no since version override
        );
    }

    @Override
    public CellSplitter2NodeModel createNodeModel() {
        return new CellSplitter2NodeModel();
    }

    @Override
    public NodeView<CellSplitter2NodeModel> createNodeView(final int viewIndex,
        final CellSplitter2NodeModel nodeModel) {
        return null;
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

}
