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
 *   Jan 27, 2023 (Jonas Klotz, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.columnmerge;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.booleanhelpers.AlwaysSaveTrueBoolean;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelStringPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.AllColumnsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.TextInputWidgetValidation.PatternValidation.ColumnNameValidationV2;

/**
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 * @since 5.0
 */
@SuppressWarnings("restriction")
public final class ColumnMergerNodeSettings implements DefaultNodeSettings {

    @Persistor(PrimaryColumnPersistor.class)
    @Widget(title = "Primary column",
        description = "The column with the value that will be used, unless it is missing.")
    @ChoicesProvider(AllColumnsProvider.class)
    String m_primaryColumn = "";

    static final class PrimaryColumnPersistor extends SettingsModelStringPersistor {
        PrimaryColumnPersistor() {
            super("primaryColumn");
        }
    }

    @Persistor(SecondaryColumnPersistor.class)
    @Widget(title = "Secondary column", description = "The column with the value that will be used if it is missing "//
        + "in the primary column.")
    @ChoicesProvider(AllColumnsProvider.class)
    String m_secondaryColumn = "";

    static final class SecondaryColumnPersistor extends SettingsModelStringPersistor {
        SecondaryColumnPersistor() {
            super("secondaryColumn");
        }
    }

    @Persistor(OutputPlacementOptionsPersistor.class)
    @Widget(title = "Replace/append columns", description = "Choose where to put the result column:"//
        + "<ul>"//
        + "<li><b>Replace primary and delete secondary</b>: Replace the primary column with the merge "
        + "result and remove the secondary column.</li>"//
        + "<li><b>Replace primary</b>: Replace the primary column with the merge "
        + "result and keep the secondary column.</li>"//
        + "<li><b>Replace secondary</b>: Keep the primary column and replace the "
        + "secondary column with the merge result.</li>"//
        + "<li><b>Append as new column</b>: Append a new column with the name provided below.</li>"//
        + "</ul>")
    @RadioButtonsWidget
    @ValueReference(OutputPlacement.Ref.class)
    OutputPlacement m_outputPlacement = OutputPlacement.ReplaceBoth;

    static final class OutputNamePersisor extends SettingsModelStringPersistor {
        OutputNamePersisor() {
            super("outputName");
        }
    }

    @Persistor(OutputNamePersisor.class)
    @Widget(title = "New column name", description = "The name for the new column.")
    @Effect(predicate = OutputPlacement.IsAppendAsNewColumn.class, type = EffectType.SHOW)
    @TextInputWidget(patternValidation = ColumnNameValidationV2.class)
    String m_outputName = "NewColumn";

    static final class DoNotAllowBlankOrPaddedColumnNamePersistor extends AlwaysSaveTrueBoolean {
        protected DoNotAllowBlankOrPaddedColumnNamePersistor() {
            super("doNotAllowBlankOrPaddedColumnName");
        }
    }

    @Persistor(DoNotAllowBlankOrPaddedColumnNamePersistor.class)
    boolean m_doNotAllowBlankOrPaddedColumnName = true;

    /** Policy how to place output. */
    enum OutputPlacement {
            /** Replace both columns, put output at position of primary column. */
            @Label("Replace primary and delete secondary")
            ReplaceBoth,
            /** Replace primary column. */
            @Label("Replace primary")
            ReplacePrimary,
            /** Replace secondary column. */
            @Label("Replace secondary")
            ReplaceSecondary,
            /** Append as new column. */
            @Label("Append as new column")
            AppendAsNewColumn;

        interface Ref extends Reference<OutputPlacement> {
        }

        static final class IsAppendAsNewColumn implements PredicateProvider {
            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getEnum(Ref.class).isOneOf(OutputPlacement.AppendAsNewColumn);
            }
        }
    }

    private static final class OutputPlacementOptionsPersistor implements NodeSettingsPersistor<OutputPlacement> {

        private static final String OUTPUT_PLACEMENT_CFGKEY = "outputPlacement";

        @Override
        public OutputPlacement load(final NodeSettingsRO settings) throws InvalidSettingsException {
            String outputPlacement = settings.getString(OUTPUT_PLACEMENT_CFGKEY);
            try {
                return OutputPlacement.valueOf(outputPlacement);
            } catch (IllegalArgumentException ex) {
                throw new InvalidSettingsException(
                    "Unrecognized option \"" + outputPlacement + "\" for output placement selection.", ex);
            }
        }

        @Override
        public void save(final OutputPlacement obj, final NodeSettingsWO settings) {
            if (obj == null) {
                settings.addString(OUTPUT_PLACEMENT_CFGKEY, OutputPlacement.ReplaceBoth.name());
                return;
            }
            settings.addString(OUTPUT_PLACEMENT_CFGKEY, obj.name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{OUTPUT_PLACEMENT_CFGKEY}};
        }
    }

    /**
     * Create an instance with default values.
     */
    public ColumnMergerNodeSettings() {
        this((DataTableSpec)null);
    }

    ColumnMergerNodeSettings(final DefaultNodeSettingsContext context) {
        this(context.getDataTableSpec(0).orElse(null));
    }

    ColumnMergerNodeSettings(final DataTableSpec spec) {
        if (spec == null) {
            return;
        }

        int numCols = spec.getNumColumns();
        if (numCols == 0) {
            return;
        }

        final var lastCol = spec.getColumnNames()[numCols - 1]; //return the last column
        m_primaryColumn = lastCol;
        m_secondaryColumn = lastCol;
    }
}
