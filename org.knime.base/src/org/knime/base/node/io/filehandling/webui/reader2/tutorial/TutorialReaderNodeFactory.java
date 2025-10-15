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

import static org.knime.node.impl.description.PortDescription.dynamicPort;
import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Optional;

import org.knime.base.node.io.filehandling.webui.reader2.FileSelectionPath;
import org.knime.base.node.io.filehandling.webui.reader2.NodeParametersConfigAndSourceSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.WebUITableReaderNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.util.Version;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.GenericTableReader;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;

/**
 * @author KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
// TODO (#4): Adjust Class<?> (T) and String (V) to match your TableReader's type parameters if needed
public class TutorialReaderNodeFactory extends
    WebUITableReaderNodeFactory<TutorialReaderNodeParameters, FSPath, FileSelectionPath, DummyTableReaderConfig, Class<?>, String, DummyMultiTableReadConfig> {

    @SuppressWarnings("javadoc")
    public TutorialReaderNodeFactory() {
        super(TutorialReaderNodeParameters.class);
    }

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        return super.createPortsConfigBuilder();
    }

    // TODO (#5): Complete the node description
    @Override
    protected NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription( //
            "TODO (#5): Name", //
            "TODO (#5): Icon", //
            List.of(dynamicPort(FS_CONNECT_GRP_ID, "File System Connection", "The file system connection.")), //
            List.of(fixedPort("TODO (#5): Output name", "TODO (#5): Output description")), //
            "TODO (#5): short description", //
            "TODO (#5): Full description", //
            List.of(), //
            TutorialReaderNodeParameters.class, //
            null, //
            NodeType.Source, //
            List.of("Input", "Read"), // TODO (#5): set additional keywords
            new Version(5, 9, 0) // TODO (#5): Set the version when the node is first introduced
        );
    }

    // TODO (#5): Return your ReadAdapterFactory instance
    @Override
    protected ReadAdapterFactory<Class<?>, String> getReadAdapterFactory() {
        return null;
    }

    // TODO (#3): Return new instance of your TableReader
    @Override
    protected GenericTableReader<FSPath, DummyTableReaderConfig, Class<?>, String> createReader() {
        return new DummyTableReader();
    }

    // TODO (#5): Implement if your V type requires special handling
    @Override
    protected String extractRowKey(final String value) {
        return value;
    }

    // TODO (#5): Return your ReadAdapterFactory's TYPE_HIERARCHY
    @Override
    protected TypeHierarchy<Class<?>, Class<?>> getTypeHierarchy() {
        return null;
    }

    @Override
    protected TutorialConfigAndSourceSerializer createSerializer() {
        return new TutorialConfigAndSourceSerializer();
    }

    private final class TutorialConfigAndSourceSerializer extends
        NodeParametersConfigAndSourceSerializer<TutorialReaderNodeParameters, FSPath, FileSelectionPath, DummyTableReaderConfig, Class<?>, DummyMultiTableReadConfig> {
        protected TutorialConfigAndSourceSerializer() {
            super(TutorialReaderNodeParameters.class);
        }

        @Override
        protected void saveToSourceAndConfig(final TutorialReaderNodeParameters params,
            final FileSelectionPath sourceSettings, final DummyMultiTableReadConfig config) {
            params.saveToSource(sourceSettings);
            params.saveToConfig(config);
        }
    }

    @Override
    protected FileSelectionPath createPathSettings(final NodeCreationConfiguration nodeCreationConfig) {
        final var source = new FileSelectionPath();
        final var defaultParams = new TutorialReaderNodeParameters(nodeCreationConfig);
        defaultParams.saveToSource(source);
        return source;
    }

    @Override
    protected DummyMultiTableReadConfig createConfig(final NodeCreationConfiguration nodeCreationConfig) {
        final var cfg = new DummyMultiTableReadConfig();
        final var defaultParams = new TutorialReaderNodeParameters(nodeCreationConfig);
        defaultParams.saveToConfig(cfg);
        return cfg;
    }
}
