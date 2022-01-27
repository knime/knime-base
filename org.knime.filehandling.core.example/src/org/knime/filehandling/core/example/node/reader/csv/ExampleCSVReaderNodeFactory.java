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
 *   28 Jun 2021 (Moditha Hewasinghaget): created
 */
package org.knime.filehandling.core.example.node.reader.csv;

import org.knime.core.data.DataType;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.node.table.reader.AbstractTableReaderNodeFactory;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AbstractPathTableReaderNodeDialog;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;

/**
 * This is an example implementation of the node factory of the
 * "ExampleCSVReader" node.
 *
 * We are using the {@link Class} to identify the external data types and using
 * {@link String} as the value from the reader
 */
public class ExampleCSVReaderNodeFactory
        extends AbstractTableReaderNodeFactory<ExampleCSVReaderConfig, DataType, String> {
    // File extensions for the file browser
    private static final String[] FILE_SUFFIXES = new String[] { ".csv" };

    @Override
    protected SettingsModelReaderFileChooser createPathSettings(final NodeCreationConfiguration nodeCreationConfig) {
        return new SettingsModelReaderFileChooser("file_selection",
                nodeCreationConfig.getPortConfig().orElseThrow(IllegalStateException::new), FS_CONNECT_GRP_ID,
                EnumConfig.create(FilterMode.FILE, FilterMode.FILES_IN_FOLDERS), FILE_SUFFIXES);
    }

    @Override
    protected ReadAdapterFactory<DataType, String> getReadAdapterFactory() {
        return ExampleCSVReadAdapterFactory.INSTANCE;
    }

    @Override
    protected TableReader<ExampleCSVReaderConfig, DataType, String> createReader() {
        return new ExampleCSVReader();
    }

    @Override
    protected String extractRowKey(final String value) {
        return value;
    }

    @Override
    protected TypeHierarchy<DataType, DataType> getTypeHierarchy() {
        return ExampleCSVReadAdapterFactory.TYPE_HIERARCHY;
    }

    @Override
    protected AbstractPathTableReaderNodeDialog<ExampleCSVReaderConfig, DataType> createNodeDialogPane(
            final NodeCreationConfiguration creationConfig,
            final MultiTableReadFactory<FSPath, ExampleCSVReaderConfig, DataType> readFactory,
            final ProductionPathProvider<DataType> defaultProductionPathFn) {

        return new ExampleCSVReaderNodeDialog(createPathSettings(creationConfig), createConfig(creationConfig),
                readFactory, defaultProductionPathFn);
    }

    @Override
    protected ExampleCSVMultiTableReadConfig createConfig(final NodeCreationConfiguration nodeCreationConfig) {
        return new ExampleCSVMultiTableReadConfig();
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }
}
