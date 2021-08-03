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
 *   05.08.2021 (jl): created
 */
package org.knime.filehandling.core.node.table.writer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.ConfigSerializer;

/**
 * A class to serialize the default configuration of an {@link AbstractMultiTableWriterNodeConfig}.
 * <p>
 * If no changes to the settings serialization are required, a default instance can be acquired using
 * {@link AbstractMultiTableWriterNodeConfig#getDefaultSerializer()}. This instance can then be passed to the
 * {@link AbstractMultiTableWriterNodeConfig}'s constructor directly.
 * </p>
 * <p>
 * Otherwise if new settings should be supported or the default settings should be handled differently, the
 * {@link DefaultMultiTableWriterNodeConfigSerializer} can be extended and calling the {@code super} methods will handle the default
 * settings.
 * </p>
 *
 * @param <S> The concrete implementation type that should be serialized (S for self), i.e. the class that is
 *            extending the {@link AbstractMultiTableWriterNodeConfig} and trying to use this class
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
public class DefaultMultiTableWriterNodeConfigSerializer<S extends AbstractMultiTableWriterNodeConfig<?, S>>
    implements ConfigSerializer<S> {

    private static final String CFG_GENERATE_FILE_NAMES = "generate_file_names";

    @SuppressWarnings("rawtypes")
    static final DefaultMultiTableWriterNodeConfigSerializer DEFAULT_SERIALIZER = new DefaultMultiTableWriterNodeConfigSerializer();

    @Override
    public void loadInDialog(final S config, final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            loadInModel(config, settings);
        } catch (InvalidSettingsException e) { // NOSONAR: just load the defaults
            config.getSourceColumn().setStringValue(null);
            config.getRemoveSourceColumn().setBooleanValue(false);

            config.setShouldGenerateFilename(true);
            config.getFilenamePattern().setStringValue("File_?");
            config.getFilenameColumn().setStringValue(null);
            if (config.isCompressionSupported()) {
                config.getCompressFiles().setBooleanValue(false);
            }
        }
    }

    @Override
    public void loadInModel(final S config, final NodeSettingsRO settings) throws InvalidSettingsException {
        config.getOutputLocation().loadSettingsFrom(settings);
        config.getSourceColumn().loadSettingsFrom(settings);
        config.getRemoveSourceColumn().loadSettingsFrom(settings);

        config.setShouldGenerateFilename(settings.getBoolean(CFG_GENERATE_FILE_NAMES));
        config.getFilenamePattern().loadSettingsFrom(settings);
        config.getFilenameColumn().loadSettingsFrom(settings);
        if (config.isCompressionSupported()) {
            config.getCompressFiles().loadSettingsFrom(settings);
        }
    }

    @Override
    public void saveInModel(final S config, final NodeSettingsWO settings) {
        config.getOutputLocation().saveSettingsTo(settings);
        config.getSourceColumn().saveSettingsTo(settings);
        config.getRemoveSourceColumn().saveSettingsTo(settings);

        settings.addBoolean(CFG_GENERATE_FILE_NAMES, config.shouldGenerateFilename());
        config.getFilenamePattern().saveSettingsTo(settings);
        config.getFilenameColumn().saveSettingsTo(settings);
        if (config.isCompressionSupported()) {
            config.getCompressFiles().saveSettingsTo(settings);
        }
    }

    @Override
    public void saveInDialog(final S config, final NodeSettingsWO settings) throws InvalidSettingsException {
        saveInModel(config, settings);
    }

    @Override
    public void validate(final S config, final NodeSettingsRO settings) throws InvalidSettingsException {
        config.getOutputLocation().validateSettings(settings);
        config.getSourceColumn().validateSettings(settings);
        config.getRemoveSourceColumn().validateSettings(settings);

        settings.getBoolean(CFG_GENERATE_FILE_NAMES);
        config.getFilenamePattern().validateSettings(settings);
        config.getFilenameColumn().validateSettings(settings);
        if (config.isCompressionSupported()) {
            config.getCompressFiles().validateSettings(settings);
        }
    }
}