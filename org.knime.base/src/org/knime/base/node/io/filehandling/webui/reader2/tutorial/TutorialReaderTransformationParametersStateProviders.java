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
 */
package org.knime.base.node.io.filehandling.webui.reader2.tutorial;

import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.webui.reader2.ClassSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.SkipFirstDataRowsParameters;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProviders;
import org.knime.base.node.io.filehandling.webui.reader2.tutorial.TutorialReaderNodeParameters.TutorialReaderParametersRef;
import org.knime.base.node.io.filehandling.webui.reader2.tutorial.TutorialReaderSpecific.ConfigAndReader;
import org.knime.base.node.io.filehandling.webui.reader2.tutorial.TutorialReaderSpecific.ProductionPathProviderAndTypeHierarchy;

/**
 * TODO (#4): Adjust Class<?> to match your TableReader's T type parameter if needed
 *
 * @author KNIME AG, Zurich, Switzerland
 */
final class TutorialReaderTransformationParametersStateProviders {

    static final class TypedReaderTableSpecsProvider
        extends TransformationParametersStateProviders.TypedReaderTableSpecsProvider<//
                DummyTableReaderConfig, Class<?>, DummyMultiTableReadConfig>
        implements ConfigAndReader {

        private Supplier<TutorialReaderParameters> m_readerNodeParamsSupplier;

        @Override
        protected void applyParametersToConfig(final DummyMultiTableReadConfig config) {
            m_readerNodeParamsSupplier.get().saveToConfig(config);
        }

        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            m_readerNodeParamsSupplier = initializer.getValueSupplier(TutorialReaderParametersRef.class);
        }

        interface Dependent
            extends TransformationParametersStateProviders.TypedReaderTableSpecsProvider.Dependent<Class<?>> {
            @SuppressWarnings("unchecked")
            @Override
            default Class<TypedReaderTableSpecsProvider> getTypedReaderTableSpecsProvider() {
                return TypedReaderTableSpecsProvider.class;
            }

            @Override
            default void initConfigIdTriggers(final StateProviderInitializer initializer) {
                // Register triggers for all reader-specific parameters that affect table spec detection
                initializer.computeOnValueChange(SkipFirstDataRowsParameters.SkipFirstDataRowsRef.class);
                /**
                 * TODO (#6): Add triggers for your reader-specific parameters here. Note that not every setting is
                 * relevant for table spec detection! The existing framework calls settings that affect table spec
                 * detection "configId", so a good place to look at for determining which parameter references have to
                 * be listed here is the ConfigIdFactory#createFromConfig method within the factory passed to the
                 * super-constructor of your specific implementation of AbstractMultiTableReadConfig.
                 */
                // Example:
                initializer.computeOnValueChange(MyTutorialSpecificParameters.MyParameterAfterSourceRef.class);
            }
        }
    }

    // TODO (#4): If your T is not Class<?>, replace ClassSerializer with appropriate serializer (e.g., DataTypeSerializer)
    static final class TableSpecSettingsProvider
        extends TransformationParametersStateProviders.TableSpecSettingsProvider<Class<?>>
        implements TypedReaderTableSpecsProvider.Dependent, ClassSerializer {
    }

    // TODO (#4): If your T is not Class<?>, replace ClassSerializer with appropriate serializer (e.g., DataTypeSerializer)
    static final class TransformationElementSettingsProvider
        extends TransformationParametersStateProviders.TransformationElementSettingsProvider<Class<?>>
        implements ProductionPathProviderAndTypeHierarchy, TypedReaderTableSpecsProvider.Dependent, ClassSerializer {

        @Override
        protected boolean hasMultipleFileHandling() {
            return true;
        }
    }

    static final class TypeChoicesProvider extends TransformationParametersStateProviders.TypeChoicesProvider<Class<?>>
        implements ProductionPathProviderAndTypeHierarchy, TypedReaderTableSpecsProvider.Dependent {
    }

    static final class TransformationSettingsWidgetModification
        extends TransformationParametersStateProviders.TransformationSettingsWidgetModification<Class<?>> {

        @Override
        protected Class<TableSpecSettingsProvider> getSpecsValueProvider() {
            return TableSpecSettingsProvider.class;
        }

        @Override
        protected Class<TypeChoicesProvider> getTypeChoicesProvider() {
            return TypeChoicesProvider.class;
        }

        @Override
        protected Class<TransformationElementSettingsProvider> getTransformationSettingsValueProvider() {
            return TransformationElementSettingsProvider.class;
        }

    }

    private TutorialReaderTransformationParametersStateProviders() {
        // Not intended to be initialized
    }

}
