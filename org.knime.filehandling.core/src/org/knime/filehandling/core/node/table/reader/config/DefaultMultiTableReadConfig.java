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
 *   Feb 3, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Default implementation of {@link MultiTableReadConfig}.</br>
 * Uses a {@link ConfigSerializer} for serialization which is provided by the user.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig} used by the node implementation
 * @param <TC> the type of {@link TableReadConfig} used by the node implementation
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class DefaultMultiTableReadConfig<C extends ReaderSpecificConfig<C>, TC extends TableReadConfig<C>>
    extends AbstractMultiTableReadConfig<C, TC> {

    private final ConfigSerializer<DefaultMultiTableReadConfig<C, TC>> m_serializer;

    /**
     * Constructor.
     *
     * @param tableReadConfig holding settings for reading a single table
     * @param serializer for loading/saving/validating the config
     */
    public DefaultMultiTableReadConfig(final TC tableReadConfig,
        final ConfigSerializer<DefaultMultiTableReadConfig<C, TC>> serializer) {
        super(tableReadConfig);
        m_serializer = serializer;
    }

    /**
     * Creates a {@link DefaultMultiTableReadConfig} with default serialization.</br>
     * Can be used for quick prototyping but it is highly recommended to implement a custom serializer that ensures the
     * structure of the saved settings matches the structure of the node dialog.
     *
     * @param <C> the type of {@link ReaderSpecificConfig} used by the node implementation
     * @param readerSpecificConfig {@link ReaderSpecificConfig} used by the node implementation
     * @param specificConfigSerializer {@link ConfigSerializer} for the {@link ReaderSpecificConfig}
     * @param producerRegistry for loading type mapping production paths
     * @return a {@link DefaultMultiTableReadConfig} with default serialization
     */
    public static <C extends ReaderSpecificConfig<C>> DefaultMultiTableReadConfig<C, DefaultTableReadConfig<C>> create(
        final C readerSpecificConfig, final ConfigSerializer<C> specificConfigSerializer,
        final ProducerRegistry<?, ?> producerRegistry) {
        final DefaultTableReadConfig<C> tc = new DefaultTableReadConfig<>(readerSpecificConfig);
        final DefaultTableReadConfigSerializer<C> tcSerializer =
            new DefaultTableReadConfigSerializer<>(specificConfigSerializer);
        final DefaultMultiTableReadConfigSerializer<C, DefaultTableReadConfig<C>> serializer =
            new DefaultMultiTableReadConfigSerializer<>(tcSerializer, producerRegistry);
        return new DefaultMultiTableReadConfig<>(tc, serializer);
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_serializer.loadInModel(this, settings);
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_serializer.loadInDialog(this, settings, specs);
    }

    @Override
    public void saveInModel(final NodeSettingsWO settings) {
        m_serializer.saveInModel(this, settings);
    }

    @Override
    public void saveInDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_serializer.saveInDialog(this, settings);
    }

    @Override
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_serializer.validate(settings);
    }


}
