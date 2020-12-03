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
 *   04.11.2020 (Lars Schweikardt, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.linereader;

import java.nio.file.Path;

import org.knime.base.node.io.filehandling.csv.reader.api.StringReadAdapterFactory;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.node.table.reader.AbstractTableReaderNodeFactory;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.config.DefaultMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AbstractPathTableReaderNodeDialog;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TreeTypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeTester;

/**
 * Node factory for the line reader node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
public final class LineReaderNodeFactory2 extends AbstractTableReaderNodeFactory<LineReaderConfig2, Class<?>, String> {

    private static final TypeHierarchy<Class<?>, Class<?>> TYPE_HIERARCHY =
        TreeTypeHierarchy.builder(createTypeTester(String.class)).build();

    private static TypeTester<Class<?>, Class<?>> createTypeTester(final Class<?> type) {
        return TypeTester.createTypeTester(type, s -> true);
    }

    @Override
    protected PathSettings createPathSettings(final NodeCreationConfiguration nodeCreationConfig) {
        return new SettingsModelReaderFileChooser("file_selection",
            nodeCreationConfig.getPortConfig().orElseThrow(IllegalStateException::new), FS_CONNECT_GRP_ID,
            FilterMode.FILE);
    }

    @Override
    protected ReadAdapterFactory<Class<?>, String> getReadAdapterFactory() {
        return StringReadAdapterFactory.INSTANCE;
    }

    @Override
    protected TableReader<LineReaderConfig2, Class<?>, String> createReader() {
        return new LineReader2();
    }

    @Override
    protected String extractRowKey(final String value) {
        return value;
    }

    @Override
    protected TypeHierarchy<Class<?>, Class<?>> getTypeHierarchy() {
        return TYPE_HIERARCHY;
    }

    @Override
    protected AbstractPathTableReaderNodeDialog<LineReaderConfig2, Class<?>> createNodeDialogPane(
        final NodeCreationConfiguration creationConfig,
        final MultiTableReadFactory<Path, LineReaderConfig2, Class<?>> readFactory,
        final ProductionPathProvider<Class<?>> defaultProductionPathFn) {

        return new LineReaderNodeDialog2(createPathSettings(creationConfig), createConfig(creationConfig), readFactory,
            defaultProductionPathFn);
    }

    @Override
    protected DefaultMultiTableReadConfig<LineReaderConfig2, DefaultTableReadConfig<LineReaderConfig2>>
        createConfig(final NodeCreationConfiguration nodeCreationConfig) {
        final DefaultTableReadConfig<LineReaderConfig2> tc = new DefaultTableReadConfig<>(new LineReaderConfig2());
        return new DefaultMultiTableReadConfig<>(tc, LineReaderMultiTableReadConfigSerializer.INSTANCE);
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

}
