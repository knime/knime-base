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
 *   May 15, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.table.reader2;

import java.util.List;

import org.knime.base.node.io.filehandling.table.reader2.TableReaderSpecific.ConfigAndReader;
import org.knime.base.node.io.filehandling.table.reader2.TableReaderSpecific.ProductionPathProviderAndTypeHierarchy;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.ConfigIdSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.ReaderSpecificDependencies;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.ReaderSpecificDependenciesProvider;
import org.knime.base.node.io.filehandling.webui.reader.DataTypeStringSerializer;
import org.knime.base.node.preproc.manipulator.TableManipulatorConfig;
import org.knime.core.data.DataType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class TableReaderTransformationSettingsStateProviders {

    @SuppressWarnings("unused")
    static final class NoDependencies
        implements ReaderSpecificDependenciesProvider<ReaderSpecificDependencies<TableManipulatorConfig>> {

        @Override
        public ReaderSpecificDependencies<TableManipulatorConfig>
            computeState(final DefaultNodeSettingsContext context) {
            return new ReaderSpecificDependencies<>() {
            };
        }

        interface GetReferences extends ReaderSpecificDependenciesProvider.GetReferences {

            @Override
            default List<Class<? extends Reference<?>>> getDependencyReferences() {
                return List.of();
            }

        }

        interface Dependent
            extends ReaderSpecificDependenciesProvider.Dependent<ReaderSpecificDependencies<TableManipulatorConfig>> {

            @Override
            default Class<NoDependencies> getDependenciesProvider() {
                return NoDependencies.class;
            }
        }
    }

    static final class TypedReaderTableSpecsProvider
        extends CommonReaderTransformationSettingsStateProviders.TypedReaderTableSpecsProvider<//
                TableManipulatorConfig, DataType, ReaderSpecificDependencies<TableManipulatorConfig>>
        implements NoDependencies.Dependent, ConfigAndReader {

        interface Dependent
            extends CommonReaderTransformationSettingsStateProviders.TypedReaderTableSpecsProvider.Dependent<DataType> {
            @Override
            default Class<? extends CommonReaderTransformationSettingsStateProviders.TypedReaderTableSpecsProvider<//
                    ?, DataType, ?>> getTypedReaderTableSpecsProvider() {
                return TypedReaderTableSpecsProvider.class;
            }
        }
    }

    static final class TableSpecSettingsProvider
        extends CommonReaderTransformationSettingsStateProviders.TableSpecSettingsProvider<String, DataType>
        implements TypedReaderTableSpecsProvider.Dependent, DataTypeStringSerializer, NoDependencies.GetReferences {
    }

    static final class TransformationElementSettingsProvider
        extends CommonReaderTransformationSettingsStateProviders.TransformationElementSettingsProvider<String, DataType>
        implements ProductionPathProviderAndTypeHierarchy, DataTypeStringSerializer,
        TypedReaderTableSpecsProvider.Dependent, NoDependencies.GetReferences {
        @Override
        protected TypeReference<List<CommonReaderTransformationSettings.TableSpecSettings<String>>>
            getTableSpecSettingsTypeReference() {
            return new TypeReference<>() {
            };
        }
    }

    static final class TypeChoicesProvider
        extends CommonReaderTransformationSettingsStateProviders.TypeChoicesProvider<DataType>
        implements ProductionPathProviderAndTypeHierarchy, TypedReaderTableSpecsProvider.Dependent {
    }

    static final class TransformationSettingsWidgetModification
        extends CommonReaderTransformationSettingsStateProviders.TransformationSettingsWidgetModification<//
                ConfigIdSettings<TableManipulatorConfig>, String, DataType> {

        static final class KnimeTableReaderConfigIdSettingsValueRef
            implements Reference<ConfigIdSettings<TableManipulatorConfig>> {
        }

        @Override
        protected Class<? extends Reference<ConfigIdSettings<TableManipulatorConfig>>> getConfigIdSettingsValueRef() {
            return KnimeTableReaderConfigIdSettingsValueRef.class;
        }

        @Override
        protected Class<? extends CommonReaderTransformationSettingsStateProviders.TableSpecSettingsProvider<//
                String, DataType>> getSpecsValueProvider() {
            return TableSpecSettingsProvider.class;
        }

        @Override
        protected Class<? extends CommonReaderTransformationSettingsStateProviders.TypeChoicesProvider<DataType>>
            getTypeChoicesProvider() {
            return TypeChoicesProvider.class;
        }

        @Override
        protected Class<TransformationElementSettingsProvider> getTransformationSettingsValueProvider() {
            return TransformationElementSettingsProvider.class;
        }

    }

    private TableReaderTransformationSettingsStateProviders() {
        // Not intended to be initialized
    }
}
