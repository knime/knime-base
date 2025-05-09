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
import java.util.Set;
import java.util.regex.Pattern;

import org.knime.base.node.util.regex.RegexReplaceUtils;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalReplacementException;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalSearchPatternException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;

/**
 * Model for the Column Name Replacer node (formerly Column Rename (Regex)).
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ColumnNameReplacerNodeModel extends WebUISimpleStreamableFunctionNodeModel<ColumnNameReplacerNodeSettings> {

    /**
     * Creates a new instance of the model.
     *
     * @param config The configuration for the node.
     */
    public ColumnNameReplacerNodeModel(final WebUINodeConfiguration config) {
        super(config, ColumnNameReplacerNodeSettings.class);
    }

    @Override
    protected void validateSettings(final ColumnNameReplacerNodeSettings settings) throws InvalidSettingsException {
        try {
            createColumnRenamePattern(settings);
        } catch (IllegalSearchPatternException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
    }

    /**
     * Use the settings to create a {@link Pattern} that can be used to rename columns.
     *
     * @throws IllegalSearchPatternException if the pattern is invalid
     */
    private static Pattern createColumnRenamePattern(final ColumnNameReplacerNodeSettings settings)
        throws IllegalSearchPatternException {
        return RegexReplaceUtils.compilePattern( //
            settings.m_pattern, //
            settings.m_patternType, //
            settings.m_caseSensitivity, //
            settings.m_enableEscapingWildcard, //
            settings.m_properlySupportUnicodeCharacters //
        );
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
        final ColumnNameReplacerNodeSettings settings) {
        // if there are no columns in the input we can just return right now
        if (inSpec.getNumColumns() == 0) {
            return new ColumnRearranger(inSpec);
        }

        Map<String, String> renameMapping;
        try {
            renameMapping = ColumnNameReplacerUtils.columnRenameMappings( //
                inSpec.getColumnNames(), //
                settings.m_pattern, //
                settings.m_patternType, //
                settings.m_caseSensitivity, //
                settings.m_replacementStrategy, //
                settings.m_enableEscapingWildcard, //
                settings.m_properlySupportUnicodeCharacters, //
                settings.m_replacement //
            );
        } catch (IllegalSearchPatternException e) {
            // we should be covered by validateSettings, so this is an implementation error
            throw new IllegalStateException("Implementation error: " + e.getMessage(), e);
        } catch (IllegalReplacementException e) {
            throw new KNIMEException("Replacement string contained an invalid group.", e).toUnchecked();
        }

        if (renameMapping.isEmpty()) {
            setWarningMessage("Pattern did not match any column names. Input remains unchanged.");
        } else if (ColumnNameReplacerUtils.renamesHaveCollisions(renameMapping)) {
            // if there are now duplicate column names, we should warn. But we'll use a unique name generator
            // so we don't actually have an error.
            setWarningMessage("Pattern replace resulted in duplicate column names. Conflicts were resolved by adding "
                + "\"(#index)\" suffix.");
        }

        // this won't change anything if we have no collisions
        renameMapping = ColumnNameReplacerUtils.fixCollisions(Set.of(inSpec.getColumnNames()), renameMapping);

        var rearranger = new ColumnRearranger(inSpec);

        for (final var entry : renameMapping.entrySet()) {
            // add the new column name to the rearranger
            final var inColIndex = inSpec.findColumnIndex(entry.getKey());
            var newCellFactory = new ColumnRenamingCellFactory( //
                entry.getValue(), //
                inSpec.getColumnSpec(inColIndex), //
                inColIndex//
            );
            rearranger.replace(newCellFactory, entry.getKey());
        }

        return rearranger;
    }

    /**
     * Cell factory that returns the original cell but with a new column name.
     */
    private static class ColumnRenamingCellFactory extends SingleCellFactory {

        private int m_targetColumnIndex;

        ColumnRenamingCellFactory(final String newName, final DataColumnSpec oldSpec, final int targetColumnIndex) {
            super(renameColumn(oldSpec, newName));

            m_targetColumnIndex = targetColumnIndex;
        }

        private static DataColumnSpec renameColumn(final DataColumnSpec column, final String name) {
            var creator = new DataColumnSpecCreator(column);
            creator.setName(name);
            return creator.createSpec();
        }

        @Override
        public DataCell getCell(final DataRow row) {
            return row.getCell(m_targetColumnIndex);
        }
    }
}
