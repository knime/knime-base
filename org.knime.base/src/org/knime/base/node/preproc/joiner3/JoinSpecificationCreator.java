/*
 * ------------------------------------------------------------------------
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
 *   27.07.2007 (thor): created
 */
package org.knime.base.node.preproc.joiner3;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.knime.base.node.preproc.joiner3.Joiner3NodeSettings.CompositionMode;
import org.knime.base.node.preproc.joiner3.Joiner3NodeSettings.DuplicateHandling;
import org.knime.base.node.preproc.joiner3.Joiner3NodeSettings.RowKeyFactory;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.DataCellComparisonMode;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.JoinTableSettings;
import org.knime.core.data.join.JoinTableSettings.JoinColumn;
import org.knime.core.data.join.JoinTableSettings.SpecialJoinColumn;
import org.knime.core.data.join.KeepRowKeysFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;

/**
 * This class serves as an adapter of the {@link Joiner3NodeSettings} to be used in the {@link Joiner3NodeModel}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Paul Bärnreuther
 * @since 4.2
 */
final class JoinSpecificationCreator {

    Joiner3NodeSettings m_joinerNodeSettings;

    JoinSpecificationCreator(final Joiner3NodeSettings joinerNodeSettings) {
        m_joinerNodeSettings = joinerNodeSettings;
    }

    void validateSettings() throws InvalidSettingsException {
        if (getLeftJoinColumns().length == 0) {
            throw new InvalidSettingsException("Please define at least one joining column pair.");
        }

        if (m_joinerNodeSettings.m_duplicateHandling == DuplicateHandling.APPEND_SUFFIX
            && m_joinerNodeSettings.m_suffix.trim().isEmpty()) {
            throw new InvalidSettingsException("No suffix for duplicate columns provided");
        }
    }

    /**
     * Create the join specification for the given table specs.
     *
     * @throws InvalidSettingsException
     */
    JoinSpecification joinSpecificationForSpecs(final PortObjectSpec... portSpecs) throws InvalidSettingsException {

        // left (top port) input table
        DataTableSpec left = (DataTableSpec)portSpecs[0];
        String[] leftIncludes =
            m_joinerNodeSettings.m_leftColumnSelectionConfigV2.getSelected(left.getColumnNames(), left);
        var leftSettings =
            new JoinTableSettings(isIncludeLeftUnmatched(), getLeftJoinColumns(), leftIncludes, InputTable.LEFT, left);

        // right (bottom port) input table
        DataTableSpec right = (DataTableSpec)portSpecs[1];
        String[] rightIncludes =
            m_joinerNodeSettings.m_rightColumnSelectionConfigV2.getSelected(right.getColumnNames(),
                right);
        var rightSettings = new JoinTableSettings(isIncludeRightUnmatched(), getRightJoinColumns(), rightIncludes,
            InputTable.RIGHT, right);

        UnaryOperator<String> columnNameDisambiguator;
        // replace with custom
        if (m_joinerNodeSettings.m_duplicateHandling == DuplicateHandling.APPEND_SUFFIX) {
            columnNameDisambiguator = s -> s.concat(m_joinerNodeSettings.m_suffix);
        } else {
            columnNameDisambiguator = s -> s.concat(" (#1)");
        }

        final var rowKeyFactory = getRowKeyFactory();

        final var spec = new JoinSpecification.Builder(leftSettings, rightSettings)//
            .conjunctive(m_joinerNodeSettings.m_compositionMode == CompositionMode.MATCH_ALL)//
            .outputRowOrder(getOutputRowOrder())//
            .retainMatched(isIncludeMatches())//
            .mergeJoinColumns(m_joinerNodeSettings.m_mergeJoinColumns)//
            .columnNameDisambiguator(columnNameDisambiguator)
            .dataCellComparisonMode(getDataCellComparisonMode())//
            .rowKeyFactory(rowKeyFactory.getFirst(), rowKeyFactory.getSecond())//
            .build();

        // RowIDs can only be kept if they are guaranteed to be equal for each pair of matching rows
        if (m_joinerNodeSettings.m_rowKeyFactory == RowKeyFactory.KEEP_ROWID) {
            Optional<String> problem = KeepRowKeysFactory.applicable(//
                spec, m_joinerNodeSettings.m_outputUnmatchedRowsToSeparatePorts);
            if (problem.isPresent()) {
                throw new InvalidSettingsException("Cannot reuse input row keys for output. " + problem.get()); // NOSONAR
            }
        }

        return spec;
    }

    private JoinColumn[] getLeftJoinColumns() {
        return Arrays.stream(m_joinerNodeSettings.m_matchingCriteria).map(criterion -> criterion.m_leftTableColumn)
            .map(JoinSpecificationCreator::toJoinColumn).toArray(JoinColumn[]::new);
    }

