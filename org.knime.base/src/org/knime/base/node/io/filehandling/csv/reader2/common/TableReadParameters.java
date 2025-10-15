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
 *   Oct 21, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader2.common;

import java.util.Optional;

import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout.DataArea.LimitNumberOfRows;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout.DataArea.SkipFirstDataRows;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout.DataArea.UseExistingRowId;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

public final class TableReadParameters implements NodeParameters {

    //    static class SkipFirstDataRowsRef extends ReferenceStateProvider<Long> {
    //    }

    @Widget(title = "Skip first data rows", description = SkipFirstDataRows.DESCRIPTION)
    //    @ValueReference(SkipFirstDataRowsRef.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(SkipFirstDataRows.class)
    long m_skipFirstDataRows;

    static final class MaximumNumberOfRowsDefaultProvider implements DefaultValueProvider<Long> {
        @Override
        public Long computeState(final NodeParametersInput context) {
            return 50L;
        }
    }

    //    interface LimitNumberOfRowsRef extends ParameterReference<Boolean> {
    //    }

    @Widget(title = "Limit number of rows", description = LimitNumberOfRows.DESCRIPTION, advanced = true)
    @Layout(LimitNumberOfRows.class)
    //    @ValueReference(LimitNumberOfRowsRef.class)
    @OptionalWidget(defaultProvider = MaximumNumberOfRowsDefaultProvider.class)
    Optional<Long> m_maximumNumberOfRows = Optional.empty();

    /**
     * Access {@link #m_firstColumnContainsRowIds} by this reference.
     */
    public static class FirstColumnContainsRowIdsRef extends ReferenceStateProvider<Boolean> {
    }

    /**
     * This reference is meant to be used to possibly modify title and description of
     * {@link #m_firstColumnContainsRowIds}.
     */
    public static class UseExistingRowIdWidgetRef implements Modification.Reference {
    }

    @Widget(title = "Use existing RowID", description = UseExistingRowId.DESCRIPTION)
    @ValueReference(FirstColumnContainsRowIdsRef.class)
    @Layout(UseExistingRowId.class)
    @Modification.WidgetReference(UseExistingRowIdWidgetRef.class)
    boolean m_firstColumnContainsRowIds;

    void loadFromConfig(final TableReadConfig<?> config) {
        m_skipFirstDataRows = config.skipRows() ? config.getNumRowsToSkip() : 0L;

        m_maximumNumberOfRows = config.getMaxRows() <= 0 ? Optional.empty() : Optional.of(config.getMaxRows());

        m_firstColumnContainsRowIds = config.useRowIDIdx();
    }

    void saveToConfig(final DefaultTableReadConfig<?> config) {
        config.setSkipRows(m_skipFirstDataRows > 0);
        config.setNumRowsToSkip(m_skipFirstDataRows);

        config.setLimitRows(m_maximumNumberOfRows.isPresent());
        config.setMaxRows(m_maximumNumberOfRows.orElse(0L));

        config.setRowIDIdx(0);
        config.setUseRowIDIdx(m_firstColumnContainsRowIds);
    }

}