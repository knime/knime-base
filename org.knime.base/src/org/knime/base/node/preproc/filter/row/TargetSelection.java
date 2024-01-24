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
 *   24 Jan 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
final class TargetSelection implements PersistableSettings {

    enum Type {
        ROWID("<row-key>"),
        ROWINDEX("<row-index>"),
        ROWNUMBER("<row-number>"),
        COLUMN("<column>");

        private static final Map<String, Type> ID_MAP = Arrays.stream(Type.values())
                .collect(Collectors.toMap(t -> t.m_typeId, Function.identity()));

        private final String m_typeId;

        Type(final String typeId) {
            m_typeId = typeId;
        }

        static Type fromId(final String value) {
            return ID_MAP.get(value);
        }
    }

    final Type m_type;
    final String m_column;

    public TargetSelection() {
        this(Type.ROWID, null);
    }

    // from selected choice
    @JsonCreator
    public static TargetSelection valueOf(final String value) {
        if (value.startsWith(Type.COLUMN.m_typeId)) {
            return new TargetSelection(StringUtils.removeStart(value, Type.COLUMN.m_typeId));
        }
        return new TargetSelection(Type.fromId(value), null);
    }

    public TargetSelection(final String column) {
        this(Type.COLUMN, column);
    }

    public TargetSelection(final Type type, final String column) {
        m_type = type;
        m_column = column;
        if (type == Type.COLUMN) {
            CheckUtils.checkArgument(column != null, "Column missing");
        } else {
            checkColumnMissing(column);
        }
    }

    private static void checkColumnMissing(final String column) {
        if (column != null) {
            throw new IllegalArgumentException("Erroneously supplied column name: " + column);
        }
    }

    public static TargetSelection createDefault(final DefaultNodeSettings.DefaultNodeSettingsContext context) {
        return new TargetSelection(Type.ROWID, null);
    }

    static final class Choices implements ChoicesProvider {

        @Override
        public IdAndText[] choicesWithIdAndText(final DefaultNodeSettingsContext context) {
            final var special = List.of(
                new IdAndText(Type.ROWID.m_typeId, "RowID"),
                new IdAndText(Type.ROWINDEX.m_typeId, "Row Index"),
                new IdAndText(Type.ROWNUMBER.m_typeId, "Row Number")
            );
            final var specs = context.getDataTableSpec(0).map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .map(dcs -> new IdAndText(Type.COLUMN.m_typeId + dcs.getName(), dcs.getName()));
            return Stream.concat(special.stream(), specs).toArray(IdAndText[]::new);
        }
    }
}
