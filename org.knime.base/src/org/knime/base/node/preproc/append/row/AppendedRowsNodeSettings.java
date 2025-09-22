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
 *   21 Dec 2022 (ivan): created
 */
package org.knime.base.node.preproc.append.row;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Currently only used for the node dialogue, backwards compatible loading is ensured by the node model. If this is ever
 * used for the node model, backwards compatible loading will need to be implemented.
 *
 * @author Jonas Klotz, KNIME GbmH, Berlin, Germany
 * @author Ivan Prigarin, KNIME GbmH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GbmH, Konstanz, Germany
 * @since 5.1
 */
@SuppressWarnings("restriction")
public final class AppendedRowsNodeSettings implements NodeParameters {

    enum ColumnSetOperation {
            @Label("Union")
            UNION,

            @Label("Intersection")
            INTERSECTION;
    }

    @Persistor(ColumnSetOperationPersistor.class)
    @Widget(title = "How to combine input columns", description = """
            Choose the output column selection process:<ul>
            <li><b>Union</b>: Use all columns from all input tables. Fill rows with missing values if they miss cells
                for some columns.</li>
            <li><b>Intersection</b>: Use only the columns that appear in every input table. Any other column is ignored
                and won't appear in the output table.</li>
            </ul>
            """)
    @ValueSwitchWidget
    ColumnSetOperation m_columnSetOperation = ColumnSetOperation.UNION;

    @Persistor(RowIdResolutionPersistor.class)
    RowIdStrategySelection m_rowIdStrategy = new RowIdStrategySelection();

    @Persist(configKey = AppendedRowsNodeModel.CFG_HILITING)
    @Migrate(loadDefaultIfAbsent = true)
    @Widget(title = "Enable hiliting",
        description = "Enable hiliting between both inputs and the concatenated output table.", advanced = true)
    boolean m_enableHiliting = false; //NOSONAR being explicit is desired here

    private static final class RowIdStrategySelection implements WidgetGroup {
        enum RowIdStrategy {
                @Label("Create new")
                CREATE_NEW,

                @Label("Reuse existing")
                REUSE_EXISTING;
        }

        interface RowIdStrategyRef extends ParameterReference<RowIdStrategy> {

        }

        @Widget(title = "RowID handling", description = """
                Choose how to handle RowIDs:
                <ul>
                    <li><b>Create new:</b> Discard the RowIDs of the input tables and generate new RowIDs</li>
                    <li><b>Reuse existing:</b> Reuse the RowIDs of the input tables. This might lead to conflicts due to
                        duplicate RowIDs, see <em>Duplicate RowID strategy</em> for different ways to resolve them.</li>
                </ul>
                """)
        @ValueSwitchWidget
        @ValueReference(RowIdStrategyRef.class)
        RowIdStrategy m_strategy = RowIdStrategy.CREATE_NEW;

        enum DuplicateRowIdResolution {
                @Label("Append suffix")
                APPEND,

                @Label("Skip")
                SKIP,

                @Label("Fail")
                FAIL;
        }

        interface DuplicateRowIdResolutionRef extends ParameterReference<DuplicateRowIdResolution> {
        }

