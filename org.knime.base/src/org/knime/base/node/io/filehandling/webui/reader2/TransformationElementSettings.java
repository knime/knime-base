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
 *   Feb 26, 2026 (Thomas Reifenberger): extracted from TransformationParameters
 */
package org.knime.base.node.io.filehandling.webui.reader2;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.datatype.convert.ProductionPathUtils;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ProductionPathSerializer;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Paul Bärnreuther
 * @since 5.12
 * @noreference non-public API
 *
 */
public class TransformationElementSettings implements WidgetGroup, Persistable {

    static class ColumnNameRef implements ParameterReference<String> {
    }

    static final class ColumnNameIsNull implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getString(ColumnNameRef.class).isEqualTo(null);
        }
    }

    /**
     * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
     */
    @ValueReference(ColumnNameRef.class)
    @JsonInclude(JsonInclude.Include.ALWAYS) // Necessary for the ColumnNameIsNull PredicateProvider to work
    public String m_columnName;

    static class OriginalProductionPathRef implements ParameterReference<String> {
    }

    /**
     * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
     */
    @ValueReference(OriginalProductionPathRef.class)
    @Persistor(OriginalPathPersistor.class)
    public String m_originalProductionPath;

    static final class OriginalPathPersistor extends ProductionPathUtils.ProductionPathPersistor {

        private static final String CFG_ORIGINAL_PRODUCTION_PATH = "originalProductionPath";

        OriginalPathPersistor() {
            super(CFG_ORIGINAL_PRODUCTION_PATH);
        }

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(CFG_ORIGINAL_PRODUCTION_PATH)) {
                return super.load(settings);
            }
            return TransformationParametersStateProviders.TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID;
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            if (TransformationParametersStateProviders.TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID.equals(param)) {
                // do not save default value
                return;
            }
            super.save(param, settings);
        }
    }

    static class OriginalProductionPathLabelRef implements ParameterReference<String> {
    }

    @ValueReference(OriginalProductionPathLabelRef.class)
    String m_originalTypeLabel;

    /**
     * Visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
     */
    @Widget(title = "Include in output", description = "") // TODO NOSONAR UIEXT-1901 add description
    @ArrayWidgetInternal.ElementCheckboxWidget
    public boolean m_includeInOutput;

    static final class ColumnNameResetter implements StateProvider<String> {

        private Supplier<String> m_originalColumnNameSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(ArrayWidgetInternal.ElementResetButton.class);
            m_originalColumnNameSupplier = initializer.getValueSupplier(ColumnNameRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            return m_originalColumnNameSupplier.get();
        }
    }

    static final class TypeResetter implements StateProvider<String> {

        private Supplier<String> m_originalTypeSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(ArrayWidgetInternal.ElementResetButton.class);
            m_originalTypeSupplier = initializer.getValueSupplier(OriginalProductionPathRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            return m_originalTypeSupplier.get();
        }
    }

    static final class TitleProvider implements StateProvider<String> {

        private Supplier<String> m_originalColumnNameSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnValueChange(TransformationParameters.TableSpecSettingsRef.class);
            m_originalColumnNameSupplier = initializer.getValueSupplier(ColumnNameRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            final var originalName = m_originalColumnNameSupplier.get();
            return originalName == null ? "Any unknown column" : originalName;
        }
    }

    static final class SubTitleProvider implements StateProvider<String> {

        private Supplier<String> m_originalTypeLabelSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnValueChange(TransformationParameters.TableSpecSettingsRef.class);
            m_originalTypeLabelSupplier = initializer.getValueSupplier(OriginalProductionPathLabelRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            return m_originalTypeLabelSupplier.get();
        }
    }

    static final class ElementIsEditedAndColumnNameIsNotNull implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(ArrayWidgetInternal.ElementIsEdited.class)
                    .and(i.getPredicate(ColumnNameIsNull.class).negate());
        }
    }

    /**
     * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
     */
    @Widget(title = "Column name", description = "")
    @WidgetInternal(hideControlHeader = true)
    @ValueProvider(ColumnNameResetter.class)
    @Effect(predicate = ElementIsEditedAndColumnNameIsNotNull.class, type = Effect.EffectType.SHOW)
    @JsonInclude(JsonInclude.Include.ALWAYS) // Necessary for comparison against m_columnName
    @TextInputWidget(patternValidation = ColumnNameValidationUtils.ColumnNameValidation.class)
    public String m_columnRename;

    /**
     * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
     */
    @Widget(title = "Column type", description = "")
    @WidgetInternal(hideControlHeader = true)
    // for adding dynamic choices
    @Modification.WidgetReference(TransformationParametersStateProviders.TransformationSettingsWidgetModification.TypeChoicesWidgetRef.class)
    @ValueProvider(TypeResetter.class)
    @Effect(predicate = ArrayWidgetInternal.ElementIsEdited.class, type = Effect.EffectType.SHOW)
    @Persistor(PathPersistor.class)
    public String m_productionPath;

    static final class PathPersistor extends ProductionPathUtils.ProductionPathPersistor {

        private static final String CFG_UNKNOWNS_COLUMN_DATA_TYPE = "unknownsColumnDataType";

        private static final String CFG_PRODUCTION_PATH = "productionPath";

        PathPersistor() {
            super(CFG_PRODUCTION_PATH);
        }

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(CFG_UNKNOWNS_COLUMN_DATA_TYPE)) {
                return settings.getString(CFG_UNKNOWNS_COLUMN_DATA_TYPE);
            }
            return super.load(settings);
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            if (ProductionPathUtils.isPathIdentifier(param)) {
                super.save(param, settings);
            } else {
                settings.addString(CFG_UNKNOWNS_COLUMN_DATA_TYPE, param);
            }
        }
    }

    TransformationElementSettings() {
    }

    /**
     * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
     */
    @SuppressWarnings("javadoc")
    public TransformationElementSettings(final String columnName, final boolean includeInOutput,
                                         final String columnRename, final String type, final String originalType, final String originalTypeLabel) {
        m_columnName = columnName;
        m_includeInOutput = includeInOutput;
        m_columnRename = columnRename;
        m_productionPath = type; // converter fac id
        m_originalProductionPath = originalType; // converter fac id
        m_originalTypeLabel = originalTypeLabel;
    }

    /**
     * Constructor for concrete columns.
     *
     * @param columnName
     * @param includeInOutput
     * @param columnRename
     * @param productionPath
     * @param defaultProductionPath
     * @param productionPathSerializer
     */
    TransformationElementSettings(final String columnName, final boolean includeInOutput, final String columnRename,
                                  final ProductionPath productionPath, final ProductionPath defaultProductionPath,
                                  final ProductionPathSerializer productionPathSerializer) {
        this(columnName, includeInOutput, columnRename, //
                ProductionPathUtils.getPathIdentifier(productionPath, productionPathSerializer), //
                ProductionPathUtils.getPathIdentifier(defaultProductionPath, productionPathSerializer), //
                defaultProductionPath.getDestinationType().toPrettyString() //
        );
    }

    /**
     * Constructor for unknown columns.
     *
     * @param includeInOutput
     * @param dataType        Override data type for unknown columns. Leave null for using the default type.
     */
    TransformationElementSettings(final boolean includeInOutput, final DataType dataType) {
        this(null, includeInOutput, null,
                dataType == null ? TransformationParametersStateProviders.TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID : TransformationParameters.getDataTypeId(dataType),
                TransformationParametersStateProviders.TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID, //
                TransformationParametersStateProviders.TypeChoicesProvider.DEFAULT_COLUMNTYPE_TEXT //
        );
    }

    /**
     * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
     */
    public static TransformationElementSettings createUnknownElement() {
        return new TransformationElementSettings(true, null);
    }

    /**
     * A "data copy" of the {@link TransformationElementSettings}, but without all the UI specific annotations. Just
     * reusing the {@link TransformationElementSettings} directly does not work, as then some references show up twice
     * and therefore widget modifications don't work.
     */
    static class Data {
        String m_columnName;

        String m_originalProductionPath;

        String m_originalTypeLabel;

        boolean m_includeInOutput;

        String m_columnRename;

        String m_productionPath;

        private Data() {
            // needed by framework
        }

        Data(final TransformationElementSettings settings) {
            m_columnName = settings.m_columnName;
            m_originalProductionPath = settings.m_originalProductionPath;
            m_originalTypeLabel = settings.m_originalTypeLabel;
            m_includeInOutput = settings.m_includeInOutput;
            m_columnRename = settings.m_columnRename;
            m_productionPath = settings.m_productionPath;
        }

        @SuppressWarnings("java:S1067") // number of conditional operators
        private boolean matches(TransformationElementSettings other) {
            return Objects.equals(m_columnName, other.m_columnName)
                && Objects.equals(m_originalProductionPath, other.m_originalProductionPath)
                && Objects.equals(m_originalTypeLabel, other.m_originalTypeLabel)
                && m_includeInOutput == other.m_includeInOutput && Objects.equals(m_columnRename, other.m_columnRename)
                && Objects.equals(m_productionPath, other.m_productionPath);
        }

        static boolean areSettingsMatching(Data[] data,
                                           TransformationElementSettings[] settings) {
            if (data.length != settings.length) {
                return false;
            }
            for (int i = 0; i < data.length; i++) {
                if (!data[i].matches(settings[i])) {
                    return false;
                }
            }
            return true;
        }

        static class DoNotPersist implements NodeParametersPersistor<Data[]> {

            @Override
            public Data[] load(NodeSettingsRO settings) throws InvalidSettingsException {
                return new Data[0];
            }

            @Override
            public void save(Data[] param, NodeSettingsWO settings) {
                // do nothing
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[0][];
            }
        }
    }
}
