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
 *   Jul 13, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.filter.constvalcol;

import java.util.function.Consumer;

import org.knime.base.node.preproc.filter.constvalcol.ConstantValueColumnFilterNodeParameters.FilterMode;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.CanceledExecutionException;
import org.knime.node.DefaultModel.ConfigureInput;
import org.knime.node.DefaultModel.ConfigureOutput;
import org.knime.node.DefaultModel.ExecuteInput;
import org.knime.node.DefaultModel.ExecuteOutput;
import org.knime.node.DefaultNode;
import org.knime.node.DefaultNodeFactory;

/**
 * Node factory for the Constant Value Column Filter node.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class ConstantValueColumnFilterNodeFactory extends DefaultNodeFactory {

    private static final DefaultNode NODE = DefaultNode.create() //
        .name("Constant Value Column Filter") //
        .icon("constvalcolfilter.png") //
        .shortDescription("""
                The Constant Value Column Filter filters columns
                containing only duplicates of the same value.
                """) //
        .fullDescription("""
                This node filters columns exclusively containing duplicates of
                the same value from the input data table. Examples include a column
                containing only zeroes, a column containing only identical Strings,
                or a column comprising only missing cells. In a dialog window, you
                can select which columns to apply the filter to (with all columns
                selected by default). From these selected columns, you can choose to
                remove either all constant value columns or columns containing only
                specific constant numeric, String, or missing values. Finally, you
                can also specify the minimum number of rows a table must have to be
                considered for filtering.
                """) //
        .sinceVersion(3, 6, 0) //
        .ports(p -> p //
            .addInputTable("Table to be filtered", "Table from which columns are to be filtered.") //
            .addOutputTable("Filtered Table", "Table excluding filtered columns.")) //
        .model(m -> m //
            .parametersClass(ConstantValueColumnFilterNodeParameters.class) //
            .configure(ConstantValueColumnFilterNodeFactory::configure) //
            .execute(ConstantValueColumnFilterNodeFactory::execute))
        .nodeType(NodeType.Manipulator); //

    private static final String WARNING_ONEROW = """
            Input table contains only one row. \
            All of its columns are considered constant value columns.
            """;

    private static final String WARNING_SMALL_TABLE = """
            Input table has fewer rows than the minimum specified in the filter settings. \
            Constant value column filtering disabled.
            """;

    static final String WARNING_NO_FILTER_SELECTED = """
            Filter configured to only remove columns with specific constant values but no such values are specified. \
            Constant value column filtering disabled.
            """;

    /**
     * Default constructor for the node factory.
     */
    public ConstantValueColumnFilterNodeFactory() {
        super(NODE);
    }

    private static void configure(final ConfigureInput i, final ConfigureOutput o) {

        setWarningIfNoFilterSelected(i.getParameters(), o::setWarningMessage);

        /*
         * The columns containing only constant values cannot be determined without looking at the data contained within
         * the table. Hence, the DataTableSpec cannot be determined before execution onset.
         */
    }

    private static void setWarningIfNoFilterSelected(final ConstantValueColumnFilterNodeParameters params,
        final Consumer<String> warningMessageConsumer) {

        if (params.m_filterMode == FilterMode.BY_VALUE && params.m_filterNumeric.isEmpty()
            && params.m_filterString.isEmpty() && !params.m_filterMissing) {
            warningMessageConsumer.accept(WARNING_NO_FILTER_SELECTED);
        }
    }

    private static void execute(final ExecuteInput i, final ExecuteOutput o) throws CanceledExecutionException {

        final var inputTable = i.getInTable(0);
        final ConstantValueColumnFilterNodeParameters settings = i.getParameters();
        if (inputTable.size() < settings.m_minRows) {
            o.setWarningMessage(WARNING_SMALL_TABLE);
        } else if (inputTable.size() == 1) {
            o.setWarningMessage(WARNING_ONEROW);
        } else {
            setWarningIfNoFilterSelected(settings, o::setWarningMessage);
        }

        final var filter = new ConstantValueColumnFilter.ConstantValueColumnFilterBuilder()
            .filterAll(settings.m_filterMode == FilterMode.ALL) //
            .filterNumeric(settings.m_filterNumeric.isPresent()) //
            .filterNumericValue(settings.m_filterNumeric.orElse(0.0)) //
            .filterString(settings.m_filterString.isPresent()) //
            .filterStringValue(settings.m_filterString.orElse("")) //
            .filterMissing(settings.m_filterMissing) //
            .rowThreshold(settings.m_minRows) //
            .createConstantValueColumnFilter();

        final var spec = inputTable.getDataTableSpec();
        final var toFilter = settings.m_consideredColumns.filterFromFullSpec(spec);
        final var exec = i.getExecutionContext();
        final String[] toRemove = filter.determineConstantValueColumns(inputTable, toFilter, exec);

        final var columnRearranger = new ColumnRearranger(spec);
        columnRearranger.remove(toRemove);
        o.setOutData(exec.createColumnRearrangeTable(inputTable, columnRearranger, exec));
    }
}
