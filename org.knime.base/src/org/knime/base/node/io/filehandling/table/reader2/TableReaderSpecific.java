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
 *   Sep 20, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.table.reader2;

import org.knime.base.node.io.filehandling.table.reader.KnimeTableMultiTableReadConfig;
import org.knime.base.node.io.filehandling.table.reader.KnimeTableReader;
import org.knime.base.node.io.filehandling.webui.reader.ReaderSpecific;
import org.knime.base.node.preproc.manipulator.TableManipulatorConfig;
import org.knime.base.node.preproc.manipulator.mapping.DataTypeTypeHierarchy;
import org.knime.base.node.preproc.manipulator.mapping.DataValueReadAdapterFactory;
import org.knime.core.data.DataType;
import org.knime.filehandling.core.node.table.reader.DefaultProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class TableReaderSpecific {

    static final ProductionPathProvider<DataType> PRODUCTION_PATH_PROVIDER =
        new DefaultProductionPathProvider<>(DataValueReadAdapterFactory.INSTANCE.getProducerRegistry(),
            DataValueReadAdapterFactory.INSTANCE::getDefaultType);

    interface ProductionPathProviderAndTypeHierarchy
        extends ReaderSpecific.ProductionPathProviderAndTypeHierarchy<DataType> {
        @Override
        default ProductionPathProvider<DataType> getProductionPathProvider() {
            return PRODUCTION_PATH_PROVIDER;
        }

        @Override
        default TypeHierarchy<DataType, DataType> getTypeHierarchy() {
            return DataTypeTypeHierarchy.INSTANCE;
        }
    }

    interface ConfigAndReader extends ReaderSpecific.ConfigAndReader<TableManipulatorConfig, DataType> {
        @Override
        default KnimeTableMultiTableReadConfig getMultiTableReadConfig() {
            return new KnimeTableMultiTableReadConfig();
        }

        @Override
        default KnimeTableReader getTableReader() {
            return new KnimeTableReader();
        }
    }

    private TableReaderSpecific() {
        // Utility class
    }
}
