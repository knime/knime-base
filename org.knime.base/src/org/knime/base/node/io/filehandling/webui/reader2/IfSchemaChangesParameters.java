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
 *   Nov 25, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.webui.reader2;

import org.knime.core.webui.node.dialog.defaultdialog.persistence.booleanhelpers.DoNotPersistBoolean;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.filehandling.core.node.table.reader.config.AbstractMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Inside;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

import java.util.function.Supplier;

/**
 * Parameters for handling schema changes.
 *
 * @author Paul Bärnreuther
 * @since 5.10
 */
public final class IfSchemaChangesParameters implements NodeParameters {

    /**
     * Reference this interface to position parameters relative to "If schema changes".
     */
    @Inside(ReaderLayout.ColumnAndDataTypeDetection.class)
    public interface IfSchemaChanges {
    }

    static final class IfSchemaChangesOptionRef implements ParameterReference<IfSchemaChangesOption> {
    }

    private static final class IgnoreSchemaChangeRef implements ParameterReference<Boolean> {
    }

    private static final class IgnoreSchemaChangeWasSetInitiallyRef implements ParameterReference<Boolean> {
    }

    @Widget(title = "If schema changes, ignore schema change (deprecated)", description = """
            If set, the node tries to ignore the changes and outputs a table with the old table specification.
            This option is deprecated. Once unchecked, it will disappear and cannot be selected again, as it may lead
            to invalid data in the resulting table.
            """)
    @ValueReference(IgnoreSchemaChangeRef.class)
    @Layout(IfSchemaChanges.class)
    @Effect(predicate = ShowIgnoreSchemaChangePredicate.class, type = Effect.EffectType.SHOW)
    boolean m_ignoreSchemaChange;

    @Widget(title = "If schema changes", description = """
            Specifies the node behavior if the content of the configured file/folder changes between executions,
            i.e., columns are added/removed to/from the file(s) or their types change. The following options are
            available:
            """)
    @ValueSwitchWidget
    @Layout(IfSchemaChanges.class)
    @ValueReference(IfSchemaChangesOptionRef.class)
    @Effect(predicate = ShowIfSchemaChangePredicate.class, type = Effect.EffectType.SHOW)
    IfSchemaChangesOption m_ifSchemaChangesOption = IfSchemaChangesOption.FAIL;

    @Persistor(DoNotPersistBoolean.class)
    @ValueReference(IgnoreSchemaChangeWasSetInitiallyRef.class)
    @ValueProvider(IgnoreSchemaWasSetInitiallyValueProvider.class)
    boolean m_ignoreSchemaChangeWasSetInitially;

    /**
     * Predicate provider for checking if new schema should be used.
     */
    static final class UseNewSchema implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(IfSchemaChangesOptionRef.class).isOneOf(IfSchemaChangesOption.USE_NEW_SCHEMA);
        }
    }

    static final class ShowIfSchemaChangePredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(IgnoreSchemaChangeRef.class).isFalse();
        }
    }

    static final class ShowIgnoreSchemaChangePredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(IgnoreSchemaChangeWasSetInitiallyRef.class).isTrue();
        }
    }

    /**
     * Populate the initial value of IgnoreSchemaChangeWasSetInitially once upon loading the dialog. That way, we can
     * show the deprecated ignore schema change option only if it was set initially. After unchecking it and saving, it
     * will be hidden and cannot be re-enabled via the dialog.
     */
    private static final class IgnoreSchemaWasSetInitiallyValueProvider implements StateProvider<Boolean> {

        Supplier<Boolean> m_ignoreSchemaChange;

        @Override
        public void init(StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_ignoreSchemaChange = initializer.getValueSupplier(IgnoreSchemaChangeRef.class);
        }

        @Override
        public Boolean computeState(NodeParametersInput parametersInput) throws StateComputationFailureException {
            return m_ignoreSchemaChange.get();
        }
    }

    /**
     * Saves the settings to the given config. Call this method in a saveToConfig method of an enclosing parameters
     * class.
     *
     * @param config the config to save to
     */
    public void saveToConfig(final AbstractMultiTableReadConfig<?, ?, ?, ?> config) {
        if (m_ignoreSchemaChange) {
            config.setSaveTableSpecConfig(true);
            config.setCheckSavedTableSpec(false);
        } else {
            switch (m_ifSchemaChangesOption) {
                case USE_NEW_SCHEMA -> {
                    config.setSaveTableSpecConfig(false);
                    config.setCheckSavedTableSpec(false);
                }
                case FAIL -> {
                    config.setSaveTableSpecConfig(true);
                    config.setCheckSavedTableSpec(true);
                }
            }
        }
    }

    /**
     * Load the settings from the given config.
     *
     * @param config the config to load from
     */
    public void loadFromConfig(final MultiTableReadConfig<?, ?> config) {
        if (config.saveTableSpecConfig()) {
            /*
             * The legacy setting is only relevant for the fail option, otherwise the new schema will be used anyway.
             * This is consistent with how that flag was originally mapped to the three radio buttons 
             * "Fail", "Use new schema", "Ignore (deprecated)"
             */
            m_ignoreSchemaChange = !config.checkSavedTableSpec();
            m_ifSchemaChangesOption = IfSchemaChangesOption.FAIL;
        } else {
            m_ignoreSchemaChange = false;
            m_ifSchemaChangesOption = IfSchemaChangesOption.USE_NEW_SCHEMA;
        }
    }

    /**
     * Options for handling schema changes.
     */
    public enum IfSchemaChangesOption {
            /**
             * Fail if schema has changed.
             */
            @Label(value = "Fail", description = """
                    If set, the node fails if the column names in the file have changed. Changes in column types
                    will not be detected.
                    """) //
            FAIL, //
            /**
             * Use the new schema without transformations.
             */
            @Label(value = "Use new schema", description = """
                    If set, the node will compute a new table specification for the current schema of the file at
                    the time when the node is executed. Note that the node will not output a table specification
                    before execution and that it will not apply transformations, therefore the transformation tab
                    is disabled.
                    """) //
            USE_NEW_SCHEMA, //
    }

}
