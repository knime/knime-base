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
 *   Feb 3, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import java.util.EnumMap;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;

/**
 * Adds version information and delegates to the {@link TableSpecConfigSerializer} for a particular version.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify external data types
 */
final class VersionedTableSpecConfigSerializer<T> implements TableSpecConfigSerializer<T> {

    private static final String CFG_VERSION = "version";

    private final EnumMap<TableSpecConfigSerializerVersion, TableSpecConfigSerializer<T>> m_versionSerializers;

    private final TableSpecConfigSerializerVersion m_currentVersion;

    private final TableSpecConfigSerializer<T> m_saver;

    VersionedTableSpecConfigSerializer(
        final EnumMap<TableSpecConfigSerializerVersion, TableSpecConfigSerializer<T>> serializers,
        final TableSpecConfigSerializerVersion currentVersion) {
        super();
        m_versionSerializers = serializers;
        m_currentVersion = currentVersion;
        m_saver = serializers.get(currentVersion);
    }

    @Override
    public void save(final TableSpecConfig<T> config, final NodeSettingsWO settings) {
        settings.addString(CFG_VERSION, m_currentVersion.name());
        m_saver.save(config, settings);
    }

    @Override
    public TableSpecConfig<T> load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final TableSpecConfigSerializer<T> serializer = getSerializerFor(settings);
        return serializer.load(settings);
    }

    @Override
    public TableSpecConfig<T> load(final NodeSettingsRO settings, final AdditionalParameters additionalParameters)
        throws InvalidSettingsException {
        final TableSpecConfigSerializer<T> serializer = getSerializerFor(settings);
        return serializer.load(settings, additionalParameters);
    }

    private TableSpecConfigSerializer<T> getSerializerFor(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final TableSpecConfigSerializerVersion version = loadVersion(settings);
        return getSerializer(version);
    }

    private static TableSpecConfigSerializerVersion loadVersion(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (settings.containsKey(CFG_VERSION)) {
            // the versioning was introduced in KAP 4.4.0
            return TableSpecConfigSerializerVersion.valueOf(settings.getString(CFG_VERSION));
        } else if (V43TableSpecConfigSerializer.isV43OrGreater(settings)) {
            // the enforce types option was introduced in KAP 4.3.0
            return TableSpecConfigSerializerVersion.V4_3;
        } else {
            // fallback onto the first version
            return TableSpecConfigSerializerVersion.V4_2;
        }
    }

    private TableSpecConfigSerializer<T> getSerializer(final TableSpecConfigSerializerVersion version)
        throws InvalidSettingsException {
        final TableSpecConfigSerializer<T> serializer = m_versionSerializers.get(version);
        CheckUtils.checkSetting(serializer != null,
            "The TableSpecConfigSerializer version %s is not supported by this node.", version);
        return serializer;
    }

}
