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
 *   18.06.2007 (thor): created
 */
package org.knime.base.node.preproc.stringreplacer;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.node.preproc.stringreplacer.StringReplacerNodeSettings.PatternType;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;

/**
 * This is the model for the string replacer node that does the work.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class StringReplacerNodeModel extends SimpleStreamableFunctionNodeModel {

    private final StringReplacerSettings m_settings = new StringReplacerSettings();

    /**
     * A pre-compiled {@link Pattern} that matches a back-reference in a replacement string and captures them.
     *
     * See https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#cg
     *
     * We match on numerical and named back-references as specified in the above doc
     */
    private static final Pattern backreferencePattern = //
        Pattern.compile("(\\$\\d+|\\$\\{[A-Za-z][A-Za-z0-9]*\\})"); //NOSONAR: only match spec

    /**
     * Creates the column rearranger that computes the new cells.
     *
     * @param spec the spec of the input table
     * @return a column rearranger
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        final var compiledPattern = createPattern(m_settings);

        var newColumnName = m_settings.createNewColumn() ? m_settings.newColumnName() : m_settings.columnName();
        var colSpec = new DataColumnSpecCreator(newColumnName, StringCell.TYPE).createSpec();

        final String replacement;
        if (m_settings.patternType() == PatternType.WILDCARD) {
            // Remove back-references when matching with wildcards
            var backreferenceMatcher = backreferencePattern.matcher(m_settings.replacement());
            replacement = backreferenceMatcher.replaceAll("\\\\$1");
        } else {
            replacement = m_settings.replacement();
        }
        final int index = spec.findColumnIndex(m_settings.columnName());
        SingleCellFactory cf = new SingleCellFactory(colSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(index);
                if (cell.isMissing()) {
                    return cell;
                }
                final var originalStringValue = ((StringValue)cell).getStringValue();
                if (m_settings.patternType() == PatternType.LITERAL) {
                    return new StringCell(getLiteralReplacementString(m_settings, replacement, originalStringValue));
                } else if (compiledPattern.isPresent() && (m_settings.patternType() == PatternType.REGEX
                    || m_settings.patternType() == PatternType.WILDCARD)) {
                    return new StringCell(getPatternReplacementString(m_settings, compiledPattern.get(), replacement,
                        originalStringValue));
                } else {
                    return new StringCell(originalStringValue);
                }
            }
        };

        var crea = new ColumnRearranger(spec);
        if (m_settings.createNewColumn()) {
            if (spec.containsName(m_settings.newColumnName())) {
                throw new InvalidSettingsException("Duplicate column name: " + m_settings.newColumnName());
            }
            crea.append(cf);
        } else {
            crea.replace(cf, m_settings.columnName());
        }

        return crea;
    }

    /**
     * Optionally (depending on the node settings) compile the pattern that will matched to the string cells
     * @param settings The node settings instance of the current node
     * @return A compiled {@link Pattern}, or {@code Optional.empty()} if compilation is not necessary
     */
    private static Optional<Pattern> createPattern(final StringReplacerSettings settings) {
        String regex;
        var flags = 0;
        if (settings.patternType() == PatternType.REGEX) {
            regex = settings.pattern();
        } else if (settings.patternType() == PatternType.WILDCARD) {
            regex = WildcardMatcher.wildcardToRegex(settings.pattern(), settings.enableEscaping());
            flags = Pattern.DOTALL | Pattern.MULTILINE;
        } else {
            // no Pattern needs be compiled for other types of patterns
            return Optional.empty();
        }
        // support for \n and international characters
        if (!settings.caseSensitive()) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        return Optional.of(Pattern.compile(regex, flags));
    }

    /**
     * Perform the replacement operation for literal string matching on one input string
     *
     * @param settings The relevant node settings instance
     * @param replacement The replacement string
     * @param originalStringValue The original string (the cell content before any modification)
     * @return A new string that has all occurrences of the literal pattern replaced by the replacement string
     */
    private static String getLiteralReplacementString(final StringReplacerSettings settings, final String replacement,
        final String originalStringValue) {
        if (settings.caseSensitive()) {
            if (settings.replaceAllOccurrences() && StringUtils.contains(originalStringValue, settings.pattern())) {
                // we check for contains so we can return Optional.empty() else
                return StringUtils.replace(originalStringValue, settings.pattern(), replacement);
            } else if (!settings.replaceAllOccurrences()
                && StringUtils.equals(originalStringValue, settings.pattern())) {
                // replace whole string
                return replacement;
            }
        } else {
            if (settings.replaceAllOccurrences()
                && StringUtils.containsIgnoreCase(originalStringValue, settings.pattern())) {
                // we check for contains so we can return Optional.empty() else
                return
                    StringUtils.replaceIgnoreCase(originalStringValue, settings.pattern(), replacement);
            } else if (!settings.replaceAllOccurrences()
                && StringUtils.equalsIgnoreCase(originalStringValue, settings.pattern())) {
                // replace whole string
                return replacement;
            }
        }
        return originalStringValue;
    }

    /**
     * Perform the replacement operation for string matching with RegEx or Wildcard on one input string
     *
     * @param settings The relevant node settings instance
     * @param pattern The compiled pattern that will be used to search in the input string
     * @param replacement The replacement string
     * @param originalStringValue The original string (the cell content before any modification)
     * @return A new string that has all occurrences of the literal pattern replaced by the replacement string
     */
    private static String getPatternReplacementString(final StringReplacerSettings settings, final Pattern pattern,
        final String replacement, final String originalStringValue) {
        var m = pattern.matcher(originalStringValue);
        if (settings.replaceAllOccurrences()) {
            return switch (settings.patternType()) {
                // Intentionally don't alter any regex behaviour here so we can rely on the Java Pattern Doc
                case REGEX -> m.replaceAll(replacement);
                // Replace e.g. "*" only once, otherwise fall back to replaceAll
                case WILDCARD -> m.matches() ? replacement : m.replaceAll(replacement);
                default -> throw new IllegalStateException(
                    "The PatternReplacer can only handle RegEx and Wildcard Replacements");
            };
        } else if (m.matches()) { // whole string matches, replace whole string
            // Here, there used to be a check whether the pattern is `.*`
            // This has been removed, since m.matches() already indicates that the whole string matches.
            // We use the state of the matcher now (i.e. start() and end() point to the start and end of the
            // string) to build a new string from the replacement. The replacement might contain
            // back-references.
            var sb = new StringBuilder();
            m.appendReplacement(sb, replacement);
            return sb.toString();
        }
        return originalStringValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_settings.columnName() == null) {
            m_settings.columnName(getLastStringColumn(inSpecs[0]));
            setWarningMessage("No column selected, using '" + m_settings.columnName() + "'.");
        } else if (inSpecs[0].findColumnIndex(m_settings.columnName()) == -1) {
            throw new InvalidSettingsException("The previously selected column '" + m_settings.columnName()
                + "' is not available. " + "Please reconfigure the node.");
        }

        var crea = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{crea.createSpec()};
    }

    private static String getLastStringColumn(final DataTableSpec dataTableSpec) throws InvalidSettingsException {
        final var lastMatchingColumn = IntStream.range(0, dataTableSpec.getNumColumns()) //
            .map(i -> dataTableSpec.getNumColumns() - i - 1) //
            .mapToObj(dataTableSpec::getColumnSpec) //
            .filter(columnSpec -> columnSpec.getType().isCompatible(StringValue.class)) //
            .findFirst();
        return lastMatchingColumn.orElseThrow(() -> new InvalidSettingsException("No String column available."))
            .getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        exec.setMessage("Searching & Replacing");
        var crea = createColumnRearranger(inData[0].getDataTableSpec());
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[0], crea, exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        var s = new StringReplacerSettings();
        s.loadSettings(settings);

        if (s.createNewColumn() && (s.newColumnName() == null || s.newColumnName().trim().length() == 0)) {
            throw new InvalidSettingsException("No name for the new column given");
        }
        if (s.columnName() == null) {
            throw new InvalidSettingsException("No column selected");
        }
        if (s.pattern() == null) {
            throw new InvalidSettingsException("No pattern given");
        }
        if (s.replacement() == null) {
            throw new InvalidSettingsException("No replacement string given");
        }

        createPattern(s);
    }

}
