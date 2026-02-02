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
 * History
 *   11.02.2005 (ohl): created
 */
package org.knime.base.node.io.arffreader;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
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
 *
 * @author Peter Ohl, University of Konstanz
 *
 * @author Tim Crundall, TNG Technology Consulting GmbH
 *
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class ARFFReaderNodeFactory extends NodeFactory implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private final String m_fileURL;

    /**
     * Will deliver a model with no default file set.
     */
    public ARFFReaderNodeFactory() {
        m_fileURL = null;
    }

    /**
     * This factory will create a model with the file set as default file.
     *
     * @param fileURL a valid URL to the default ARFF file.
     */
    public ARFFReaderNodeFactory(final String fileURL) {
        m_fileURL = fileURL;
    }

    @Override
    public NodeModel createNodeModel() {
        if (m_fileURL == null) {
            return new ARFFReaderNodeModel();
        } else {
            return new ARFFReaderNodeModel(m_fileURL);
        }
    }

    @Override
    public NodeView createNodeView(final int viewIndex, final NodeModel nodeModel) {
        assert false;
        return null;
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "ARFF Reader";

    private static final String NODE_ICON = "./arffreader.png";

    private static final String SHORT_DESCRIPTION = """
            Reads ARFF data files.
            """;

    private static final String FULL_DESCRIPTION = """
            This node reads in ARFF data from an URL. In the configuration dialog specify a valid URL and set an
            optional row prefix. A RowID is generated by the reader in the form 'prefix + rownumber'. If no prefix
            is specified, the RowIDs are just the row numbers. Sparse ARFF files are currently not supported (these
            are ARFF files where data with value 0 is not explicitly represented)!
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of();

    private static final List<PortDescription> OUTPUT_PORTS =
        List.of(fixedPort("Data from ARFF", "The data read from ARFF files."));

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.11
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, ARFFReaderNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription( //
            NODE_NAME, //
            NODE_ICON, //
            INPUT_PORTS, //
            OUTPUT_PORTS, //
            SHORT_DESCRIPTION, //
            FULL_DESCRIPTION, //
            List.of(), //
            ARFFReaderNodeParameters.class, //
            null, //
            NodeType.Source, //
            List.of(), //
            null //
        );
    }

    /**
     * @since 5.11
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, ARFFReaderNodeParameters.class));
    }
}
