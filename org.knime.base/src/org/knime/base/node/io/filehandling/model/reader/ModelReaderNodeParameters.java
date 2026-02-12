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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.io.filehandling.model.reader;

import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.LegacyReaderFileSelectionPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.persistence.Persistor;

/**
 * Node parameters for Model Reader.
 *
 * @author GitHub Copilot, KNIME GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ModelReaderNodeParameters implements NodeParameters {

    private static final class FileChooserPersistor extends LegacyReaderFileSelectionPersistor {
        FileChooserPersistor() {
            super("filechooser");
        }
    }

    @Widget(title = "Source", description = """
            Select a file system which stores the data you want to read. There are four default file system \
            options to choose from: <br /> <ul> <li><i>Local File System:</i> Allows you to select a file \
            from your local system. </li> <li><i>Mountpoint:</i> Allows you to read from a mountpoint. When \
            selected, a new drop-down menu appears to choose the mountpoint. Unconnected mountpoints are \
            greyed out but can still be selected (note that browsing is disabled in this case). Go to the \
            KNIME Explorer and connect to the mountpoint to enable browsing. A mountpoint is displayed in red \
            if it was previously selected but is no longer available. You won't be able to save the dialog as \
            long as you don't select a valid i.e. known mountpoint. </li> <li><i>Relative to:</i> Allows you \
            to choose whether to resolve the path relative to the current mountpoint, current workflow or the \
            current workflow's data area. When selected, a new drop-down menu appears to choose which of the \
            three options to use. </li> <li><i>Custom/KNIME URL:</i> Allows to specify a URL (e.g. file://, \
            http:// or knime:// protocol). When selected, a spinner appears that allows you to specify the \
            desired connection and read timeout in milliseconds. Browsing is disabled for this option. </li> \
            </ul> To read from other file systems, click on <b>...</b> in the bottom left corner of the node \
            icon followed by <i>Add File System Connection port</i>. Afterwards, connect the desired file \
            system connector node to the newly added input port. The file system connection will then be \
            shown in the drop-down menu. It is greyed out if the file system is not connected in which case \
            you have to (re)execute the connector node first. Note: The default file systems listed above \
            can't be selected if a file system is provided via the input port. \
            <br/><br/> \
            Enter a URL when reading from <i>Custom/KNIME URL</i>, otherwise enter a path to a file. The \
            required syntax of a path depends on the chosen file system, such as "C:\\path\\to\\file" (Local \
            File System on Windows) or "/path/to/file" (Local File System on Linux/MacOS and Mountpoint). For \
            file systems connected via input port, the node description of the respective connector node \
            describes the required path format. You can also choose a previously selected file from the \
            drop-down list, or select a location from the "Browse..." dialog. Note that browsing is disabled \
            in some cases: <ul> <li><i>Custom/KNIME URL:</i>Browsing is never enabled.</li> \
            <li><i>Mountpoint:</i> Browsing is disabled if the selected mountpoint isn't connected. Go to the \
            KNIME Explorer and connect to the mountpoint to enable browsing.</li> <li><i>File systems \
            provided via input port:</i> Browsing is disabled if the connector node hasn't been executed \
            since the workflow has been opened. (Re)execute the connector node to enable browsing. </li> \
            </ul> <i>The location can be exposed as or automatically set via a </i><a \
            href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html#path"> \
            <i>path flow variable.</i></a>
            """)
    @Persistor(FileChooserPersistor.class)
    @Migrate
    @FileReaderWidget(fileExtensions = {"model", "zip"})
    FileSelection m_source = new FileSelection();

}
