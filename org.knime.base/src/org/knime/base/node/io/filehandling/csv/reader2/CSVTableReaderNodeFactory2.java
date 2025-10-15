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

import static org.knime.node.impl.description.PortDescription.dynamicPort;
import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knime.base.node.io.filehandling.csv.reader.CSVMultiTableReadConfig;
import org.knime.base.node.io.filehandling.csv.reader.CSVTableReaderNodeFactory;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReader;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.StringReadAdapterFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.core.util.Version;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.filehandling.core.connections.FSLocationUtil;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.node.table.reader.CommonTableReaderNodeFactory;
import org.knime.filehandling.core.node.table.reader.GenericTableReader;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;

/**
 * Node factory for the CSV reader node that operates similar to the {@link CSVTableReaderNodeFactory} but features a
 * modern / Web UI {@link NodeDialog}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public class CSVTableReaderNodeFactory2 extends
    CommonTableReaderNodeFactory<FSPath, TableReaderPath, CSVTableReaderConfig, Class<?>, CSVMultiTableReadConfig, String>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private static final String FULL_DESCRIPTION = """
            Use this node to read CSV files into your workflow. The node will produce a data table with numbers and
            types of columns guessed automatically.
            """;

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() { // NOSONAR only to make this visible to testing
        return super.createPortsConfigBuilder();
    }

    @Override
    protected NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription( //
            "CSV Reader (Labs)", //
            "csvreader.png", //
            List.of(dynamicPort(FS_CONNECT_GRP_ID, "Input column", "Table containing time series.")), //
            List.of(fixedPort("File Table",
                "Data table based on the file being read with number and types of columns guessed automatically.")), //
            "Reads CSV files", //
            FULL_DESCRIPTION, //
            List.of(), //
            CSVTableReaderNodeSettings.class, //
            null, //
            NodeType.Source, //
            List.of("Text", "Comma", "File", "Input", "Read"), //
            new Version(5, 9, 0) //
        );
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, CSVTableReaderNodeParameters.class);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, CSVTableReaderNodeParameters.class));
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    private static final String[] FILE_SUFFIXES = new String[]{".csv", ".tsv", ".txt", ".gz"};

    @Override
    protected TableReaderPath createPathSettings(final NodeCreationConfiguration nodeCreationConfig) {
        // mostly copied from CSVTableReaderNodeFactory
        // TODO bind "file_selection" config key to params; also de-duplicate
        final SettingsModelReaderFileChooser settingsModel = new SettingsModelReaderFileChooser("file_selection",
            nodeCreationConfig.getPortConfig().orElseThrow(IllegalStateException::new), FS_CONNECT_GRP_ID,
            EnumConfig.create(FilterMode.FILE, FilterMode.FILES_IN_FOLDERS), FILE_SUFFIXES);
        final Optional<? extends URLConfiguration> urlConfig = nodeCreationConfig.getURLConfig();
        if (urlConfig.isPresent()) {
            settingsModel.setLocation(FSLocationUtil.createFromURL(urlConfig.get().getUrl().toString()));
        }
        //        return settingsModel;
        return new TableReaderPath();
        // TODO how does the file selected in the params reach the model?
    }

    //    @Override
    //    protected StorableMultiTableReadConfig<CSVTableReaderConfig, Class<?>>
    //        createConfig(final NodeCreationConfiguration nodeCreationConfig) {

    //    }

    @Override
    protected ReadAdapterFactory<Class<?>, String> getReadAdapterFactory() {
        // copied from AbstractCSVTableReaderNodeFactory
        return StringReadAdapterFactory.INSTANCE;
    }

    @Override
    protected GenericTableReader<FSPath, CSVTableReaderConfig, Class<?>, String> createReader() {
        // copied from AbstractCSVTableReaderNodeFactory
        return new CSVTableReader();
    }

    @Override
    protected String extractRowKey(final String value) {
        // copied from AbstractCSVTableReaderNodeFactory
        return value;
    }

    @Override
    protected TypeHierarchy<Class<?>, Class<?>> getTypeHierarchy() {
        // copied from AbstractCSVTableReaderNodeFactory
        return StringReadAdapterFactory.TYPE_HIERARCHY;
    }

    @Override
    protected
        ConfigAndSourceSerializer<FSPath, TableReaderPath, CSVTableReaderConfig, Class<?>, CSVMultiTableReadConfig>
        createSerializer() {
        return new ConfigAndSourceSerializer<FSPath, TableReaderPath, CSVTableReaderConfig, Class<?>, CSVMultiTableReadConfig>() {

            @Override
            public void validateSettings(final TableReaderPath sourceSettings, final CSVMultiTableReadConfig config,
                final NodeSettingsRO settings) throws InvalidSettingsException {
                final var params = NodeParametersUtil.loadSettings(settings, CSVTableReaderNodeParameters.class);
                params.validate();
                // TODO move validation from CSVMultiTableReadConfigSerializer.validate and AbstractSettingsModelFileChooser.validateSettingsForModel to params
            }

            @Override
            public void saveSettingsTo(final TableReaderPath sourceSettings, final CSVMultiTableReadConfig config,
                final NodeSettingsWO settings) {
                final var params = new CSVTableReaderNodeParameters();
                params.loadFromTableReaderPathSettings(sourceSettings);
                params.loadFromConfig(config);
                NodeParametersUtil.saveSettings(CSVTableReaderNodeParameters.class, params, settings);
            }

            @Override
            public void loadValidatedSettingsFrom(final TableReaderPath sourceSettings,
                final CSVMultiTableReadConfig config, final NodeSettingsRO settings) throws InvalidSettingsException {
                final var params = NodeParametersUtil.loadSettings(settings, CSVTableReaderNodeParameters.class);
                params.saveToTableReaderPathSettings(sourceSettings);
                params.saveToConfig(config);
            }
        };
    }

    @Override
    protected CSVMultiTableReadConfig createConfig(final NodeCreationConfiguration nodeCreationConfig) {
        final var cfg = new CSVMultiTableReadConfig();
        final var defaultParams = new CSVTableReaderNodeParameters();
        defaultParams.saveToConfig(cfg);
        // below mostly copied from CSVTableReaderNodeFactory
        final Optional<? extends URLConfiguration> urlConfig = nodeCreationConfig.getURLConfig();
        if (urlConfig.isPresent() && urlConfig.get().getUrl().toString().endsWith(".tsv")) { //NOSONAR
            cfg.getTableReadConfig().getReaderSpecificConfig().setDelimiter("\t");
        }
        return cfg;
    }
}