        static final class ReuseExisting implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(RowIdStrategyRef.class).isOneOf(RowIdStrategy.REUSE_EXISTING);
            }
        }

        static final class ReusedExistingAndAppend implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                final var isAppend =
                    i.getEnum(DuplicateRowIdResolutionRef.class).isOneOf(DuplicateRowIdResolution.APPEND);
                return isAppend.and(i.getPredicate(ReuseExisting.class));
            }
        }

        @Widget(title = "Duplicate RowID strategy", description = """
                Select how duplicate RowIDs are handled:
                <ul>
                    <li><b>Append suffix</b>: The output table will include all rows, but duplicate RowIDs will have a
                        suffix added. This method is also memory intensive, similar to the "Skip" option.</li>
                    <li><b>Skip</b>: Duplicate RowIDs in the additional tables are not added to the output table. This
                        option is memory intensive because it caches the RowIDs to find duplicates and requires full
                        data duplication.</li>
                    <li><b>Fail</b>: The node will fail during execution if duplicate RowIDs are encountered. This
                        option is efficient for checking uniqueness.</li>
                </ul>
                """)
        @ValueSwitchWidget
        @Effect(predicate = ReuseExisting.class, type = EffectType.SHOW)
        @ValueReference(DuplicateRowIdResolutionRef.class)
        DuplicateRowIdResolution m_rowIdResolution = DuplicateRowIdResolution.APPEND;

        @Persist(configKey = AppendedRowsNodeModel.CFG_SUFFIX)
        @Widget(title = "Suffix", description = "The suffix to be appended to RowIDs.")
        @Effect(predicate = ReusedExistingAndAppend.class, type = EffectType.SHOW)
        String m_suffix = "_dup";

    }

    private static final class RowIdResolutionPersistor implements NodeParametersPersistor<RowIdStrategySelection> {

        @Override
        public RowIdStrategySelection load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var strategy = new RowIdStrategySelection();
            // suffix may be stored in the settings, regardless of the strategy. Reuse that if available.
            strategy.m_suffix = settings.getString(AppendedRowsNodeModel.CFG_SUFFIX, strategy.m_suffix);
            if (settings.getBoolean(AppendedRowsNodeModel.CFG_NEW_ROWIDS, false)) {
                strategy.m_strategy = RowIdStrategySelection.RowIdStrategy.CREATE_NEW;
            } else if (settings.getBoolean(AppendedRowsNodeModel.CFG_FAIL_ON_DUPLICATES, false)) {
                strategy.m_strategy = RowIdStrategySelection.RowIdStrategy.REUSE_EXISTING;
                strategy.m_rowIdResolution = RowIdStrategySelection.DuplicateRowIdResolution.FAIL;
            } else if (settings.getBoolean(AppendedRowsNodeModel.CFG_APPEND_SUFFIX)) {
                strategy.m_strategy = RowIdStrategySelection.RowIdStrategy.REUSE_EXISTING;
                strategy.m_rowIdResolution = RowIdStrategySelection.DuplicateRowIdResolution.APPEND;
                // *require* a suffix to be stored in the settings
                strategy.m_suffix = settings.getString(AppendedRowsNodeModel.CFG_SUFFIX);
            } else {
                strategy.m_strategy = RowIdStrategySelection.RowIdStrategy.REUSE_EXISTING;
                strategy.m_rowIdResolution = RowIdStrategySelection.DuplicateRowIdResolution.SKIP;
            }
            return strategy;
        }

        @Override
        public void save(final RowIdStrategySelection resolution, final NodeSettingsWO settings) {
            settings.addBoolean(AppendedRowsNodeModel.CFG_NEW_ROWIDS,
                resolution.m_strategy == RowIdStrategySelection.RowIdStrategy.CREATE_NEW);
            settings.addBoolean(AppendedRowsNodeModel.CFG_FAIL_ON_DUPLICATES,
                resolution.m_strategy == RowIdStrategySelection.RowIdStrategy.REUSE_EXISTING
                    && resolution.m_rowIdResolution == RowIdStrategySelection.DuplicateRowIdResolution.FAIL);
            settings.addBoolean(AppendedRowsNodeModel.CFG_APPEND_SUFFIX,
                resolution.m_strategy == RowIdStrategySelection.RowIdStrategy.REUSE_EXISTING
                    && resolution.m_rowIdResolution == RowIdStrategySelection.DuplicateRowIdResolution.APPEND);
            // add suffix string (so that it doesn't reset when changing settings, this reflects old behaviour)
            settings.addString(AppendedRowsNodeModel.CFG_SUFFIX, resolution.m_suffix);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{//
                {AppendedRowsNodeModel.CFG_NEW_ROWIDS}, //
                {AppendedRowsNodeModel.CFG_APPEND_SUFFIX}, //
                {AppendedRowsNodeModel.CFG_FAIL_ON_DUPLICATES}, //
                {AppendedRowsNodeModel.CFG_SUFFIX}//
            };
        }
    }

    private static final class ColumnSetOperationPersistor implements NodeParametersPersistor<ColumnSetOperation> {

        @Override
        public ColumnSetOperation load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean(AppendedRowsNodeModel.CFG_INTERSECT_COLUMNS)) {
                return ColumnSetOperation.INTERSECTION;
            } else {
                return ColumnSetOperation.UNION;
            }
        }

        @Override
        public void save(final ColumnSetOperation obj, final NodeSettingsWO settings) {
            settings.addBoolean(AppendedRowsNodeModel.CFG_INTERSECT_COLUMNS, obj == ColumnSetOperation.INTERSECTION);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{AppendedRowsNodeModel.CFG_INTERSECT_COLUMNS}};
        }
    }
}
