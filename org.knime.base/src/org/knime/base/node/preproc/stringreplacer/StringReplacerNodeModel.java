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

import static org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationV2Utils.validateColumnName;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;

/**
 * This is the model for the string replacer node that does the work.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
@SuppressWarnings("restriction")
public class StringReplacerNodeModel extends WebUISimpleStreamableFunctionNodeModel<StringReplacerNodeSettings> {

    /**
     * @param config the {@link WebUINodeConfiguration}
     * @since 5.5
     */
    protected StringReplacerNodeModel(final WebUINodeConfiguration config) {
        super(config, StringReplacerNodeSettings.class);
    }

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
     * @since 5.5
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final StringReplacerNodeSettings modelSettings) throws InvalidSettingsException {
        if (modelSettings.m_colName == null) {
            modelSettings.m_colName = getLastStringColumn(spec);
        } else if (spec.findColumnIndex(modelSettings.m_colName) == -1) {
            throw new InvalidSettingsException("The previously selected column '" + modelSettings.m_colName
                + "' is not available. Please reconfigure the node.");
        }

        final var compiledPattern = createPattern(modelSettings);

        var newColumnName = modelSettings.m_createNewCol ? modelSettings.m_newColName : modelSettings.m_colName;
        var colSpec = new DataColumnSpecCreator(newColumnName, StringCell.TYPE).createSpec();

        final String replacement;
        if (modelSettings.m_patternType == PatternType.WILDCARD) {
            // Remove back-references when matching with wildcards
            var backreferenceMatcher = backreferencePattern.matcher(modelSettings.m_replacement);
            replacement = backreferenceMatcher.replaceAll("\\\\$1");
        } else {
            replacement = modelSettings.m_replacement;
        }
        final int index = spec.findColumnIndex(modelSettings.m_colName);
        SingleCellFactory cf = new SingleCellFactory(colSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(index);
                if (cell.isMissing()) {
                    return cell;
                }
                final var originalStringValue = ((StringValue)cell).getStringValue();
                if (modelSettings.m_patternType == PatternType.LITERAL) {
                    return new StringCell(getLiteralReplacementString(modelSettings, replacement, originalStringValue));
                } else if (compiledPattern.isPresent() && (modelSettings.m_patternType == PatternType.REGEX
                    || modelSettings.m_patternType == PatternType.WILDCARD)) {
                    return new StringCell(getPatternReplacementString(modelSettings, compiledPattern.get(), replacement,
                        originalStringValue));
                } else {
                    return new StringCell(originalStringValue);
                }
            }
        };

        var crea = new ColumnRearranger(spec);
        if (modelSettings.m_createNewCol) {
            if (spec.containsName(modelSettings.m_newColName)) {
                throw new InvalidSettingsException("Duplicate column name: " + modelSettings.m_newColName);
            }
            crea.append(cf);
        } else {
            crea.replace(cf, modelSettings.m_colName);
        }

        return crea;
    }

    /**
     * Optionally (depending on the node settings) compile the pattern that will matched to the string cells
     *
     * @param settings The node settings instance of the current node
     * @return A compiled {@link Pattern}, or {@code Optional.empty()} if compilation is not necessary
     */
    private static Optional<Pattern> createPattern(final StringReplacerNodeSettings settings) {
        String regex;
        var flags = 0;
        if (settings.m_patternType == PatternType.REGEX) {
            regex = settings.m_pattern;
        } else if (settings.m_patternType == PatternType.WILDCARD) {
            regex = WildcardMatcher.wildcardToRegex(settings.m_pattern, settings.m_enableEscaping);
            flags = Pattern.DOTALL | Pattern.MULTILINE;
        } else {
            // no Pattern needs be compiled for other types of patterns
            return Optional.empty();
        }
        // support for \n and international characters

        if (settings.m_caseMatching == CaseMatching.CASEINSENSITIVE) {
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
    private static String getLiteralReplacementString(final StringReplacerNodeSettings settings,
        final String replacement, final String originalStringValue) {
        if (settings.m_caseMatching == CaseMatching.CASESENSITIVE) {
            if (settings.m_replacementStrategy == ReplacementStrategy.ALL_OCCURRENCES
                && StringUtils.contains(originalStringValue, settings.m_pattern)) {
                // we check for contains so we can return Optional.empty() else
                return StringUtils.replace(originalStringValue, settings.m_pattern, replacement);
            } else if (settings.m_replacementStrategy != ReplacementStrategy.ALL_OCCURRENCES
                && StringUtils.equals(originalStringValue, settings.m_pattern)) {
                // replace whole string
                return replacement;
            }
        } else {
            if (settings.m_replacementStrategy == ReplacementStrategy.ALL_OCCURRENCES
                && StringUtils.containsIgnoreCase(originalStringValue, settings.m_pattern)) {
                // we check for contains so we can return Optional.empty() else
                return StringUtils.replaceIgnoreCase(originalStringValue, settings.m_pattern, replacement);
            } else if (settings.m_replacementStrategy != ReplacementStrategy.ALL_OCCURRENCES
                && StringUtils.equalsIgnoreCase(originalStringValue, settings.m_pattern)) {
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
    private static String getPatternReplacementString(final StringReplacerNodeSettings settings, final Pattern pattern,
        final String replacement, final String originalStringValue) {
        var m = pattern.matcher(originalStringValue);
        if (settings.m_replacementStrategy == ReplacementStrategy.ALL_OCCURRENCES) {
            return switch (settings.m_patternType) {
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
     * @since 5.5
     */
    @Override
    protected void validateSettings(final StringReplacerNodeSettings settings) throws InvalidSettingsException {
        if (settings.m_createNewCol) {
            if (settings.m_isColumnNameValidationV2) {
                validateColumnName(settings.m_newColName, "New column name");
            } else {
                if (settings.m_newColName == null || settings.m_newColName.trim().length() == 0) {
                    throw new InvalidSettingsException("No name for the new column given");
                }
            }
        }
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

}
