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
 *   17.02.2005 (ohl): created
 */
package org.knime.base.node.io.arffwriter;

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

 * @author Tim Crundall, TNG Technology Consulting GmbH

 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class ARFFWriterNodeFactory extends NodeFactory implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private final String m_file;

    /**
     * New factory - no default file.
     */
    public ARFFWriterNodeFactory() {
        m_file = null;
    }

    /**
     * New ARFF factory with default output file.
     *
     * @param defFile the default file to write to
     */
    public ARFFWriterNodeFactory(final String defFile) {
        m_file = defFile;
    }

    @Override
    public NodeModel createNodeModel() {
        if (m_file == null) {
            return new ARFFWriterNodeModel();
        } else {
            return new ARFFWriterNodeModel(m_file);
        }
    }

    @Override
    public NodeView createNodeView(final int viewIndex,
            final NodeModel nodeModel) {
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
    private static final String NODE_NAME = "ARFF Writer";
    private static final String NODE_ICON = "./arffwriter.png";
    private static final String SHORT_DESCRIPTION = "Writes data into a file in ARFF format.";
    private static final String FULL_DESCRIPTION = """
            This node saves data to a file or to a remote location denoted by an URL in ARFF format.
            In the configuration dialog, specify a valid destination location. When executed, the
            node writes the data, coming through its input port, into the specified location. At
            this point in time, it only writes not-sparse ARFF files (i.e. it always writes out all
            data, even if its value is zero). <br />
            Note that if the destination location is a remote URL not all options are available
            because in general it's not possible to determine whether the remote location exists. In
            this case it will always be overwritten.
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Input table", "The data table to be written to the file.")
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of();

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.11
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, ARFFWriterNodeParameters.class);
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
            ARFFWriterNodeParameters.class, //
            null, //
            NodeType.Sink, //
            List.of(), //
            null //
        );
    }

    /**
     * @since 5.11
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, ARFFWriterNodeParameters.class));
    }
}
