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
import java.util.Optional;

import org.knime.base.node.io.filehandling.csv.reader.CSVMultiTableReadConfig;
import org.knime.base.node.io.filehandling.csv.reader.CSVTableReaderNodeFactory;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReader;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.StringReadAdapterFactory;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileSelectionPath;
import org.knime.base.node.io.filehandling.webui.reader2.NodeParametersConfigAndSourceSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.WebUITableReaderNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.util.Version;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.filehandling.core.connections.FSPath;
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
public class CSVTableReaderNodeFactory2 extends WebUITableReaderNodeFactory<CSVTableReaderNodeParameters, FSPath, //
        MultiFileSelectionPath, CSVTableReaderConfig, Class<?>, String, CSVMultiTableReadConfig> {

    @SuppressWarnings("javadoc")
    public CSVTableReaderNodeFactory2() {
        super(CSVTableReaderNodeParameters.class);
    }

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
            "CSV Reader", //
            "csvreader.png", //
            List.of(dynamicPort(FS_CONNECT_GRP_ID, "File System Connection", "The file system connection.")), //
            List.of(fixedPort("File Table",
                "Data table based on the file being read with number and types of columns guessed automatically.")), //
            "Reads CSV files", //
            FULL_DESCRIPTION, //
            List.of(), //
            CSVTableReaderNodeParameters.class, //
            null, //
            NodeType.Source, //
            List.of("Text", "Comma", "File", "Input", "Read"), //
            new Version(5, 9, 0) //
        );
    }

    @Override
    protected ReadAdapterFactory<Class<?>, String> getReadAdapterFactory() {
        return StringReadAdapterFactory.INSTANCE;
    }

    @Override
    protected GenericTableReader<FSPath, CSVTableReaderConfig, Class<?>, String> createReader() {
        return new CSVTableReader();
    }

    @Override
    protected String extractRowKey(final String value) {
        return value;
    }

    @Override
    protected TypeHierarchy<Class<?>, Class<?>> getTypeHierarchy() {
        return StringReadAdapterFactory.TYPE_HIERARCHY;
    }

    @Override
    protected CSVConfigAndSourceSerializer createSerializer() {
        return new CSVConfigAndSourceSerializer();
    }

    private final class CSVConfigAndSourceSerializer
        extends NodeParametersConfigAndSourceSerializer<CSVTableReaderNodeParameters, FSPath, MultiFileSelectionPath, //
                CSVTableReaderConfig, Class<?>, CSVMultiTableReadConfig> {
        protected CSVConfigAndSourceSerializer() {
            super(CSVTableReaderNodeParameters.class);
        }

        @Override
        protected void saveToSourceAndConfig(final CSVTableReaderNodeParameters params,
            final MultiFileSelectionPath sourceSettings, final CSVMultiTableReadConfig config) {
            params.saveToSource(sourceSettings);
            params.saveToConfig(config);
        }
    }

    @Override
    protected MultiFileSelectionPath createPathSettings(final NodeCreationConfiguration nodeCreationConfig) {
        final var source = new MultiFileSelectionPath();
        final var defaultParams = new CSVTableReaderNodeParameters(nodeCreationConfig);
        defaultParams.saveToSource(source);
        return source;
    }

    @Override
    protected CSVMultiTableReadConfig createConfig(final NodeCreationConfiguration nodeCreationConfig) {
        final var cfg = new CSVMultiTableReadConfig();
        final var defaultParams = new CSVTableReaderNodeParameters(nodeCreationConfig);
        defaultParams.saveToConfig(cfg);
        return cfg;
    }
}
