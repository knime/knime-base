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
 *   Dec 3, 2025 (paulbaernreuther): created
 */
package org.knime.base.node.preproc.pmml.missingval.compute;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.knime.base.node.preproc.pmml.missingval.MissingCellHandlerFactoryManager;
import org.knime.base.node.preproc.pmml.missingval.handlers.DoNothingMissingCellHandlerFactory;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.FallbackDialogNodeParameters;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DefaultClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicParametersWithFallbackProvider;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;

/**
 * It's required to attach a {@link MissingValueTreatmentModifier} via @Modification to every case where this class is
 * used to provide the choices for the factory ID and the factory-specific settings widget.
 *
 */
@SuppressWarnings({"restriction", "javadoc"})
class MissingValueTreatment implements NodeParameters {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MissingValueTreatment.class);

    MissingValueTreatment() {
        // default constructor
    }

    MissingValueTreatment(final MissingValueTreatment other) {
        m_factoryID = other.m_factoryID;
    }

    MissingValueTreatment(final String factoryId) {
        m_factoryID = factoryId;
    }

    boolean isDefault() {
        return DoNothingMissingCellHandlerFactory.getInstance().getID().equals(m_factoryID);
    }

    @Widget(title = "Treatment",
        description = "Choose the missing value treatment strategy. "
            + "Some strategies require additional parameters which can be set below after selecting the strategy."//
            + "<br/><br/>" //
            + "Options marked with an asterisk (*) will result in non-standard PMML, "
            + "which uses extensions that cannot be read by other tools than KNIME.")
    @Modification.WidgetReference(FactoryIDWidget.class)
    String m_factoryID = DoNothingMissingCellHandlerFactory.getInstance().getID();

    interface FactoryIDWidget extends Modification.Reference {
        // marker interface
    }

    interface FactoryIDRef extends ParameterReference<String> {
    }

    /**
     * Used in the {@link MissingValueTreatmentModifier} to provide the choices for the factory ID selection.
     */
    abstract static class FactoryChoicesProvider implements StringChoicesProvider {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
        }

        abstract DataType[] getDataTypes(NodeParametersInput context);

        @Override
        public List<StringChoice> computeState(final NodeParametersInput context) {
            final var dataTypes = getDataTypes(context);
            if (dataTypes.length == 0) {
                final var doNothingFacory = DoNothingMissingCellHandlerFactory.getInstance();
                return List.of(new StringChoice(doNothingFacory.getID(), doNothingFacory.toString()));
            }
            final var factories = getFactoryManager().getFactoriesSorted(dataTypes);
            return factories.stream().map(f -> new StringChoice(f.getID(), f.toString())).toList();
        }

    }

    private static final String FACTORY_SETTINGS_CFG_KEY = "settings";

    @Modification.WidgetReference(FactorySettingsWidget.class)
    @Migration(LoadFallbackSettingsMigration.class)
    @DynamicParameters(DummyFactoryDynamicSettingsProvider.class)
    @Persist(configKey = FACTORY_SETTINGS_CFG_KEY)
    MissingValueTreatmentParameters m_parameters;

    interface FactorySettingsWidget extends Modification.Reference {
        // marker interface
    }

    interface FactorySettingsRef extends ParameterReference<MissingValueTreatmentParameters> {
    }

    static final class LoadFallbackSettingsMigration
        implements NodeParametersMigration<MissingValueTreatmentParameters> {

        @Override
        public List<ConfigMigration<MissingValueTreatmentParameters>> getConfigMigrations() {
            return List.of(ConfigMigration
                .<MissingValueTreatmentParameters> builder(settings -> new LegacyMissingValueTreatmentParameters(
                    settings.getNodeSettings(FACTORY_SETTINGS_CFG_KEY)))
                .withMatcher(settings -> {
                    try {
                        return !settings.getNodeSettings(FACTORY_SETTINGS_CFG_KEY).containsKey("@class");
                    } catch (InvalidSettingsException e) { // NOSONAR No "settings"
                        return false;
                    }
                }).build());
        }

    }

    static final class DummyFactoryDynamicSettingsProvider extends FactoryDynamicSettingsProvider {

        @Override
        protected Class<? extends FactorySettingsRef> getParameterRefClass() {
            throw new UnsupportedOperationException("Replace this dummy by using the MissingValueTreatmentModifier!");
        }

        @Override
        protected Class<? extends FactoryIDRef> getFactoryIDParameterRefClass() {
            throw new UnsupportedOperationException("Replace this dummy by using the MissingValueTreatmentModifier!");
        }

        @Override
        Class<? extends FactoryChoicesProvider> getChoicesProviderClass() {
            throw new UnsupportedOperationException("Replace this dummy by using the MissingValueTreatmentModifier!");
        }

    }

    abstract static class FactoryDynamicSettingsProvider
        implements DynamicParametersWithFallbackProvider<MissingValueTreatmentParameters> {

        private Supplier<MissingValueTreatmentParameters> m_settingsSupplier;

        private Supplier<String> m_factoryIDSupplier;

        protected abstract Class<? extends FactorySettingsRef> getParameterRefClass();

        protected abstract Class<? extends FactoryIDRef> getFactoryIDParameterRefClass();

        abstract Class<? extends FactoryChoicesProvider> getChoicesProviderClass();

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_settingsSupplier = initializer.getValueSupplier(getParameterRefClass());
            m_factoryIDSupplier = initializer.computeFromValueSupplier(getFactoryIDParameterRefClass());
        }

        @Override
        public ClassIdStrategy<MissingValueTreatmentParameters> getClassIdStrategy() {
            final List<Class<? extends MissingValueTreatmentParameters>> allClasses = new ArrayList<>();
            allClasses.add(LegacyMissingValueTreatmentParameters.class);

            // Collect parameter classes from all registered factories
            for (var factory : getFactoryManager().getFactories()) {
                final var paramClass = factory.getParametersClass();
                if (paramClass != null) {
                    allClasses.add(paramClass);
                }
            }

            return new DefaultClassIdStrategy<>(allClasses);
        }

        @Override
        public MissingValueTreatmentParameters computeParameters(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var currentFactoryID = m_factoryIDSupplier.get();
            if (currentFactoryID == null) {
                // no factory selected yet, abort update
                throw new StateComputationFailureException();
            }
            final var factory = getFactoryManager().getFactoryByID(currentFactoryID);
            if (factory == null) {
                LOGGER.warn("Unknown missing value handler factory: " + currentFactoryID);
                throw new StateComputationFailureException();
            }
            if (!factory.hasSettingsPanel()) {
                return null;
            }

            final var currentValue = m_settingsSupplier.get();

            final var paramClass = factory.getParametersClass();
            if (paramClass != null && currentValue != null && paramClass.isInstance(currentValue)) {
                return currentValue;
            } else if (paramClass != null) {
                if (currentValue instanceof LegacyMissingValueTreatmentParameters legacy) {
                    try {
                        return NodeParametersUtil.loadSettings(legacy.getNodeSettings(), paramClass);
                    } catch (InvalidSettingsException e) { // NOSONAR best-effort
                        // fall-through: cannot re-use settings
                        // When switching the factory this can be expected
                    }
                }
                try {
                    return NodeParametersUtil.createSettings(paramClass, parametersInput);
                } catch (final Exception e) { // NOSONAR we want to be safe and rather fall back to the fallback dialog
                    LOGGER.warn(() -> String.format(
                        "Failed to instantiate parameter class \"%s\", falling back to legacy parameters",
                        paramClass.getName()), e);
                }
            }

            final var settings = new NodeSettings("factory settings");
            final var settingsPanel = factory.getSettingsPanel();
            try {
                settingsPanel.saveSettingsTo(settings);
            } catch (final InvalidSettingsException e) {
                LOGGER.error(
                    "Failed to save fallback default settings of missing value handler factory " + factory.getID(), e);
            }
            final var toBeUsedSettings = currentValue instanceof LegacyMissingValueTreatmentParameters legacy
                && legacy.hasSameFallbackDialog(settings) //
                    ? legacy.getNodeSettings()//
                    : settings;
            return new LegacyMissingValueTreatmentParameters(toBeUsedSettings);

        }

        @Override
        public NodeSettings computeFallbackSettings(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var params = computeParameters(parametersInput);
            if (params instanceof LegacyMissingValueTreatmentParameters legacy) {
                return legacy.getNodeSettings();
            }
            // no fallback "dialog" needed (no factory parameters or new parameters based)
            return null;
        }

        @Override
        public FallbackDialogNodeParameters getParametersFromFallback(final NodeSettingsRO fallbackSettings) {
            return new LegacyMissingValueTreatmentParameters(fallbackSettings);
        }

    }

    /**
     * It's required to attach this via @Modification to every case where {@link MissingValueTreatment} is used.
     */
    abstract static class MissingValueTreatmentModifier implements Modification.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            final var factoryIDWidget = group.find(FactoryIDWidget.class);
            factoryIDWidget.addAnnotation(ChoicesProvider.class).withValue(getChoicesProviderClass()).modify();
            factoryIDWidget.addAnnotation(ValueReference.class).withValue(getFactoryIDParameterRefClass()).modify();
            final var settingsWidget = group.find(FactorySettingsWidget.class);
            settingsWidget.modifyAnnotation(DynamicParameters.class).withValue(getDynamicSettingsProviderClass())
                .modify();
            settingsWidget.addAnnotation(ValueReference.class).withValue(getParameterRefClass()).modify();
        }

        abstract Class<? extends FactoryChoicesProvider> getChoicesProviderClass();

        abstract Class<? extends FactoryDynamicSettingsProvider> getDynamicSettingsProviderClass();

        abstract Class<? extends FactoryIDRef> getFactoryIDParameterRefClass();

        abstract Class<? extends FactorySettingsRef> getParameterRefClass();

    }

    private static MissingCellHandlerFactoryManager getFactoryManager() {
        return MissingCellHandlerFactoryManager.getInstance();
    }

}
