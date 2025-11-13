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
 *   Nov 13, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.groupby.common;

import java.util.function.Supplier;

import org.apache.commons.lang.StringEscapeUtils;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.base.node.preproc.groupby.Sections;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * The bottom settings of the GroupBy node parameters. Public to be reused also by related parameters (e.g. Pivot).
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
public final class GroupByAdditionalParameters implements NodeParameters {

    @Layout(Sections.Output.class)
    @Modification.WidgetReference(ColumnNamePolicyWidget.class)
    @Widget(title = "Column naming", description = """
            The name of the resulting aggregation column(s) depends on the
            selected naming schema.
            <ul>
                <li>Keep original name(s):
                Keeps the original column names.
                Note that you can use all aggregation columns only once with
                this column naming option to prevent duplicate column names.
                </li>
                <li>Aggregation method (column name):
                Uses the aggregation method first and appends the column name
                in brackets
                </li>
                <li>Column name (aggregation method):
                Uses the column name first and appends the aggregation method
                in brackets
                </li>
            </ul>
            All aggregation methods get a * appended if the missing value option
            is not ticked in the aggregation settings in order to distinguish
            between columns that considered missing values in the aggregation
            process and columns that do not.
            """)
    @Persistor(LegacyColumnNamePolicyPersistor.class)
    ColumnNamePolicy m_columnNamePolicy = ColumnNamePolicy.getDefault();

    interface ColumnNamePolicyWidget extends Modification.Reference {
    }

    /**
     * Use this modification to change the title of the ColumnNamePolicy widget.
     */
    public static abstract class ChangeColumnNamePolicyTitleModification implements Modification.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            group.find(ColumnNamePolicyWidget.class).modifyAnnotation(Widget.class)
                .withProperty("title", getColumnNamePolicyTitle()).modify();
        }

        /**
         * The new title for the ColumnNamePolicy widget.
         *
         * @return the new title
         */
        protected abstract String getColumnNamePolicyTitle();

    }

    @Layout(Sections.Output.class)
    @Widget(title = "Maximum unique values per group", description = """
            Defines the maximum number of unique values per group to avoid
            problems with memory overloading. All groups with more unique
            values are skipped during the calculation and a missing value is set
            in the corresponding column, and a warning is displayed.
            """)
    @NumberInputWidget()
    @Persist(configKey = "maxNoneNumericalVals")
    int m_maxUniqueValues = 100000;

    static final class Delimiter implements NodeParameters {

        @Widget(title = "Value delimiter",
            description = "The value delimiter used by aggregation methods such as concatenate.")
        @TextInputWidget
        String m_delimiter;

        // This is the current version of the GroupBy node to which we are fully compatible,
        // hence we need to keep the same version number.
        // Version 0 did not have this field, so we must persist it in order to identify
        // ourselves as version 1.
        // The field was introduced to change the escaping of the delimiter
        // (version 1 supports control characters (Bug 4865 -- no Jira))
        int m_version = 1;

        Delimiter() {
            // for framework
        }

        // for default value
        Delimiter(final String delimiter) {
            m_delimiter = delimiter;
        }

        // for persistor
        Delimiter(final int version, final String delimiter) {
            m_version = version;
            m_delimiter = delimiter;
        }

    }

    static final class DelimiterPersistor implements NodeParametersPersistor<GroupByAdditionalParameters.Delimiter> {

        private static final String CFG_VERSION = "nodeVersion";

        private static final String CFG_VALUE_DELIMITER = "valueDelimiter";

        // workaround for UIEXT-3012 and handling of GroupBy node version specific persistence...
        @Override
        public GroupByAdditionalParameters.Delimiter load(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            // version 0 did not have this field
            final var version = settings.getInt(CFG_VERSION, 0);
            final var delim = settings.getString(CFG_VALUE_DELIMITER, GlobalSettings.STANDARD_DELIMITER);
            // we need to escape, otherwise the Frontend will display the invisible control characters as-is
            return new Delimiter(version, version == 0 ? delim : StringEscapeUtils.escapeJava(delim));
        }

        @Override
        public void save(final GroupByAdditionalParameters.Delimiter param, final NodeSettingsWO settings) {
            // the frontend sends us escaped control characters
            final var version = param.m_version;
            final var delim = param.m_delimiter;

            final var toSave = version == 0 ? delim : StringEscapeUtils.unescapeJava(delim);
            settings.addString(CFG_VALUE_DELIMITER, toSave);
            settings.addInt(CFG_VERSION, version);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_VALUE_DELIMITER}};
        }

    }

    @Persistor(GroupByAdditionalParameters.DelimiterPersistor.class)
    @Layout(Sections.Output.class)
    GroupByAdditionalParameters.Delimiter m_valueDelimiter = new Delimiter(GlobalSettings.STANDARD_DELIMITER);

    @Layout(Sections.Performance.class)
    @Widget(title = "Enable hiliting", description = """
            If enabled, the hiliting of a group row will hilite all rows of this
            group in other views. Depending on the number of rows, enabling this
            feature might consume a lot of memory.
            """)
    @Persist(configKey = "enableHilite")
    boolean m_enableHiliting;

    @Layout(Sections.Performance.class)
    @Widget(title = "Process in memory", description = """
            Process the table in the memory. Requires more memory but is faster
            since the table needs not to be sorted prior aggregation.
            The memory consumption depends on the number of unique groups and
            the chosen aggregation method. The row order of the input table is
            automatically retained.
            """)
    @ValueReference(GroupByAdditionalParameters.ProcessInMemoryRef.class)
    @Persist(configKey = "inMemory")
    boolean m_processInMemory;

    static final class ProcessInMemoryRef implements ParameterReference<Boolean> {
        // empty class used for EffectPredicateProvider
    }

    @Layout(Sections.Performance.class)
    @Widget(title = "Retain row order", description = """
            Retains the original row order of the input table.
            Could result in longer execution time.
            The row order is automatically retained if the process in memory
            option is selected.
            """)
    @Effect(predicate = GroupByAdditionalParameters.ProcessInMemoryEffect.class, type = Effect.EffectType.DISABLE)
    @ValueProvider(GroupByAdditionalParameters.ProcessInMemoryEffect.class)
    @ValueReference(GroupByAdditionalParameters.RetainOrderRef.class)
    @Persist(configKey = "retainOrder")
    boolean m_retainOrder;

    static final class RetainOrderRef implements ParameterReference<Boolean> {
    }

    static final class ProcessInMemoryEffect implements EffectPredicateProvider, StateProvider<Boolean> {

        private Supplier<Boolean> m_processInMemoryChange;

        private Supplier<Boolean> m_currentRetain;

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(GroupByAdditionalParameters.ProcessInMemoryRef.class).isTrue();
        }

        @Override
        public void init(final StateProviderInitializer init) {
            m_processInMemoryChange =
                init.computeFromValueSupplier(GroupByAdditionalParameters.ProcessInMemoryRef.class);
            m_currentRetain = init.getValueSupplier(GroupByAdditionalParameters.RetainOrderRef.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput input) {
            // in-memory implies retain order
            // otherwise, we keep its current value
            return m_processInMemoryChange.get() || m_currentRetain.get();
        }
    }
}
