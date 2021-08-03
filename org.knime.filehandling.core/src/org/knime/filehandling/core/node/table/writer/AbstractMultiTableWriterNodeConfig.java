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
 *   20 Jul 2021 (Moditha Hewasinghage, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.node.table.writer;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * An abstract implementation of a node config for multitable writer nodes.
 *
 * @param <T> the type of {@link DataValue}
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractMultiTableWriterNodeConfig<T extends DataValue> {
    // Allows one instance of ?, file extension will be automatically detected, no spaces are allowed in filenames.
    private static final Predicate<String> CFG_FILENAME_PREDICATE =
        Pattern.compile("[^?\\s/\\00]*\\?[^?\\s/\\00]*", Pattern.UNICODE_CHARACTER_CLASS).asMatchPredicate();

    private static final Pattern WHITESPACES_PATTERN = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private static final String CFG_OUTPUT_LOCATION = "output_location";

    private static final String CFG_OUTPUT_FILENAME_PATTERN = "filename_pattern";

    private static final String CFG_OUTPUT_FILENAME_COLUMN_NAME = "filename_column";

    private static final String CFG_GENERATE_FILE_NAMES = "generate_file_names";

    private static final String DEFAULT_WRITER_TYPE_NAME = "src";

    private final SettingsModelWriterFileChooser m_outputLocation;

    private final SettingsModelString m_sourceColumn;

    private final SettingsModelBoolean m_removeSourceColumn;

    private final SettingsModelString m_filenamePattern;

    private final SettingsModelColumnName m_filenameColumn;

    private final Class<T> m_dataValueClass;

    private boolean m_shouldGenerateFilename;


    /**
     * Constructor.
     *
     * @param portsConfig
     * @param connectionInputPortGrouptName
     * @param dataValueClass
     */
    protected AbstractMultiTableWriterNodeConfig(final PortsConfiguration portsConfig,
        final String connectionInputPortGrouptName, final Class<T> dataValueClass) {

        m_dataValueClass = dataValueClass;

        m_outputLocation = new SettingsModelWriterFileChooser(CFG_OUTPUT_LOCATION, //
            portsConfig, //
            connectionInputPortGrouptName, //
            EnumConfig.create(FilterMode.FOLDER), //
            EnumConfig.create(FileOverwritePolicy.FAIL, FileOverwritePolicy.OVERWRITE, FileOverwritePolicy.IGNORE), //
            EnumSet.of(FSCategory.LOCAL, FSCategory.MOUNTPOINT, FSCategory.RELATIVE));

        final String cfgWriterTypeNameString = String.format("%s_column", getConfigTypeString());

        final String cfgRemoveWriterColumnString = String.format("remove_%s_column", getConfigTypeString());

        m_sourceColumn = new SettingsModelString(cfgWriterTypeNameString, null);

        m_removeSourceColumn = new SettingsModelBoolean(cfgRemoveWriterColumnString, false);

        m_filenamePattern = new SettingsModelString(CFG_OUTPUT_FILENAME_PATTERN, "File_?") {

            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                super.validateSettingsForModel(settings);
                if (!isValidFilenamePattern(settings.getString(CFG_OUTPUT_FILENAME_PATTERN))) {
                    throw new InvalidSettingsException("The file name pattern is not valid.");
                }
            }
        };

        m_filenameColumn = new SettingsModelColumnName(CFG_OUTPUT_FILENAME_COLUMN_NAME, null);

        m_shouldGenerateFilename = true;
        m_filenamePattern.setEnabled(m_shouldGenerateFilename);
        m_filenameColumn.setEnabled(!m_shouldGenerateFilename);

    }

    Class<T> getValueClass() {
        return m_dataValueClass;
    }

    SettingsModelWriterFileChooser getOutputLocation() {
        return m_outputLocation;
    }

    SettingsModelString getSourceColumn() {
        return m_sourceColumn;
    }

    SettingsModelBoolean getRemoveSourceColumn() {
        return m_removeSourceColumn;
    }

    SettingsModelString getFilenamePattern() {
        return m_filenamePattern;
    }

    SettingsModelColumnName getFilenameColumn() {
        return m_filenameColumn;
    }

    private void setShouldGenerateFilename(final boolean shouldGenerateFilename) {
        m_shouldGenerateFilename = shouldGenerateFilename;
    }

    boolean shouldGenerateFilename() {
        return m_shouldGenerateFilename;
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_outputLocation.loadSettingsFrom(settings);
        m_sourceColumn.loadSettingsFrom(settings);
        m_removeSourceColumn.loadSettingsFrom(settings);

        setShouldGenerateFilename(settings.getBoolean(CFG_GENERATE_FILE_NAMES));
        m_filenamePattern.loadSettingsFrom(settings);
        m_filenameColumn.loadSettingsFrom(settings);
        loadWriterSpecificSettingsForModel(settings);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_outputLocation.saveSettingsTo(settings);
        m_sourceColumn.saveSettingsTo(settings);
        m_removeSourceColumn.saveSettingsTo(settings);

        saveFileNameRadioSelectionToSettings(settings);
        m_filenamePattern.saveSettingsTo(settings);
        m_filenameColumn.saveSettingsTo(settings);
        saveWriterSpecificSettingsForModel(settings);
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_outputLocation.validateSettings(settings);
        m_sourceColumn.validateSettings(settings);
        m_removeSourceColumn.validateSettings(settings);

        settings.getBoolean(CFG_GENERATE_FILE_NAMES);
        m_filenamePattern.validateSettings(settings);
        m_filenameColumn.validateSettings(settings);
        validateWriterSpecificSettingsForModel(settings);
    }

    void saveFileNameRadioSelectionForDialog(final NodeSettingsWO settings, final boolean generateFileNameRadio) {
        setShouldGenerateFilename(generateFileNameRadio);
        saveFileNameRadioSelectionToSettings(settings);
    }

    void loadFileNameRadioSelectionForDialog(final NodeSettingsRO settings) {
        setShouldGenerateFilename(settings.getBoolean(CFG_GENERATE_FILE_NAMES, true));
    }

    private void saveFileNameRadioSelectionToSettings(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_GENERATE_FILE_NAMES, shouldGenerateFilename());
    }

    private static boolean isValidFilenamePattern(final String incomingFilename) {
        return CFG_FILENAME_PREDICATE.test(incomingFilename);
    }

    /**
     * replaces the whitespaces with _ for the flow variable keys.
     *
     * @return
     */
    private String getConfigTypeString() {
        return WHITESPACES_PATTERN.matcher(getWriterTypeName()).replaceAll("_");
    }

    String getWriterTypeName() {
        if (getWriterSpecificTypeName() != null && !StringUtils.isBlank(getWriterSpecificTypeName())) {
            return getWriterSpecificTypeName().toLowerCase();
        }
        return DEFAULT_WRITER_TYPE_NAME;
    }

    /**
     * Returns the writer specific type name, which is used to create flow variable keys (e.g. m_sourceColumnSelection,
     * writerSpecificTypeName_column) and text for the dialog border. E.g. the writer specific type name for the
     * ImageWriter would be <i>image</i>.
     *
     * The name can be separated by spaces, which will be replaced by _ when creating flow variables and capitalized for
     * the dialog panels (Do not use _ ).
     *
     * @return writer specific name used to create flow variable keys and dialog border text
     */
    protected abstract String getWriterSpecificTypeName();

    /**
     * Saves additional settings for the model. Implement if there are additional settings specific to the writer node
     * implementation.
     *
     * @param settings
     */
    protected abstract void saveWriterSpecificSettingsForModel(NodeSettingsWO settings);

    /**
     * Loads additional settings for the model. Implement if there are additional settings specific to the writer node
     * implementation.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    protected abstract void loadWriterSpecificSettingsForModel(NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Validates additional settings for the model. Implement if there are additional settings specific to the writer
     * node implementation.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    protected abstract void validateWriterSpecificSettingsForModel(NodeSettingsRO settings)
        throws InvalidSettingsException;

}
