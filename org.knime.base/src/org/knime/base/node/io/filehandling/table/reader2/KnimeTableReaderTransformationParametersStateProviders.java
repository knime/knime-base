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
 *   Nov 21, 2025: created
 */
package org.knime.base.node.io.filehandling.table.reader2;

import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.table.reader.KnimeTableMultiTableReadConfig;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeParameters.KnimeTableReaderParametersRef;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderSpecific.ConfigAndReader;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderSpecific.ProductionPathProviderAndTypeHierarchy;
import org.knime.base.node.io.filehandling.webui.reader2.DataTypeSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProviders;
import org.knime.base.node.preproc.manipulator.TableManipulatorConfig;
import org.knime.core.data.DataType;

/**
 * State providers for the Table Reader Node transformation settings.
 *
 * @author Paul Bärnreuther
 */
final class KnimeTableReaderTransformationParametersStateProviders {

    static final class TypedReaderTableSpecsProvider
        extends TransformationParametersStateProviders.TypedReaderTableSpecsProvider<//
                TableManipulatorConfig, DataType, KnimeTableMultiTableReadConfig>
        implements ConfigAndReader {

        private Supplier<KnimeTableReaderParameters> m_readerNodeParamsSupplier;

        @Override
        protected void applyParametersToConfig(final KnimeTableMultiTableReadConfig config) {
            m_readerNodeParamsSupplier.get().saveToConfig(config);
        }

        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            m_readerNodeParamsSupplier = initializer.getValueSupplier(KnimeTableReaderParametersRef.class);
        }

        interface Dependent
            extends TransformationParametersStateProviders.TypedReaderTableSpecsProvider.Dependent<DataType> {
            @SuppressWarnings("unchecked")
            @Override
            default Class<TypedReaderTableSpecsProvider> getTypedReaderTableSpecsProvider() {
                return TypedReaderTableSpecsProvider.class;
            }

            @Override
            default void initConfigIdTriggers(final StateProviderInitializer initializer) {
                // No triggers for Knime Table Reader. The spec does not depend on any parameter.
            }
        }
    }

    static final class TableSpecSettingsProvider
        extends TransformationParametersStateProviders.TableSpecSettingsProvider<DataType>
        implements TypedReaderTableSpecsProvider.Dependent, DataTypeSerializer {
    }

    static final class TransformationElementSettingsProvider
        extends TransformationParametersStateProviders.TransformationElementSettingsProvider<DataType>
        implements ProductionPathProviderAndTypeHierarchy, TypedReaderTableSpecsProvider.Dependent, DataTypeSerializer {

        @Override
        protected boolean hasMultipleFileHandling() {
            return true;
        }
    }

    static final class TypeChoicesProvider extends TransformationParametersStateProviders.TypeChoicesProvider<DataType>
        implements ProductionPathProviderAndTypeHierarchy, TypedReaderTableSpecsProvider.Dependent {
    }

    static final class TransformationSettingsWidgetModification
        extends TransformationParametersStateProviders.TransformationSettingsWidgetModification<DataType> {

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

    private KnimeTableReaderTransformationParametersStateProviders() {
        // Not intended to be initialized
    }

}
