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
 *   Sep 9, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.metainfo;

import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.choices.ColumnFilter;
import org.knime.node.parameters.widget.choices.ColumnSelectionWidget;
import org.knime.node.parameters.widget.choices.filter.TypeColumnFilter;

/**
 * Settings for the Files/Folders Meta Info node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class FileFolderMetaInfoNodeSettings implements NodeParameters {

    @Section(title = "Settings")
    interface SettingsSection {
    }
    
    @Section(title = "File Attributes")
    interface AttributesSection {
    }

    @Layout(SettingsSection.class)
    @Widget(title = "Path column", 
            description = "Select the column containing file or folder paths")
    @ColumnSelectionWidget
    @ColumnFilter(TypeColumnFilter.class)
    @Persist(configKey = "column")
    public String selectedColumn;

    @Layout(SettingsSection.class)
    @Widget(title = "Fail if file/folder does not exist", 
            description = "If checked, the execution fails when a file or folder does not exist")
    @Persist(configKey = "file_if_path_not_exists")
    public boolean failIfPathNotExists = true;

    @Layout(SettingsSection.class)
    @Widget(title = "Calculate overall folder size", 
            description = "If checked, the total size of folders including all subfiles will be calculated")
    @Persist(configKey = "calculate_overall_folder_size")
    public boolean calculateOverallFolderSize = false;

    @Layout(AttributesSection.class)
    @Widget(title = "Append file permissions", 
            description = "If checked, file permission information will be appended as additional columns")
    @Persist(configKey = "append_permissions")
    public boolean appendPermissions = false;

    @Layout(AttributesSection.class)
    @Widget(title = "Append POSIX attributes", 
            description = "If checked, POSIX file attributes will be appended as additional columns")
    @Persist(configKey = "append_posix_attrs")
    public boolean appendPosixAttrs = false;

    /**
     * Type filter for FSLocation columns.
     */
    public static final class FSLocationTypeFilter implements TypeColumnFilter {
        @Override
        public boolean isCompatible(final Class<?> type) {
            return FSLocationValue.class.isAssignableFrom(type);
        }
    }
}