    private JoinColumn[] getRightJoinColumns() {
        return Arrays.stream(m_joinerNodeSettings.m_matchingCriteria).map(criterion -> criterion.m_rightTableColumn)
            .map(JoinSpecificationCreator::toJoinColumn).toArray(JoinColumn[]::new);
    }

    private static JoinColumn toJoinColumn(final String columnName) {
        if (SpecialColumns.ROWID.getId().equals(columnName)) {
            return new JoinColumn(SpecialJoinColumn.ROW_KEY);
        }
        return new JoinColumn(columnName);
    }

    private DataCellComparisonMode getDataCellComparisonMode() {
        return switch (m_joinerNodeSettings.m_dataCellComparisonMode) {
            case STRICT -> DataCellComparisonMode.STRICT;
            case STRING -> DataCellComparisonMode.AS_STRING;
            case NUMERIC -> DataCellComparisonMode.NUMERIC_AS_LONG;
        };
    }

    private OutputRowOrder getOutputRowOrder() {
        return switch (m_joinerNodeSettings.m_outputRowOrder) {
            case ARBITRARY -> OutputRowOrder.ARBITRARY;
            case LEFT_RIGHT -> OutputRowOrder.LEFT_RIGHT;
        };
    }

    /**
     * @return Pair of the row key factory and a flag whether in the context of this node the factory is guaranteed to
     * create unique keys. Note that KEEP_ROWID is only applicable if Row ID equality is enforced.
     * @see KeepRowKeysFactory#applicable(JoinSpecification, boolean)
     */
    private Pair<BiFunction<DataRow, DataRow, RowKey>, Boolean> getRowKeyFactory() {
        return switch (m_joinerNodeSettings.m_rowKeyFactory) {
            case CONCATENATE -> Pair
                .create(JoinSpecification.createConcatRowKeysFactory(m_joinerNodeSettings.m_rowKeySeparator), false);
            case KEEP_ROWID -> Pair.create(JoinSpecification.createRetainRowKeysFactory(), true);
            case SEQUENTIAL -> Pair.create(JoinSpecification.createSequenceRowKeysFactory(), true);
        };
    }

    /**
     * This enum holds all ways of joining the two tables.
     */
    enum JoinMode {
            INNER("Inner join", true, false, false), //
            LEFT_OUTER("Left outer join", true, true, false), //
            RIGHT_OUTER("Right outer join", true, false, true), //
            FULL_OUTER("Full outer join", true, true, true), //
            LEFT_ANTI("Left antijoin", false, true, false), //
            RIGHT_ANTI("Right antijoin", false, false, true), //
            FULL_ANTI("Full antijoin", false, true, true), //
            EMPTY("No output rows", false, false, false);

        private final String m_uiDisplayText;

        private final boolean m_includeMatchingRows;

        private final boolean m_includeLeftUnmatchedRows;

        private final boolean m_includeRightUnmatchedRows;

        JoinMode(final String uiDisplayText, final boolean includeMatchingRows,
            final boolean includeLeftUnmatchedRows, final boolean includeRightUnmatchedRows) {
            m_uiDisplayText = uiDisplayText;
            m_includeMatchingRows = includeMatchingRows;
            m_includeLeftUnmatchedRows = includeLeftUnmatchedRows;
            m_includeRightUnmatchedRows = includeRightUnmatchedRows;
        }

        boolean isIncludeMatchingRows() {
            return m_includeMatchingRows;
        }

        boolean isIncludeLeftUnmatchedRows() {
            return m_includeLeftUnmatchedRows;
        }

        boolean isIncludeRightUnmatchedRows() {
            return m_includeRightUnmatchedRows;
        }

        @Override
        public String toString() {
            return m_uiDisplayText;
        }

    }

    JoinMode getJoinMode() {
        return Arrays.stream(JoinMode.values())
            .filter(mode -> mode.isIncludeMatchingRows() == isIncludeMatches()
                && mode.isIncludeLeftUnmatchedRows() == isIncludeLeftUnmatched()
                && mode.isIncludeRightUnmatchedRows() == isIncludeRightUnmatched())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Unknown join mode selected in node configuration."));
    }

    private boolean isIncludeMatches() {
        return m_joinerNodeSettings.m_includeMatchesInOutput;
    }

    private boolean isIncludeLeftUnmatched() {
        return m_joinerNodeSettings.m_includeLeftUnmatchedInOutput;
    }

    private boolean isIncludeRightUnmatched() {
        return m_joinerNodeSettings.m_includeRightUnmatchedInOutput;
    }

}
