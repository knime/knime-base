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

import java.util.OptionalInt;
import java.util.OptionalLong;

import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;

/**
 * Contains utility methods for configuration classes.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class ReaderConfigUtils {

    private ReaderConfigUtils() {
        // utility class
    }

    static OptionalLong emptyIfNegative(final long value) {
        return value < 0 ? OptionalLong.empty() : OptionalLong.of(value);
    }

    static OptionalInt emptyIfNegative(final int value) {
        return value < 0 ? OptionalInt.empty() : OptionalInt.of(value);
    }

    static NodeSettingsRO getOrEmpty(final NodeSettingsRO settings, final String key) {
        try {
            return settings.getNodeSettings(key);
        } catch (InvalidSettingsException ex) {
            return new NodeSettings(key);
        }
    }

    /**
     * Creates a new {@link MultiTableReadConfig} that contains the provided {@link TableReadConfig}.
     *
     * @param tableReadConfig the config specifying how to read individual tables
     * @return a new MultiTableReadConfig
     */
    public static <C extends ReaderSpecificConfig<C>> MultiTableReadConfig<C> createMultiTableReadConfig(
        final TableReadConfig<C> tableReadConfig) {
        return new DefaultMultiTableReadConfig<>(tableReadConfig);
    }

    /**
     * Creates a new {@link MultiTableReadConfig} that contains a {@link TableReadConfig} holding the provided
     * {@link ReaderSpecificConfig}.
     *
     * @param readerSpecificConfig the config specific to the concrete reader implementation
     * @param producerRegistry used to deserialize production paths
     * @return a new MultiTableReadConfig
     */
    public static <C extends ReaderSpecificConfig<C>> MultiTableReadConfig<C>
        createMultiTableReadConfig(final C readerSpecificConfig, final ProducerRegistry<?, ?> producerRegistry) {
        return createMultiTableReadConfig(createTableReadConfig(readerSpecificConfig));
    }

    /**
     * Creates a new {@link TableReadConfig} that holds the provided {@link ReaderSpecificConfig}.
     *
     * @param readerSpecificConfig config specific to the concrete reader implementation
     * @return a new TableReadConfig
     */
    public static <C extends ReaderSpecificConfig<C>> TableReadConfig<C>
        createTableReadConfig(final C readerSpecificConfig) {
        return new DefaultTableReadConfig<>(readerSpecificConfig);
    }

}
