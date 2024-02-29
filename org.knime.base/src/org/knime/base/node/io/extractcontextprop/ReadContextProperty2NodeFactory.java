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
 *
 * History
 *   Aug 11, 2010 (wiswedel): created
 */
package org.knime.base.node.io.extractcontextprop;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.xml.sax.SAXException;

/**
 * "Newer" version of {@link ReadContextPropertyNodeFactory}, which will use a empty string ("") instead of null for
 * "context.job.id". See also AP-15364 (Extract Context Properties: inconsistent output of context.job.id when running
 * on AP).
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
public class ReadContextProperty2NodeFactory extends NodeFactory<ReadContextProperty2NodeModel>
implements NodeDialogFactory {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder() //
            .name("Extract Context Properties") //
            .icon("./extract_context_props.png") //
            .shortDescription("Reads workflow context related properties, "
                + "including workflow name and the logged in user.") //
            .fullDescription("""
            Reads workflow context related properties,
            including workflow name and the logged in user. The fields are
            extracted using the KNIME workflow context.<br />
            The properties are:
            <ul>
                <li><i>context.workflow.name</i> The name of the workflow.</li>
                <li><i>context.workflow.path</i> The mount-point-relative path to the workflow.</li>
                <li><i>context.workflow.absolute-path</i> The absolute path in the filesystem to the workflow.</li>
                <li><i>context.workflow.user</i>
                    User ID of the authenticated user if executed on KNIME Hub/Server, the system user otherwise.</li>
                <li><i>context.workflow.username</i>
                    User name of the authenticated user if executed on KNIME Hub/Server, the system user otherwise.
                    If executed on Hub/Server, <i>context.workflow.user</i> returns a technical permanent id.</li>
                <li><i>context.workflow.executor.version</i> Version of the KNIME AP executing the workflow.</li>
                <li><i>context.workflow.temp.location</i> The location for temporary files of the workflow job.</li>
                <li><i>context.workflow.author.name</i> Name of the workflow author (creator).</li>
                <li><i>context.workflow.last.editor.name</i>
                    Name of the person who edited the workflow last (on last save).
                    If not available, returns the author name.</li>
                <li><i>context.workflow.creation.date</i> Date when the workflow was saved the first time.</li>
                <li><i>context.workflow.last.time.modified</i> Date when workflow was saved last.
                    If not available, returns the creation date.</li>
                <li><i>context.job.id</i> The job's ID when run on KNIME Hub/Server, otherwise will be empty.</li>
                <li><i>context.workflow.hub.item.id</i>
                    Item ID of the workflow in the Hub's repository.
                    If the workflow is not on a Hub, this will be empty.</li>
                <li><i>context.workflow.hub.space.id</i>
                    Item ID of the space containing the workflow in the Hub's repository.
                    If the workflow is not on a Hub, this will be empty.</li>
                <li><i>context.workflow.hub.space.path</i>
                    The mount-point-relative path to the root of the space containing the workflow.
                    If the workflow is not on a Hub, this will be empty.</li>
                <li><i>context.workflow.hub.api.base-url</i>
                    The base URL of the Hub's API when stored on Hub, otherwise will be empty.</li>
                <li><i>context.job.account.id</i>
                    ID of the account that owns the job when run on Hub, otherwise will be empty.</li>
                <li><i>context.job.account.name</i>
                    Name of the account that owns the job when run on Hub, otherwise will be empty.</li>
            </ul>
            More properties may be added in the future.
                    """) //
            .modelSettingsClass(ReadContextProperty2NodeSettings.class) //
            .addOutputPort("Context Properties", FlowVariablePortObject.TYPE,
                "Context properties as flow variables.") //
            .nodeType(NodeType.Source) //
            .build();

    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription(CONFIG);
    }

    @Override
    public ReadContextProperty2NodeModel createNodeModel() {
        return new ReadContextProperty2NodeModel(CONFIG);
    }

    @Override
    public NodeView<ReadContextProperty2NodeModel> createNodeView(final int viewIndex,
            final ReadContextProperty2NodeModel nodeModel) {
        throw new IllegalStateException("No view");
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.3
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, ReadContextProperty2NodeSettings.class);
    }
}
