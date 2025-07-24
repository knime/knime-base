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

import static org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationUtils.validateColumnName;

import java.util.function.Function;
import java.util.regex.Pattern;

import org.knime.base.node.util.regex.RegexReplaceUtils;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalReplacementException;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalSearchPatternException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationMessageBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationUtils.InvalidColumnNameState;
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
     * @since 5.5
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final StringReplacerNodeSettings modelSettings) throws InvalidSettingsException {
        if (spec.findColumnIndex(modelSettings.m_colName) == -1) {
            throw new InvalidSettingsException("The previously selected column '" + modelSettings.m_colName
                + "' is not available. Please reconfigure the node.");
        }

        Pattern compiledPattern;
        try {
            compiledPattern = createPattern(modelSettings);
        } catch (IllegalSearchPatternException e) {
            throw new InvalidSettingsException("Invalid search pattern: " + e.getMessage(), e);
        }

        var newColumnName = modelSettings.m_createNewCol ? modelSettings.m_newColName : modelSettings.m_colName;
        var colSpec = new DataColumnSpecCreator(newColumnName, StringCell.TYPE).createSpec();

        final String replacement = createReplacement(modelSettings);
        final int index = spec.findColumnIndex(modelSettings.m_colName);
        SingleCellFactory cf = new SingleCellFactory(colSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(index);
                if (cell.isMissing()) {
                    return cell;
                }
                final var originalStringValue = ((StringValue)cell).getStringValue();

                String newStringValue;
                try {
                    newStringValue = RegexReplaceUtils.doReplacement( //
                        compiledPattern, //
                        modelSettings.m_replacementStrategy, //
                        modelSettings.m_patternType, //
                        originalStringValue, //
                        replacement //
                    ).asOptional().orElse(originalStringValue);
                } catch (IllegalReplacementException e) {
                    throw new KNIMEException(
                        "Invalid replacement string '%s'; does it contain an invalid backreference?"
                            .formatted(replacement),
                        e).toUnchecked();
                }

                return StringCellFactory.create(newStringValue);
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

    private static String createReplacement(final StringReplacerNodeSettings modelSettings) {
        return modelSettings.m_useNewFixedWildcardBehavior //
            ? RegexReplaceUtils.processReplacementString(modelSettings.m_replacement, modelSettings.m_patternType) //
            : RegexReplaceUtils.processReplacementStringWithWildcardBackwardCompatibility( //
                modelSettings.m_replacement, //
                modelSettings.m_patternType //
            );
    }

    /**
     * Compile the pattern that will match the string cells
     *
     * @param settings The node settings instance of the current node
     * @return A compiled {@link Pattern} with flags set according to the settings
     * @throws IllegalSearchPatternException if the pattern is invalid
     */
    private static Pattern createPattern(final StringReplacerNodeSettings settings)
        throws IllegalSearchPatternException {
        return RegexReplaceUtils.compilePattern( //
            settings.m_pattern, //
            settings.m_patternType, //
            settings.m_caseMatching, //
            settings.m_enableEscaping //
        );
    }

    private static final Function<InvalidColumnNameState, String> INVALID_COL_NAME_TO_ERROR_MSG =
        new ColumnNameValidationMessageBuilder("new column name").build();

    /**
     * @since 5.5
     */
    @Override
    protected void validateSettings(final StringReplacerNodeSettings settings) throws InvalidSettingsException {
        if (settings.m_createNewCol) {
            if (settings.m_doNotAllowPaddedColumnName) {
                validateColumnName(settings.m_newColName, INVALID_COL_NAME_TO_ERROR_MSG);
            } else if (settings.m_newColName == null || settings.m_newColName.trim().length() == 0) {
                throw new InvalidSettingsException("No name for the new column given");
            }
            if (settings.m_colName == null) {
                throw new InvalidSettingsException("No column selected");
            }
        }

        try {
            createPattern(settings);
        } catch (IllegalSearchPatternException e) {
            throw new InvalidSettingsException("Invalid search pattern: " + settings.m_pattern, e);
        }
    }
}
