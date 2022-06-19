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
 *   Jun 19, 2022 (Dragan Keselj, KNIME GmbH): created
 */
package org.knime.base.node.io.filehandling.arff.writer;

import java.util.Arrays;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * Configuration for the ARFF writer node
 *
 * @author Dragan Keselj, KNIME GmbH
 */
final class ARFFWriter2Config {

    /** The allowed/recommended suffixes for writing a ARFF file */
    protected final String[] FILE_SUFFIXES = new String[]{".arff"};

    private final String DEFAULT_RELATION_NAME = "DataTable";

    /* the file we write to. Must be writable! */
    public static final String CFGKEY_FILE = "file";

    /** The key used to store the filename in the model spec. */
    static final String CFGKEY_SPARSE = "sparseARFF";

    /** The key used to store the relation name in the model spec. */
    static final String CFGKEY_RELATION_NAME = "relation_name";

    /* indicates that we are supposed to write a sparse ARFF file */
    private boolean m_sparse;

    /* the string printed after the "@relation" keyword */
    private String m_relationName;

    private final SettingsModelWriterFileChooser m_fileChooserModel;

    /**
     * Constructor.
     *
     * @param portsConfig the ports configuration
     *
     */
    ARFFWriter2Config(final PortsConfiguration portsConfig) {
        m_relationName = DEFAULT_RELATION_NAME;

        m_fileChooserModel = new SettingsModelWriterFileChooser(CFGKEY_FILE, portsConfig,
            ARFFWriter2NodeFactory.CONNECTION_INPUT_PORT_GRP_NAME, EnumConfig.create(FilterMode.FILE),
            EnumConfig.create(FileOverwritePolicy.FAIL, FileOverwritePolicy.OVERWRITE),
            FILE_SUFFIXES);
    }

    String[] getLocationKeyChain() {
        return Stream.concat(Stream.of(CFGKEY_FILE), Arrays.stream(m_fileChooserModel.getKeysForFSLocation()))
            .toArray(String[]::new);
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserModel.validateSettings(settings);
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
    }

    void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
        try {
            load(settings);
        } catch (final InvalidSettingsException e) { //NOSONAR
            // nothing to do
        }
    }

    void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserModel.loadSettingsFrom(settings);
        m_sparse = settings.getBoolean(CFGKEY_SPARSE, false);
        m_relationName = settings.getString(CFGKEY_RELATION_NAME, DEFAULT_RELATION_NAME);
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        save(settings);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        save(settings);
    }

    void save(final NodeSettingsWO settings) {
        m_fileChooserModel.saveSettingsTo(settings);
        settings.addBoolean(CFGKEY_SPARSE, m_sparse);
        settings.addString(CFGKEY_RELATION_NAME, m_relationName);
    }

    /**
     * @return the file chooser settings model of this configuration
     */
    SettingsModelWriterFileChooser getFileChooserModel() {
        return m_fileChooserModel;
    }

    String getRelationName() {
        return m_relationName;
    }

    boolean isSparse() {
        return m_sparse;
    }

    void setSparse(final boolean sparse) {
        m_sparse = sparse;
    }

    void setRelationName(final String relationName) {
        m_relationName = relationName;
    }

    /**
     * @return {@code true} if existing file should be overwritten
     */
    final boolean isFileOverwritten() {
        return getFileChooserModel().getFileOverwritePolicy() == FileOverwritePolicy.OVERWRITE;
    }

    /**
     * @return {@code true} if existing file should be appended to
     */
    final boolean isFileAppended() {
        return getFileChooserModel().getFileOverwritePolicy() == FileOverwritePolicy.APPEND;
    }

    /**
     * @return {@code true} if existing file should neither be overwritten nor appended to
     */
    final boolean isFileNeverOverwritten() {
        return getFileChooserModel().getFileOverwritePolicy() == FileOverwritePolicy.FAIL;
    }

    static boolean isEmpty(final String str) {
        return str == null || str.trim().isEmpty();
    }

}
