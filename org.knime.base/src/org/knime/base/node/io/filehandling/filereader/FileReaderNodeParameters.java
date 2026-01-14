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
    
package org.knime.base.node.io.filehandling.filereader;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;

/**
 * Node parameters for File Reader (Complex Format).
 * 
 * @author Tim Crundall, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
class FileReaderNodeParameters implements NodeParameters {
    
    /**
     * TODO: Implement the actual parameters based on the dialog configuration
     * Use the following descriptions extracted from the node factory xml for
     * the description tag in @Widget annotations of the respective parameters.
     * IMPORTANT: Due to previous decoupling of node description and actually used 
     * parameters, the following options might be 
     * a) incomplete -> Sometimes there exist tooltips in the dialog however
     * b) irrelevant/removed -> interesting to investigate why
     * c) of slightly different structure (e.g. previously, descriptions of parameters inside array layouts might have been listed top-level)
     * 
     * Option: Read from
     * Select a file system which stores the model you want to read. There are four default file system
     * options to choose from: <br /> <ul> <li><i>Local File System:</i> Allows you to select a file
     * from your local system. </li> <li><i>Mountpoint:</i> Allows you to read from a mountpoint. When
     * selected, a new drop-down menu appears to choose the mountpoint. Unconnected mountpoints are
     * greyed out but can still be selected (note that browsing is disabled in this case). Go to the
     * KNIME Explorer and connect to the mountpoint to enable browsing. A mountpoint is displayed in red
     * if it was previously selected but is no longer available. You won't be able to save the dialog as
     * long as you don't select a valid i.e. known mountpoint. </li> <li><i>Relative to:</i> Allows you
     * to choose whether to resolve the path relative to the current mountpoint, current workflow or the
     * current workflow's data area. When selected, a new drop-down menu appears to choose which of the
     * two options to use. </li> <li><i>Custom/KNIME URL:</i> Allows to specify a URL (e.g. file://,
     * http:// or knime:// protocol). When selected, a spinner appears that allows you to specify the
     * desired connection and read timeout in milliseconds. In case it takes longer to connect to the
     * host / read the file, the node fails to execute. Browsing is disabled for this option. </li>
     * </ul> It is possible to use other file systems with this node. Therefore, you have to enable the
     * file system connection input port of this node by clicking the <b>...</b> in the bottom left
     * corner of the node's icon and choose <i>Add File System Connection port</i> . <br /> Afterwards,
     * you can simply connect the desired connector node to this node. The file system connection will
     * then be shown in the drop-down menu. It is greyed out if the file system is not connected in
     * which case you have to (re)execute the connector node first. Note: The default file systems
     * listed above can't be selected if a file system is provided via the input port.
     *
     * Option: File/URL
     * Enter a URL when reading from <i>Custom/KNIME URL</i>, otherwise enter a path to a file. The
     * required syntax of a path depends on the chosen file system, such as "C:\path\to\file" (Local
     * File System on Windows) or "/path/to/file" (Local File System on Linux/MacOS and Mountpoint). For
     * file systems connected via input port, the node description of the respective connector node
     * describes the required path format. You can also choose a previously selected file from the
     * drop-down list, or select a location from the "Browse..." dialog. Note that browsing is disabled
     * in some cases: <ul> <li><i>Custom/KNIME URL:</i> Browsing is always disabled.</li>
     * <li><i>Mountpoint:</i> Browsing is disabled if the selected mountpoint isn't connected. Go to the
     * KNIME Explorer and connect to the mountpoint to enable browsing.</li> <li><i>File systems
     * provided via input port:</i> Browsing is disabled if the connector node hasn't been executed
     * since the workflow has been opened. (Re)execute the connector node to enable browsing.</li> </ul>
     * <i>The location can be exposed as or automatically set via a </i><a
     * href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html#path">
     * <i>path flow variable.</i></a>
     *
     * Option: Preserve user settings
     * If checked, the checkmarks and column names/types you explicitly entered are preserved even if
     * you select a new file. By default, the analyzer starts with fresh default settings for each new
     * file location.
     *
     * Option: Rescan
     * If clicked, the file content is analyzed again. All settings are reset (unless the "Preserve user
     * settings" option is selected) and the file is read in again to pre-set new settings and the table
     * structure.
     *
     * Option: Read RowIDs
     * If checked, the first column in the file is used as RowIDs. If not checked, default row headers
     * are created.
     *
     * Option: Read column headers
     * If checked, the items in the first line of the file are used as column names. Otherwise default
     * column names are created.
     *
     * Option: Column delimiter
     * Enter the character(s) that separate the data tokens in the file, or select a delimiter from the
     * list.
     *
     * Option: Ignore spaces and tabs
     * If checked, spaces and the TAB characters are ignored (not in quoted strings though).
     *
     * Option: Java style comment
     * Everything between '/*' and '*/' is ignored. Also everything after '//' until the end of the
     * line.
     *
     * Option: Single line comment
     * Enter one or more characters that will indicate the start of a comment (ended by a new line).
     *
     * Option: Advanced...
     * Opens a new dialog with advanced settings. There is support for quotes, different decimal
     * separators in floating point numbers, and character encoding. Also, for ignoring whitespaces, for
     * allowing rows with too few data items, for making RowIDs unique (not recommended for huge files),
     * for a global missing value pattern, and for limiting the number of rows read in.
     *
     * Option: Click on the table header
     * If the column header in the preview table is clicked, a new dialog opens where column properties
     * can be set: name and type can be changed (and will be fixed then). A pattern can be entered that
     * will cause a "missing cell" to be created when it's read for this column. Additionally, possible
     * values of the column domain can be updated by selecting "Domain". And, you can choose to skip
     * this column entirely, i.e. it will not be included in the output table then.
     * */
    
}
