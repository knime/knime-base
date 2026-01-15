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
 *   2021-07-23 (Alexander Bondaletov): created
 */
package org.knime.filehandling.core.fs.local.node;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * Factory class for the Local FS Connector node.
 *
 * @author Alexander Bondaletov
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings({"restriction", "deprecation"})
public class LocalConnectorNodeFactory extends WebUINodeFactory<LocalConnectorNodeModel> {

    /**
     * Create a new factory.
     */
    public LocalConnectorNodeFactory() {
        super(CONFIGURATION);
    }

    private static final String NODE_NAME = "Local File System Connector";

    private static final String NODE_ICON = "./file_system_connector.png";

    private static final String SHORT_DESCRIPTION = """
            Provides a file system connection with access to the file system of the local machine.
            """;

    private static final String FULL_DESCRIPTION = """
            <p>
                This node provides access to the file system of the local machine. The resulting output port allows
                downstream nodes to access <i>files</i>, e.g. to read or write, or to perform other file system
                operations (browse/list files, copy, move, ...).
            </p>
            <p>
                <b>Note:</b> In many cases it is not necessary to use this connector node to access the local file
                system. Nodes that require file system access (e.g. the File Reader node) typically provide local
                file system access, for example by choosing <i>Read from</i> &gt; <i>Local File System.</i> The
                purpose of this connector node is to make file access with
                <a href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html\
            #path-syntax">relative paths</a>
                more flexible, by allowing to explicitly configure the
                <a href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html\
            #working-directory">working directory</a>
                of the resulting file system connection. For example, this makes it easier to relocate (or change) the
                files that your workflow accesses, because you only need to change the working directory.
            </p>
            <p>
                <b>Path syntax:</b> The path syntax is identical to the one used by the operating system of the
                machine where this node is running, e.g. on Windows a path could look like <tt>C:\\Users\\joe</tt>.
            </p>
            """;

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name(NODE_NAME) //
        .icon(NODE_ICON) //
        .shortDescription(SHORT_DESCRIPTION) //
        .fullDescription(FULL_DESCRIPTION) //
        .modelSettingsClass(LocalConnectorNodeParameters.class) //
        .nodeType(NodeType.Source) //
        .sinceVersion(4, 5, 0) //
        .addOutputPort("Local File System Connection", FileSystemPortObject.TYPE, "Local File System Connection")
        .build();

    @Override
    public LocalConnectorNodeModel createNodeModel() {
        return new LocalConnectorNodeModel();
    }

}
