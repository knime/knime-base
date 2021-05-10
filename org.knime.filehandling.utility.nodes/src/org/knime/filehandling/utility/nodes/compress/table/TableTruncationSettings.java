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
 *   Apr 19, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.compress.table;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.utility.nodes.truncator.TruncationSettings;

/**
 * Extends the TruncationSettings by a field that specifies the column containing the archive entry names.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TableTruncationSettings extends TruncationSettings {

    private static final String CFG_ENTRY_NAME_DEFINED_BY_TABLE = "from_table";

    private static final String CFG_ENTRY_COLUMN = "entry_name_column";

    private boolean m_entryNameDefinedByTableCol;

    private final SettingsModelString m_entryNameColModel;

    TableTruncationSettings(final String truncateOptionConfigKey) {
        super(truncateOptionConfigKey);
        m_entryNameColModel = new SettingsModelString(CFG_ENTRY_COLUMN, null);
    }

    boolean entryNameDefinedByTableColumn() {
        return m_entryNameDefinedByTableCol;
    }

    void entryNameDefinedByTableColumn(final boolean entryNameDefinedByTableCol) {
        m_entryNameDefinedByTableCol = entryNameDefinedByTableCol;
    }

    SettingsModelString getEntryNameColModel() {
        return m_entryNameColModel;
    }

    @Override
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        super.loadSettingsForDialog(settings);
        m_entryNameDefinedByTableCol = settings.getBoolean(CFG_ENTRY_NAME_DEFINED_BY_TABLE, false);
    }

    @Override
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsForModel(settings);
        m_entryNameDefinedByTableCol = settings.getBoolean(CFG_ENTRY_NAME_DEFINED_BY_TABLE);
        m_entryNameColModel.loadSettingsFrom(settings);
    }

    @Override
    public void saveSettingsForDialog(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_ENTRY_NAME_DEFINED_BY_TABLE, m_entryNameDefinedByTableCol);
        super.saveSettingsForDialog(settings);
    }

    @Override
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        super.saveSettingsForModel(settings);
        settings.addBoolean(CFG_ENTRY_NAME_DEFINED_BY_TABLE, m_entryNameDefinedByTableCol);
        m_entryNameColModel.saveSettingsTo(settings);
    }

    @Override
    public void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettingsForModel(settings);
        settings.containsKey(CFG_ENTRY_NAME_DEFINED_BY_TABLE);
        m_entryNameColModel.validateSettings(settings);
    }
}
