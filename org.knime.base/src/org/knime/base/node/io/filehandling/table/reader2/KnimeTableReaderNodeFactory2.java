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

import static org.knime.node.impl.description.PortDescription.dynamicPort;
import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;

import org.knime.base.node.io.filehandling.table.reader.KnimeTableMultiTableReadConfig;
import org.knime.base.node.io.filehandling.table.reader.KnimeTableReader;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileSelectionPath;
import org.knime.base.node.io.filehandling.webui.reader2.NodeParametersConfigAndSourceSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.WebUITableReaderNodeFactory;
import org.knime.base.node.preproc.manipulator.TableManipulatorConfig;
import org.knime.base.node.preproc.manipulator.mapping.DataTypeTypeHierarchy;
import org.knime.base.node.preproc.manipulator.mapping.DataValueReadAdapterFactory;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
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
 * Node factory for the Table Reader node featuring a WebUI {@link NodeDialog}.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
public class KnimeTableReaderNodeFactory2 extends WebUITableReaderNodeFactory<KnimeTableReaderNodeParameters, //
        FSPath, MultiFileSelectionPath, TableManipulatorConfig, DataType, DataValue, KnimeTableMultiTableReadConfig> {

    @SuppressWarnings("javadoc")
    public KnimeTableReaderNodeFactory2() {
        super(KnimeTableReaderNodeParameters.class);
    }

    @Override
    protected NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription( //
            "Table Reader", //
            "../reader/tableread.png", //
            List.of(dynamicPort(FS_CONNECT_GRP_ID, "File System Connection", "The file system connection.")), //
            List.of(fixedPort("Read table", "The table contained in the selected file.")), //
            "Reads table written by the Table Writer node.", //
            """
                    This node reads files that have been written using the Table Writer node
                    (which uses an internal format). It retains all meta information
                    such as domain, properties, colors, size.
                    """, //
            List.of(), //
            KnimeTableReaderNodeParameters.class, //
            null, //
            NodeType.Source, //
            List.of("Table", "Read", "Input"), //
            new Version(5, 5, 0) //
        );
    }

    @Override
    protected ReadAdapterFactory<DataType, DataValue> getReadAdapterFactory() {
        return DataValueReadAdapterFactory.INSTANCE;
    }

    @Override
    protected GenericTableReader<FSPath, TableManipulatorConfig, DataType, DataValue> createReader() {
        return new KnimeTableReader();
    }

    @Override
    protected String extractRowKey(final DataValue value) {
        return value.toString();
    }

    @Override
    protected TypeHierarchy<DataType, DataType> getTypeHierarchy() {
        return DataTypeTypeHierarchy.INSTANCE;
    }

    @Override
    protected KnimeTableConfigAndSourceSerializer createSerializer() {
        return new KnimeTableConfigAndSourceSerializer();
    }

    private final class KnimeTableConfigAndSourceSerializer
        extends NodeParametersConfigAndSourceSerializer<KnimeTableReaderNodeParameters, FSPath, //
                MultiFileSelectionPath, TableManipulatorConfig, DataType, KnimeTableMultiTableReadConfig> {
        protected KnimeTableConfigAndSourceSerializer() {
            super(KnimeTableReaderNodeParameters.class);
        }

        @Override
        protected void saveToSourceAndConfig(final KnimeTableReaderNodeParameters params,
            final MultiFileSelectionPath sourceSettings, final KnimeTableMultiTableReadConfig config) {
            params.saveToSource(sourceSettings);
            params.saveToConfig(config);
        }
    }

    @Override
    protected MultiFileSelectionPath createPathSettings(final NodeCreationConfiguration nodeCreationConfig) {
        final var source = new MultiFileSelectionPath();
        final var defaultParams = new KnimeTableReaderNodeParameters(nodeCreationConfig);
        defaultParams.saveToSource(source);
        return source;
    }

    @Override
    protected KnimeTableMultiTableReadConfig createConfig(final NodeCreationConfiguration nodeCreationConfig) {
        final var cfg = new KnimeTableMultiTableReadConfig();
        final var defaultParams = new KnimeTableReaderNodeParameters(nodeCreationConfig);
        defaultParams.saveToConfig(cfg);
        return cfg;
    }
}
