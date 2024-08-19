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
 *   Jan 26, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.columnheaderextract;

import static org.knime.base.node.preproc.columnheaderextract.ColumnHeaderExtractorNodeModel.CFG_COLTYPE;
import static org.knime.base.node.preproc.columnheaderextract.ColumnHeaderExtractorNodeModel.CFG_TRANSPOSE_COL_HEADER;

import org.knime.base.node.preproc.columnheaderextract.ColumnHeaderExtractorNodeModel.ColType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.BooleanReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * Settings of the Column Header Extractor dialog. Not used by the NodeModel, yet. If it ever is please double check
 * backwards compatibility.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 */
@SuppressWarnings("restriction")
public final class ColumnHeaderExtractorNodeSettings implements DefaultNodeSettings {

    interface DontReplaceColHeader { }

    @Persist(customPersistor = OutputFormatPersistor.class)
    @Widget(title = "Output format for column names",
        description = "The format in which the first output table provides the extracted column names:" //
            + "<ul>"//
            + "<li><b>Row</b>: The column names are output as a single row with a column per name.</li>"//
            + "<li><b>Column</b>: The column names are output as a single column with a row per name.</li>"//
            + "</ul>")
    @ValueSwitchWidget
    OutputFormat m_transposeColHeader;

    static final class ReplaceColHeader implements BooleanReference {

    }

    @Persist(settingsModel = SettingsModelBoolean.class)
    @Widget(title = "Generate new column names",
        description = "If selected, the column names of both output tables will be replaced "//
            + "with automatically generated names by combining the prefix provided below with the corresponding "//
            + "column number (e.g. \"Column 1\", \"Column 2\", and so on). "//
            + "<br><br>Otherwise, the original column names will be used.")
    @ValueReference(ReplaceColHeader.class)
    boolean m_replaceColHeader;

    @Persist(settingsModel = SettingsModelString.class)
    @Widget(title = "Prefix", description = "Prefix to use when generating new column names.")
    @Effect(type = EffectType.SHOW, predicate = ReplaceColHeader.class)
    String m_unifyHeaderPrefix;

    @Persist(customPersistor = ColTypePersistor.class)
    @Widget(title = "Restrain column types", description = "Select the type of the columns to extract the names from:"//
        + "<ul>"//
        + "<li><b>All</b>: All columns are processed.</li>"//
        + "<li><b>String</b>: Only string-compatible columns are processed, "//
        + "this includes e.g. XML columns.</li>"//
        + "<li><b>Integer</b>: Only integer-compatible columns are processed.</li>"//
        + "<li><b>Double</b>: Only double-compatible columns are processed. "//
        + "This includes integer and long columns.</li>"//
        + "</ul>",
        advanced = true)
    @ValueSwitchWidget
    ColType m_colTypeFilter = ColType.ALL;

    enum OutputFormat {
            @Label("Row")
            ROW, //
            @Label("Column")
            COLUMN;
    }

    private static final class OutputFormatPersistor implements FieldNodeSettingsPersistor<OutputFormat> {

        @Override
        public OutputFormat load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(CFG_TRANSPOSE_COL_HEADER) ? OutputFormat.COLUMN : OutputFormat.ROW;
        }

        @Override
        public void save(final OutputFormat obj, final NodeSettingsWO settings) {
            settings.addBoolean(CFG_TRANSPOSE_COL_HEADER, obj == OutputFormat.COLUMN);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{CFG_TRANSPOSE_COL_HEADER};
        }
    }

    private static final class ColTypePersistor implements FieldNodeSettingsPersistor<ColType> {

        @Override
        public ColType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return ColType.fromDisplayString(settings.getString(CFG_COLTYPE));
        }

        @Override
        public void save(final ColType obj, final NodeSettingsWO settings) {
            settings.addString(CFG_COLTYPE, obj.displayString());
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{CFG_COLTYPE};
        }
    }
}
