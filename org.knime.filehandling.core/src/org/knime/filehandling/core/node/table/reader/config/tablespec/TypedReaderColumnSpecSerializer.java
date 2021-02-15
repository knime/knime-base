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
 *   Feb 2, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Serializer for {@link TypedReaderColumnSpec TypedReaderColumnSpecs}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TypedReaderColumnSpecSerializer<T> {

    private static final String CFG_TYPE = "type";

    private static final String CFG_HAS_TYPE = "has_type";

    private static final String CFG_NAME = "name";

    private final NodeSettingsSerializer<T> m_typeSerializer;

    TypedReaderColumnSpecSerializer(final NodeSettingsSerializer<T> typeSerializer) {
        m_typeSerializer = typeSerializer;
    }

    void save(final TypedReaderColumnSpec<T> columnSpec, final NodeSettingsWO settings) {
        settings.addString(CFG_NAME, MultiTableUtils.getNameAfterInit(columnSpec));
        settings.addBoolean(CFG_HAS_TYPE, columnSpec.hasType());
        m_typeSerializer.save(columnSpec.getType(), settings.addNodeSettings(CFG_TYPE));
    }

    TypedReaderColumnSpec<T> load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final String name = settings.getString(CFG_NAME);
        final boolean hasType = settings.getBoolean(CFG_HAS_TYPE);
        final T type = m_typeSerializer.load(settings.getNodeSettings(CFG_TYPE));
        return TypedReaderColumnSpec.createWithName(name, type, hasType);
    }
}
