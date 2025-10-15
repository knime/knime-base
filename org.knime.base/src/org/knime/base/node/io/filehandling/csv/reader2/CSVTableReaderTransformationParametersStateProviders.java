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
package org.knime.base.node.io.filehandling.csv.reader2;

import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.csv.reader.CSVMultiTableReadConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.ColumnDelimiterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.CommentStartRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.CustomEncodingRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.CustomRowDelimiterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.DecimalSeparatorRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.FileEncodingRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.FirstRowContainsColumnNamesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.LimitMemoryPerColumnRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.MaxDataRowsScannedRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.MaximumNumberOfColumnsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.QuoteCharacterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.QuoteEscapeCharacterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.QuotedStringsOptionRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.ReplaceEmptyQuotedStringsByMissingValuesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.RowDelimiterOptionRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.SkipFirstLinesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.ThousandsSeparatorRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderSpecific.ConfigAndReader;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderSpecific.ProductionPathProviderAndTypeHierarchy;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.CSVReaderParametersRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.ReaderParametersRef;
import org.knime.base.node.io.filehandling.webui.reader2.ClassSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderParameters;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderParameters.FirstColumnContainsRowIdsRef;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderParameters.SkipFirstDataRowsRef;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProviders;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class CSVTableReaderTransformationParametersStateProviders {

    static final class TypedReaderTableSpecsProvider
        extends TransformationParametersStateProviders.TypedReaderTableSpecsProvider<//
                CSVTableReaderConfig, Class<?>, CSVMultiTableReadConfig>
        implements ConfigAndReader {

        private Supplier<ReaderParameters> m_commonReaderNodeParamsSupplier;

        private Supplier<CSVTableReaderParameters> m_readerSpecificNodeParamsSupplier;

        @Override
        protected void applyParametersToConfig(final CSVMultiTableReadConfig config) {
            m_commonReaderNodeParamsSupplier.get().saveToConfig(config);
            m_readerSpecificNodeParamsSupplier.get().saveToConfig(config);
        }

        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            m_commonReaderNodeParamsSupplier = initializer.getValueSupplier(ReaderParametersRef.class);
            m_readerSpecificNodeParamsSupplier = initializer.getValueSupplier(CSVReaderParametersRef.class);
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
                initializer.computeOnValueChange(FirstRowContainsColumnNamesRef.class);
                initializer.computeOnValueChange(FirstColumnContainsRowIdsRef.class);
                initializer.computeOnValueChange(CommentStartRef.class);
                initializer.computeOnValueChange(ColumnDelimiterRef.class);
                initializer.computeOnValueChange(QuoteCharacterRef.class);
                initializer.computeOnValueChange(QuoteEscapeCharacterRef.class);
                initializer.computeOnValueChange(RowDelimiterOptionRef.class);
                initializer.computeOnValueChange(CustomRowDelimiterRef.class);
                initializer.computeOnValueChange(QuotedStringsOptionRef.class);
                initializer.computeOnValueChange(ReplaceEmptyQuotedStringsByMissingValuesRef.class);
                initializer.computeOnValueChange(MaxDataRowsScannedRef.class);
                initializer.computeOnValueChange(ThousandsSeparatorRef.class);
                initializer.computeOnValueChange(DecimalSeparatorRef.class);
                initializer.computeOnValueChange(FileEncodingRef.class);
                initializer.computeOnValueChange(CustomEncodingRef.class);
                initializer.computeOnValueChange(SkipFirstLinesRef.class);
                initializer.computeOnValueChange(SkipFirstDataRowsRef.class);
                initializer.computeOnValueChange(LimitMemoryPerColumnRef.class);
                initializer.computeOnValueChange(MaximumNumberOfColumnsRef.class);
            }
        }
    }

    static final class TableSpecSettingsProvider
        extends TransformationParametersStateProviders.TableSpecSettingsProvider<Class<?>>
        implements TypedReaderTableSpecsProvider.Dependent, ClassSerializer {
    }

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

    private CSVTableReaderTransformationParametersStateProviders() {
        // Not intended to be initialized
    }

}
