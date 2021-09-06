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
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.node.table.ConfigSerializer;

/**
 * An abstract implementation of a node config for multitable writer nodes.
 *
 * @param <T> the type of {@link DataValue}
 * @param <S> The concrete implementation type (S for self), i.e. the class that is extending this class
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractMultiTableWriterNodeConfig<T extends DataValue, S extends AbstractMultiTableWriterNodeConfig<T, S>> {
    // Allows one instance of ?, file extension will be automatically detected, spaces are allowed in filenames.
    private static final Predicate<String> CFG_FILENAME_PREDICATE =
        Pattern.compile("[^?/\\00]*\\?[^?/\\00]*", Pattern.UNICODE_CHARACTER_CLASS).asMatchPredicate();

    private static final Pattern WHITESPACES_PATTERN = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    protected static final String CFG_OUTPUT_LOCATION = "output_location";

    protected static final String CFG_OUTPUT_FILENAME_PATTERN = "filename_pattern";

    protected static final String CFG_OUTPUT_FILENAME_COLUMN_NAME = "filename_column";

    private static final String DEFAULT_WRITER_TYPE_NAME = "src";

    private final SettingsModelWriterFileChooser m_outputLocation;

    private final SettingsModelString m_sourceColumn;

    private final SettingsModelBoolean m_removeSourceColumn;

    private final SettingsModelString m_filenamePattern;

    private final SettingsModelColumnName m_filenameColumn;

    private final Class<T> m_dataValueClass;

    private final boolean m_compressionSupported;

    private final SettingsModelBoolean m_compressFiles;

    private boolean m_shouldGenerateFilename;

    private final ConfigSerializer<S> m_serializer;

    /**
     * Constructor.
     *
     * @param portsConfig the ports configuration used in the {@link SettingsModelWriterFileChooser}
     * @param connectionInputPortGroupName the name of the port group containing the file system port (used by the
     *            {@link SettingsModelWriterFileChooser})
     * @param dataValueClass the class of the data value
     * @param serializer the {@link ConfigSerializer} used to serialize the model's and dialog's settings which are
     *            saved in this object. If the extending class does not introduce any additional settings or wants to
     *            change the format of the settings somehow, it can use a simple
     *            {@link DefaultMultiTableWriterNodeConfigSerializer}{@code<S>} to serialize the settings.
     * @param enableCompression whether the multi-file writer should support compressing the files it writes.
     * @see #getDefaultSerializer()
     */
    protected AbstractMultiTableWriterNodeConfig(final PortsConfiguration portsConfig,
        final String connectionInputPortGroupName, final Class<T> dataValueClass, final ConfigSerializer<S> serializer,
        final boolean enableCompression) {

        m_compressionSupported = enableCompression;
        m_dataValueClass = dataValueClass;

        m_serializer = serializer;

        m_outputLocation = new SettingsModelWriterFileChooser(CFG_OUTPUT_LOCATION, //
            portsConfig, //
            connectionInputPortGroupName, //
            EnumConfig.create(FilterMode.FOLDER), //
            EnumConfig.create(FileOverwritePolicy.FAIL, FileOverwritePolicy.OVERWRITE, FileOverwritePolicy.IGNORE), //
            EnumSet.of(FSCategory.LOCAL, FSCategory.MOUNTPOINT, FSCategory.RELATIVE));

        final var configTypeString = getConfigTypeString();
        final var cfgWriterTypeNameString = String.format("%s_column", configTypeString);
        final var cfgRemoveWriterColumnString = String.format("remove_%s_column", configTypeString);

        if (m_compressionSupported) {
            final var cfgCompressFilesString = String.format("compress_%s_files", configTypeString);
            m_compressFiles = new SettingsModelBoolean(cfgCompressFilesString, false);
        } else {
            m_compressFiles = null;
        }

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

    /**
     * @return the SettingsModel containing the output location.
     * @apiNote This method should only ever be called by the serializer.
     */
    public final SettingsModelWriterFileChooser getOutputLocation() {
        return m_outputLocation;
    }

    /**
     * @return the SettingsModel containing the selected source column.
     * @apiNote This method should only ever be called by the serializer.
     */
    public final SettingsModelString getSourceColumn() {
        return m_sourceColumn;
    }

    /**
     * @return the SettingsModel containing whether the source column should be removed.
     * @apiNote This method should only ever be called by the serializer.
     */
    public final SettingsModelBoolean getRemoveSourceColumn() {
        return m_removeSourceColumn;
    }

    /**
     * @return the SettingsModel containing whether compression is enabled for the files
     * @apiNote This method should only ever be called by the serializer.
     */
    public final SettingsModelBoolean getCompressFiles() {
        return m_compressFiles;
    }

    /**
     * @return whether the multi-file writer supports compressing the files it writes
     * @apiNote This method should only ever be called by the serializer.
     */
    public final boolean isCompressionSupported() {
        return m_compressionSupported;
    }

    /**
     * @return the SettingsModel containing the configured filename pattern.
     * @apiNote This method should only ever be called by the serializer.
     */
    public final SettingsModelString getFilenamePattern() {
        return m_filenamePattern;
    }

    /**
     * @return the SettingsModel containing the name of the column which contains the file names.
     * @apiNote This method should only ever be called by the serializer.
     */
    public final SettingsModelColumnName getFilenameColumn() {
        return m_filenameColumn;
    }

    /**
     * @return whether a file name should be generated
     * @apiNote This method should only ever be called by the serializer.
     */
    public final boolean shouldGenerateFilename() {
        return m_shouldGenerateFilename;
    }

    /**
     * @param shouldGenerateFilename whether a file name should be generated
     * @apiNote This method should only ever be called by the serializer.
     */
    public final void setShouldGenerateFilename(final boolean shouldGenerateFilename) {
        m_shouldGenerateFilename = shouldGenerateFilename;
    }

    @SuppressWarnings("unchecked") // this forces the extender to specify the correct type
    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_serializer.loadInModel((S)this, settings);
    }

    @SuppressWarnings("unchecked")
    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_serializer.saveInModel((S)this, settings);
    }

    @SuppressWarnings("unchecked")
    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_serializer.validate((S)this, settings);
    }

    @SuppressWarnings("unchecked")
    void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_serializer.loadInDialog((S)this, settings, specs);
    }

    @SuppressWarnings("unchecked")
    void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_serializer.saveInDialog((S)this, settings);
    }

    @SuppressWarnings("unchecked")
    void validateSettingsForDialog(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_serializer.validate((S)this, settings);
    }

    private static boolean isValidFilenamePattern(final String incomingFilename) {
        return CFG_FILENAME_PREDICATE.test(incomingFilename);
    }

    /**
     * replaces the whitespaces with _ for the flow variable keys.
     *
     * @return the writer type name with “_” for every sequence of whitespace encountered
     */
    private String getConfigTypeString() {
        return WHITESPACES_PATTERN.matcher(getWriterTypeName()).replaceAll("_").toLowerCase();
    }

    String getWriterTypeName() {
        final var specificName = getWriterSpecificTypeName();
        if (!StringUtils.isBlank(specificName)) {
            return specificName;
        }
        return DEFAULT_WRITER_TYPE_NAME;
    }

    /**
     * Returns the writer specific type name, which is used to create flow variable keys (e.g. m_sourceColumnSelection,
     * writerSpecificTypeName_column) and text for the dialog border. It should be in a form that would also appear in
     * normal text. The name will be converted to all lower case and any white space will be replaced by an ‘_’ for the
     * flow variable keys. The first letter will be capitalized for the node dialog border and the name will be used as
     * is in other parts of the dialog.
     *
     * E.g. the writer specific type name for the ImageWriter would be <i>image</i> and for the XMLWriter it would be
     * <i>XML</i>.
     *
     * @return writer specific name used to create flow variable keys and dialog border text
     */
    protected abstract String getWriterSpecificTypeName();

    /**
     * Return a serializer that can handle the default settings if no changes to settings serialization are required.
     * This is a convenience method to avoid having to create an instance of
     * {@link DefaultMultiTableWriterNodeConfigSerializer}.
     *
     * @param <S> The concrete implementation type that should be serialized (S for self), i.e. the class that is
     *            extending the {@link AbstractMultiTableWriterNodeConfig} and trying to use the serializer
     * @return a serializer that handles the default settings.
     */
    @SuppressWarnings("unchecked")
    protected static <S extends AbstractMultiTableWriterNodeConfig<?, S>> DefaultMultiTableWriterNodeConfigSerializer<S>
        getDefaultSerializer() {
        return DefaultMultiTableWriterNodeConfigSerializer.DEFAULT_SERIALIZER;
    }
}
