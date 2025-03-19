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
package org.knime.base.node.preproc.columnnamereplacer2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.base.node.preproc.columnnamereplacer2.RegexRenameUtils.PatternType;
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
final class ColumnNameReplacer2NodeModel extends WebUINodeModel<ColumnNameReplacer2NodeSettings> {

    /**
     * Creates a new instance of the model.
     *
     * @param config The configuration for the node.
     */
    public ColumnNameReplacer2NodeModel(final WebUINodeConfiguration config) {
        super(config, ColumnNameReplacer2NodeSettings.class);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final ColumnNameReplacer2NodeSettings modelSettings) throws Exception {

        var newSpec = createOutSpec(inData[0].getDataTableSpec(), modelSettings);
        return new BufferedDataTable[]{exec.createSpecReplacerTable(inData[0], newSpec)};
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final ColumnNameReplacer2NodeSettings modelSettings) throws InvalidSettingsException {

        return new DataTableSpec[]{createOutSpec(inSpecs[0], modelSettings)};
    }

    @Override
    protected void validateSettings(final ColumnNameReplacer2NodeSettings settings) throws InvalidSettingsException {
        try {
            createColumnRenamePattern(settings);
        } catch (PatternSyntaxException e) {
            throw new InvalidSettingsException("Invalid pattern: " + settings.m_pattern, e);
        }
    }

    /**
     * Use the settings to create a {@link Pattern} that can be used to rename columns.
     *
     * @throws PatternSyntaxException if something in the settings is invalid and a pattern can't be created
     */
    private static Pattern createColumnRenamePattern(final ColumnNameReplacer2NodeSettings settings)
        throws PatternSyntaxException {
        return RegexRenameUtils.createColumnRenamePattern( //
            settings.m_pattern, //
            settings.m_patternType, //
            settings.m_caseSensitivity, //
            settings.m_replacementStrategy //
        );
    }

    private DataTableSpec createOutSpec(final DataTableSpec inSpec, final ColumnNameReplacer2NodeSettings settings) {
        // if there are no columns in the input we can just return right now
        if (inSpec.getNumColumns() == 0) {
            return inSpec;
        }

        var compiledPattern = createColumnRenamePattern(settings);

        // only allow replacement backrefs like $1 if the pattern is a regex
        var replacementString = settings.m_patternType == PatternType.REGEX //
            ? settings.m_replacement //
            : Matcher.quoteReplacement(settings.m_replacement);

        var renameMapping =
            RegexRenameUtils.columnRenameMappings(inSpec.getColumnNames(), compiledPattern, replacementString);

        // if there are now duplicate column names, we should warn. But we'll use a unique name generator
        // so we don't actually have an error.
        if (RegexRenameUtils.renamesHaveCollisions(renameMapping)) {
            setWarningMessage("Pattern replace resulted in duplicate column names. Conflicts were resolved by adding "
                + "\"(#index)\" suffix.");
        } else if (RegexRenameUtils.renamesAreAllSame(renameMapping)) {
            setWarningMessage("Pattern did not change any column names.");
        }

        // this won't change anything if we have no collisions
        renameMapping = RegexRenameUtils.fixCollisions(renameMapping);

        var newSpecs = renameMapping.entrySet().stream() //
            .map(entry -> new DataColumnSpecCreator( //
                entry.getValue(), //
                inSpec.getColumnSpec(entry.getKey()).getType() //
            ).createSpec()) //
            .toArray(DataColumnSpec[]::new);

        return new DataTableSpec(newSpecs);
    }
}
