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
 *   Dec 11, 2023 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.knime.base.node.io.filehandling.csv.reader.CSVMultiTableReadConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReader;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.StringReadAdapterFactory;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.filehandling.core.connections.FSLocationUtil;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.node.table.reader.DefaultMultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.DefaultProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.DefaultSourceGroup;
import org.knime.filehandling.core.node.table.reader.MultiTableReader;
import org.knime.filehandling.core.node.table.reader.SourceGroup;
import org.knime.filehandling.core.node.table.reader.TableReaderNodeModel;
import org.knime.filehandling.core.node.table.reader.preview.dialog.GenericItemAccessor;
import org.knime.filehandling.core.node.table.reader.rowkey.DefaultRowKeyGeneratorContextFactory;

/**
 * This will probably have to extend {@link TableReaderNodeModel}
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class CSVTableReaderNodeModel2 extends NodeModel {

    private static final String[] FILE_SUFFIXES = new String[]{".csv", ".tsv", ".txt", ".gz"};

    private CSVTableReaderNodeSettings m_settings;

    private final NodeCreationConfiguration m_creationConfig;

    private final NodeModelStatusConsumer m_statusConsumer =
        new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));

    CSVTableReaderNodeModel2(final NodeCreationConfiguration creationConfig) {
        super(creationConfig.getPortConfig().get().getInputPorts(),
            creationConfig.getPortConfig().get().getOutputPorts());
        m_creationConfig = creationConfig;
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_settings == null) {
            m_settings = DefaultNodeSettings.createSettings(CSVTableReaderNodeSettings.class, inSpecs);
        }
        return null;
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        var config = new CSVMultiTableReadConfig();
        final Optional<? extends URLConfiguration> urlConfig = m_creationConfig.getURLConfig();
        if (urlConfig.isPresent() && urlConfig.get().getUrl().toString().endsWith(".tsv")) { //NOSONAR
            config.getReaderSpecificConfig().setDelimiter("\t");
        }

        final var pathSettings = new SettingsModelReaderFileChooser("file_selection",
            m_creationConfig.getPortConfig().orElseThrow(IllegalStateException::new),
            CSVTableReaderNodeFactory2.FS_CONNECT_GRP_ID,
            EnumConfig.create(FilterMode.FILE, FilterMode.FILES_IN_FOLDERS), FILE_SUFFIXES);
        if (urlConfig.isPresent()) {
            pathSettings.setLocation(FSLocationUtil.createFromURL(urlConfig.get().getUrl().toString()));
        }

        pathSettings.setLocation(m_settings.m_csvFile.getFSLocation());

        final var reader = new CSVTableReader();

        final var readAdapterFactory = StringReadAdapterFactory.INSTANCE;

        final var productionPathProvider = new DefaultProductionPathProvider<>(readAdapterFactory.getProducerRegistry(),
            readAdapterFactory::getDefaultType);

        final var rowKeyGenFactory =
            new DefaultRowKeyGeneratorContextFactory<FSPath, String>(this::extractRowKey, "File");

        final var multiTableReadFactory =
            new DefaultMultiTableReadFactory<FSPath, CSVTableReaderConfig, Class<?>, String>(
                StringReadAdapterFactory.TYPE_HIERARCHY, rowKeyGenFactory, reader, productionPathProvider,
                readAdapterFactory::createReadAdapter);

        final var multiTableReader = new MultiTableReader<>(multiTableReadFactory);

        try (final GenericItemAccessor<FSPath> accessor = pathSettings.createItemAccessor()) {
            final List<FSPath> paths = getPaths(accessor);
            final SourceGroup<FSPath> sourceGroup = new DefaultSourceGroup<>(pathSettings.getSourceIdentifier(), paths);
            return new BufferedDataTable[]{multiTableReader.readTable(sourceGroup, config, exec)};
        }
    }

    private String extractRowKey(final String value) {
        return value;
    }

    private List<FSPath> getPaths(final GenericItemAccessor<FSPath> accessor)
        throws IOException, InvalidSettingsException {
        final List<FSPath> items = accessor.getItems(m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);

        if (items.isEmpty()) {
            throw new InvalidSettingsException("No files/folders matched the filters");
        }
        return items;
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_settings != null) {
            DefaultNodeSettings.saveSettings(CSVTableReaderNodeSettings.class, m_settings, settings);
        }
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings = DefaultNodeSettings.loadSettings(settings, CSVTableReaderNodeSettings.class);
    }

    @Override
    protected void reset() {
    }

}
