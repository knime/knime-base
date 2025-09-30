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
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 19, 2025 (david): created
 */
package org.knime.base.node.preproc.columnrenameregex;

import java.util.Map;

import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalSearchPatternException;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * Model for the Column Name Replacer node (formerly Column Rename (Regex)).
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ColumnNameReplacerNodeModel
    extends WebUINodeModel<ColumnNameReplacerNodeParametersWithLegacyReplacementStrategy1> {

    ColumnNameReplacerNodeModel(final WebUINodeConfiguration config) {
        super(config, ColumnNameReplacerNodeParametersWithLegacyReplacementStrategy1.class);
    }

    @Override
    protected void validateSettings(final ColumnNameReplacerNodeParametersWithLegacyReplacementStrategy1 settings)
        throws InvalidSettingsException {
        try {
            ColumnNameReplacerUtils.createColumnRenamePattern(settings);
        } catch (IllegalSearchPatternException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final ColumnNameReplacerNodeParametersWithLegacyReplacementStrategy1 settings) throws InvalidSettingsException {
        final var inSpec = inSpecs[0];
        final var renameMapping = getRenameMapping(settings, inSpec);
        final var outSpec = performRenamings(inSpec, renameMapping);
        return new DataTableSpec[]{outSpec};

    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final ColumnNameReplacerNodeParametersWithLegacyReplacementStrategy1 settings) throws Exception {
        BufferedDataTable in = inData[0];
        DataTableSpec oldSpec = in.getDataTableSpec();
        final var renameMapping = getRenameMapping(settings, oldSpec);
        DataTableSpec newSpec = performRenamings(oldSpec, renameMapping);
        BufferedDataTable result = exec.createSpecReplacerTable(in, newSpec);
        return new BufferedDataTable[]{result};
    }

    private Map<String, String> getRenameMapping(final ColumnNameReplacerNodeSettings settings,
        final DataTableSpec inSpec) throws InvalidSettingsException {
        final var renameMapping = ColumnNameReplacerUtils.createColumnRenameMappings( //
            inSpec.getColumnNames(), //
            settings, //
            this::setWarningMessage //
        );
        if (renameMapping.isEmpty() || renameMapping.entrySet().stream().allMatch(e -> e.getKey().equals(e.getValue()))) {
            setWarningMessage("Pattern did not match any column names. Input remains unchanged.");
        }
        return renameMapping;
    }

    /**
     * Convert old to new spec. Note that naming collisions are already resolved
     */
    private static DataTableSpec performRenamings(final DataTableSpec in, final Map<String, String> renameMappings)
        throws InvalidSettingsException {
        DataColumnSpec[] cols = new DataColumnSpec[in.getNumColumns()];
        for (int i = 0; i < cols.length; i++) {
            final DataColumnSpec oldCol = in.getColumnSpec(i);
            final String oldName = oldCol.getName();
            DataColumnSpecCreator creator = new DataColumnSpecCreator(oldCol);
            final String newName = renameMappings.getOrDefault(oldName, oldName);

            if (newName.length() == 0) {
                throw new InvalidSettingsException(
                    "Replacement in column '" + oldName + "' leads to an empty column name.");
            }
            creator.setName(newName);
            cols[i] = creator.createSpec();
        }
        return new DataTableSpec(in.getName(), cols);
    }

}
