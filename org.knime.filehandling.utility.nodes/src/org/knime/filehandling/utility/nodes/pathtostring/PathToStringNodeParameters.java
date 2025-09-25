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
    
package org.knime.filehandling.utility.nodes.pathtostring;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Path to String.
 * 
 * @author Marc Lehner, KNIME GmbH, Zurich, Switzerland
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
class PathToStringNodeParameters implements NodeParameters {

    PathToStringNodeParameters() {
    }

    PathToStringNodeParameters(final NodeParametersInput context) {
        var inputSpec = context.getInTableSpec(0);
        if (inputSpec.isPresent()) {
            var spec = inputSpec.get();
            // Auto-select first path column if available
            var pathColumn = spec.stream()
                .filter(col -> col.getType().isCompatible(FSLocationValue.class))
                .map(DataColumnSpec::getName)
                .findFirst();
            if (pathColumn.isPresent()) {
                m_selectedColumn = pathColumn.get();
            }
        }
    }

    @Section(title = "Column Selection")
    interface ColumnSelectionSection {
    }

    @Section(title = "Output")
    interface OutputSection {
    }

    // Column selection
    @Widget(title = "Column", description = "The Path column that will be converted to a String column.")
    @ChoicesProvider(PathColumnsProvider.class)
    @Persist(configKey = "selected_column_name")
    String m_selectedColumn;

    // Generate column mode
    @Widget(title = "Output mode", description = "Choose whether to append a new column or replace the selected column.")
    @RadioButtonsWidget
    @ValueReference(GenerateColumnModeRef.class)
    @Persist(configKey = "generated_column_mode")
    GenerateColumnMode m_columnMode = GenerateColumnMode.APPEND_NEW;

    // Appended column name
    @Widget(title = "New column name", description = "The name of the new column to be appended.")
    @TextInputWidget
    @Effect(signals = GenerateColumnModeRef.class, type = EffectType.SHOW, predicate = IsAppendMode.class)
    @Persist(configKey = "appended_column_name")
    String m_appendedColumnName = "Location";

    // Create KNIME URL checkbox
    @Widget(title = "Create KNIME URL for 'Relative to' and 'Mountpoint' file systems", 
            description = """
                This option is only relevant for paths with the Relative to workflow data area, Relative to
                workflow, Relative to mountpoint or Mountpoint file system. If checked, a String is created that
                contains a KNIME URL. Such a KNIME URL starts with "knime://" and can be used to e.g. control
                legacy reader nodes via flow variables. If unchecked, a String is created that contains solely
                the path, i.e. without the knime protocol and hostname. Such a String can e.g. be used for
                manipulations and converted back to a Path using the String to Path node.
                """)
    @Persist(configKey = "create_knime_url")
    boolean m_createKNIMEUrl = true;

    /**
     * Enum for column generation mode options.
     */
    enum GenerateColumnMode {
        @Label("Append column:")
        APPEND_NEW,

        @Label("Replace selected column")
        REPLACE_SELECTED
    }

    /**
     * Provider for path-compatible columns.
     */
    static final class PathColumnsProvider extends CompatibleColumnsProvider {
        protected PathColumnsProvider() {
            super(FSLocationValue.class);
        }
    }

    /**
     * Reference for the column mode setting.
     */
    static final class GenerateColumnModeRef implements ValueReference<GenerateColumnMode> {
        @Override
        public GenerateColumnMode getValue(final ParameterReference parameterReference) {
            return ((PathToStringNodeParameters)parameterReference.getParameters()).m_columnMode;
        }
    }

    /**
     * Predicate to check if append mode is selected.
     */
    static final class IsAppendMode implements org.knime.node.parameters.updates.EffectPredicate {
        @Override
        public boolean isTrue(final ParameterReference parameterReference) {
            var params = (PathToStringNodeParameters)parameterReference.getParameters();
            return params.m_columnMode == GenerateColumnMode.APPEND_NEW;
        }
    }
}
