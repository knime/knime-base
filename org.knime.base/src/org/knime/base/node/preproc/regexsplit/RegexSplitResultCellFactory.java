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
 *   15 Nov 2023 (jasper): created
 */
package org.knime.base.node.preproc.regexsplit;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.message.Message;

/**
 * Provides the output cells for the String Splitter (Regex) (formerly known as Regex Split)
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
final class RegexSplitResultCellFactory extends AbstractCellFactory {

    private final RegexSplitNodeSettings m_settings;

    private final RegexSplitter m_splitter;

    private final int m_inputColumnIndex;

    private final Consumer<Message> m_warningConsumer;

    RegexSplitResultCellFactory(final DataColumnSpec[] newColumns, final int inputColumnIndex,
        final RegexSplitNodeSettings settings, final RegexSplitter splitter, final Consumer<Message> warningConsumer)
        throws IllegalArgumentException {
        super(newColumns);
        m_settings = settings;
        m_splitter = splitter;
        m_inputColumnIndex = inputColumnIndex;
        m_warningConsumer = warningConsumer;
    }

    @Override
    public DataCell[] getCells(final DataRow row, final long rowIndex) {
        final var cell = row.getCell(m_inputColumnIndex);
        if (cell.isMissing()) {
            return createReplacementCells(rowIndex);
        } else if (cell instanceof StringValue sv) {
            final var split = m_splitter.apply(sv.getStringValue());
            return split.map(this::createOutputCells).orElseGet(() -> createReplacementCells(rowIndex));
        }
        throw KNIMEException.of(Message.fromRowIssue("Found a non-StringCell, aborting!", 0, rowIndex,
            m_inputColumnIndex, "This is most likely an implementation error.")).toUnchecked();
    }

    private DataCell[] createOutputCells(final List<Optional<String>> groups) {
        final Stream<DataCell> values = groups.stream()
            .map(optionalResult -> optionalResult.<DataCell> map(StringCell::new).orElseGet(DataType::getMissingCell));
        return switch (m_settings.m_output.m_mode) {
            case COLUMNS, ROWS -> values.toArray(DataCell[]::new);
            case LIST -> new DataCell[]{CollectionCellFactory.createListCell(values.toList())};
            case SET -> new DataCell[]{CollectionCellFactory.createSetCell(values.toList())};
        };
    }

    private static final Message LINE_DIDNT_MATCH_WARNING =
        Message.fromSummary("Some input string(s) were missing or did not match the pattern.");

    private DataCell[] createReplacementCells(final long rowIndex) {
        m_warningConsumer.accept(LINE_DIDNT_MATCH_WARNING);
        final var replacement = switch (m_settings.m_noMatchBehaviour) {
            case INSERT_EMPTY -> new StringCell("");
            case INSERT_MISSING -> DataType.getMissingCell();
            default -> throw KNIMEException
                .of(Message.fromRowIssue("Input string did not match the pattern. Aborting execution.", //
                    0, rowIndex, m_inputColumnIndex, //
                    "The node is configured to fail if an input string does not match the pattern."))
                .toUnchecked();
        };

        final var cells = new DataCell[this.getColumnSpecs().length];
        Arrays.fill(cells, replacement);
        return switch (m_settings.m_output.m_mode) {
            case ROWS, COLUMNS -> cells;
            case LIST -> new DataCell[]{CollectionCellFactory.createListCell(Arrays.stream(cells).toList())};
            case SET -> new DataCell[]{CollectionCellFactory.createSetCell(Arrays.stream(cells).toList())};
        };
    }

}
