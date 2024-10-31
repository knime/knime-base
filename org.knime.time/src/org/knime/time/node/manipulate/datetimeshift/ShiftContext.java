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
 *   Nov 11, 2024 (Tobias Kampmann): created
 */
package org.knime.time.node.manipulate.datetimeshift;

import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.function.Consumer;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.message.Message;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.time.util.Granularity;
import org.knime.time.util.ReplaceOrAppend;

/**
 * type to allow handling of time and date shift in a generic way.
 *
 * @param supportedTypes supported column types that are allowed for the shift
 * @param temporalColumn name of the temporal column containing a period if the shift is date-based or a duration if the
 *            shift is time-based.
 * @param numericalColumn name of the numerical column containing the shift value.
 * @param temporalValue value of the temporal column containing a period if the shift is date-based or a duration if the
 *            shift is time-based.
 * @param granularity granularity of the shift (e.g. days, hours, minutes). Must be set if numericalColumn or shiftValue
 *            is set.
 * @param replaceOrAppend whether to append the shifted column or replace the original column
 * @param outputColumnSuffix suffix to append to the output column name
 * @param columnFilter column filter to select valid columns for the shift
 * @param messageBuilder message builder that provides utility to communicate messages to the user
 * @param messageConsumer message consumer
 */
@SuppressWarnings("restriction")
record ShiftContext( // NOSONAR
    String temporalColumn, //
    TemporalAmount temporalValue, //
    String numericalColumn, //
    Granularity granularity, //
    ReplaceOrAppend replaceOrAppend, //
    String outputColumnSuffix, //
    String[] selectedColumnNames, //
    MessageBuilder messageBuilder, //
    Consumer<Message> messageConsumer) {

    /**
     * Builder class to create a {@link ShiftContext}.
     */
    public static class Builder {
        private String m_temporalColumn;

        private TemporalAmount m_temporalValue;

        private String m_numericalColumn;

        private Granularity m_granularity;

        private ReplaceOrAppend m_replaceOrAppend;

        private String m_outputColumnSuffix;

        private String[] m_selectedColumnNames;

        private MessageBuilder m_messageBuilder;

        private Consumer<Message> m_messageConsumer;

        /**
         * @param temporalColumn
         * @return this
         */
        public Builder temporalColumn(final String temporalColumn) {
            this.m_temporalColumn = temporalColumn;
            return this;
        }

        /**
         * @param temporalValue
         * @return this
         */
        public Builder temporalValue(final TemporalAmount temporalValue) {
            this.m_temporalValue = temporalValue;
            return this;
        }

        /**
         * @param numericalColumn
         * @return this
         */
        public Builder numericalColumn(final String numericalColumn) {
            this.m_numericalColumn = numericalColumn;
            return this;
        }

        /**
         * @param granularity
         * @return this
         */
        public Builder granularity(final Granularity granularity) {
            this.m_granularity = granularity;
            return this;
        }

        /**
         * @param replaceOrAppend
         * @return this
         */
        public Builder replaceOrAppend(final ReplaceOrAppend replaceOrAppend) {
            this.m_replaceOrAppend = replaceOrAppend;
            return this;
        }

        /**
         * @param outputColumnSuffix
         * @return this
         */
        public Builder outputColumnSuffix(final String outputColumnSuffix) {
            this.m_outputColumnSuffix = outputColumnSuffix;
            return this;
        }

        /**
         * @param columnFilter
         * @param supportedTypes
         * @param spec
         * @return this
         */
        public Builder selectedColumnNames(final ColumnFilter columnFilter,
            final Collection<Class<? extends DataValue>> supportedTypes, final DataTableSpec spec) {
            this.m_selectedColumnNames =
                columnFilter.getSelected(DateTimeShiftUtils.getCompatibleColumns(spec, supportedTypes), spec);
            return this;
        }

        /**
         * @param selectedColumnNames
         * @return this
         */
        public Builder selectedColumnNames(final String[] selectedColumnNames) {
            this.m_selectedColumnNames = selectedColumnNames;
            return this;
        }

        /**
         * @param messageBuilder
         * @return this
         */
        public Builder messageBuilder(final MessageBuilder messageBuilder) {
            this.m_messageBuilder = messageBuilder;
            return this;
        }

        /**
         * @param messageConsumer
         * @return this
         */
        public Builder messageConsumer(final Consumer<Message> messageConsumer) {
            this.m_messageConsumer = messageConsumer;
            return this;
        }

        /**
         * @return ShiftNodeExecutionContext
         */
        public ShiftContext build() {
            return new ShiftContext(m_temporalColumn, m_temporalValue, m_numericalColumn, m_granularity,
                m_replaceOrAppend, m_outputColumnSuffix, m_selectedColumnNames, m_messageBuilder, m_messageConsumer);
        }
    }
}
