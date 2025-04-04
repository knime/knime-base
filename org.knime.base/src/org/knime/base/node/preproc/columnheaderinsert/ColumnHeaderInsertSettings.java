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
 *   Jan 6, 2023 (Jonas Klotz, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.columnheaderinsert;

import static org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice.ROW_ID;

import java.util.Optional;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.StringToStringWithRowIDChoiceMigration;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.util.column.ColumnSelectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.CompatibleColumnsProvider.StringColumnsProvider;

/**
 * Settings of the Insert Column Header (Dictionary) Node. Only used for the Web UI dialog, please double-check
 * backwards compatible loading if this class is ever used in the NodeModel.
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 * @since 5.1
 */
@SuppressWarnings("restriction")
public final class ColumnHeaderInsertSettings implements DefaultNodeSettings {

    @Widget(title = "Lookup column",
        description = "The column in the 2nd input table containing the \"old\" names of the columns.")
    @ChoicesProvider(StringColumnsSecondTable.class)
    @Migration(LookupColumnMigration.class)
    @Persist(configKey = "lookupColumnV2")
    StringOrEnum<RowIDChoice> m_lookupColumn = new StringOrEnum<>(ROW_ID);

    static final class LookupColumnMigration extends StringToStringWithRowIDChoiceMigration {

        protected LookupColumnMigration() {
            super("lookupColumn");
        }

        @Override
        public Optional<RowIDChoice> loadEnumFromLegacyString(final String legacyString) {
            /**
             * Prior to 5.0.0 row keys were represented by a null string value in the settings.
             */
            if (legacyString == null) {
                return Optional.of(ROW_ID);
            }
            return super.loadEnumFromLegacyString(legacyString);
        }

    }

    @Widget(title = "Names column",
        description = "The column in the 2nd input table containing the \"new\" names of the columns.")
    @ChoicesProvider(StringColumnsSecondTable.class)
    String m_valueColumn;

    @Widget(title = "Fail if no assignment in dictionary table",
        description = "If selected, the node fails if there is no matching entry of a column name"
            + " in the dictionary table. Otherwise it will keep the original column name.")
    boolean m_failIfNoMatch = true;

    static final class StringColumnsSecondTable extends StringColumnsProvider {

        StringColumnsSecondTable() {
            super();
        }

        @Override
        public int getInputTableIndex() {
            return 1;
        }

    }

    void validate() throws InvalidSettingsException {
        if (m_lookupColumn.getEnumChoice().isEmpty() && m_lookupColumn.getStringChoice().isEmpty()) {
            throw new InvalidSettingsException("Invalid (empty) key for lookup column");
        }
        if (m_valueColumn == null || m_valueColumn.isEmpty()) {
            throw new InvalidSettingsException("Invalid (empty) value for value column name");
        }

    }

    /**
     * Default settings constructor
     */
    public ColumnHeaderInsertSettings() {
    }

    ColumnHeaderInsertSettings(final DefaultNodeSettingsContext context) {
        this(context.getDataTableSpec(1).orElse(null));
    }

    ColumnHeaderInsertSettings(final DataTableSpec dictSpec) {
        if (dictSpec == null) {
            return;
        }
        ColumnSelectionUtil.getFirstStringColumn(dictSpec).ifPresent(c -> {
            m_valueColumn = c.getName();
        });
    }

}
