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
 *  propagation of KNIME.
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.EnumFieldPersistor;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Path to String.
 *
 * @author Jannik Eurich, KNIME GmbH, Berlin, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class PathToStringNodeParameters implements NodeParameters {

    @Section(title = "Column selection")
    interface ColumnSelectionSection {
    }

    @Section(title = "Output")
    @After(ColumnSelectionSection.class)
    interface OutputSection {
    }

    @Layout(ColumnSelectionSection.class)
    @Widget(title = "Path column", description = """
            The Path column that will be converted to a String column.
            """)
    @ChoicesProvider(FSLocationColumnChoicesProvider.class)
    @ValueProvider(ColumnNameAutoGuesser.class)
    @Persist(configKey = PathToStringNodeModel.CFG_SELECTED_COLUMN_NAME)
    String m_selectedColumn;

    @Layout(OutputSection.class)
    @Widget(title = "Output mode", description = """
            Choose whether to append a new column or replace the selected column.
            """)
    @ValueSwitchWidget
    @Persistor(OutputModePersistor.class)
    @ValueReference(OutputModeRef.class)
    OutputMode m_outputMode = OutputMode.APPEND_NEW;

    static final class OutputModeRef implements ParameterReference<OutputMode> {
    };

    @Layout(OutputSection.class)
    @Widget(title = "Output column name", description = """
            The name of the new column to be created.
            """)
    @TextInputWidget(placeholder = "Location")
    @Effect(predicate = OutputModeIsAppendPredicate.class, type = EffectType.SHOW)
    @Persist(configKey = PathToStringNodeModel.CFG_APPENDED_COLUMN_NAME)
    String m_appendedColumnName = "Location";

    @Layout(OutputSection.class)
    @Widget(title = "Create KNIME URL for 'Relative to' and 'Mountpoint' file systems", description = """
            This option is only relevant for paths with the Relative to workflow data area,
            Relative to workflow, Relative to mountpoint or Mountpoint file system. If checked,
            a String is created that contains a KNIME URL. Such a KNIME URL starts with "knime://"
            and can be used to e.g. control legacy reader nodes via flow variables. If unchecked,
            a String is created that contains solely the path, i.e. without the knime protocol
            and hostname. Such a String can e.g. be used for manipulations and converted back to
            a Path using the String to Path node.
            """)
    @Persist(configKey = PathToStringNodeModel.CFG_CREATE_KNIME_URL)
    boolean m_createKNIMEUrl = true;

    enum OutputMode {
            @Label(value = "Append column", description = "Append the new column to the table.")
            APPEND_NEW, @Label(value = "Replace selected column",
                description = "Replace the selected column with the new String column.")
            REPLACE_SELECTED;

    }

    static final class FSLocationColumnChoicesProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            return ((Collection<DataColumnSpec>)context.getInPortSpecs()[0] //
            ).stream() //
                .filter(colSpec -> colSpec.getType().isCompatible(FSLocationValue.class)) //
                .toList();
        }
    }

    static final class ColumnNameAutoGuesser extends ColumnNameAutoGuessValueProvider {

        ColumnNameAutoGuesser() {
            super(SelectedColumnRef.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            // TODO Auto-generated method stub
            return Optional.empty();
        }
    }

    static final class OutputModePersistor implements NodeParametersPersistor<OutputMode> {

        static final EnumFieldPersistor<OutputMode> INSTANCE =
            new EnumFieldPersistor<>(PathToStringNodeModel.CFG_GENERATED_COLUMN_MODE, OutputMode.class, false);

        @Override
        public OutputMode load(final NodeSettingsRO settings) throws InvalidSettingsException {

            return INSTANCE.load(settings);
        }

        @Override
        public void save(final OutputMode param, final NodeSettingsWO settings) {

            INSTANCE.save(param, settings);
        }

        @Override
        public String[][] getConfigPaths() {

            return new String[][]{{PathToStringNodeModel.CFG_GENERATED_COLUMN_MODE}};
        }

    }

    static final class OutputModeIsAppendPredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {

            return i.getEnum(OutputModeRef.class).isOneOf(
                org.knime.filehandling.utility.nodes.pathtostring.PathToStringNodeParameters.OutputMode.APPEND_NEW);
        }

    }

    static final class SelectedColumnRef implements ParameterReference<String> {
    }

}
