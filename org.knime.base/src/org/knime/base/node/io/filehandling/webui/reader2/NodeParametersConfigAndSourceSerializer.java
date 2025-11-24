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
 *   Nov 21, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.webui.reader2;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.filehandling.core.node.table.reader.CommonTableReaderNodeFactory.ConfigAndSourceSerializer;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.paths.Source;
import org.knime.node.parameters.NodeParameters;

/**
 * Use this serializer as base class for serializers based on {@link NodeParameters}.
 *
 * @param <P> the type of node parameters
 * @param <I> the item type to read from
 * @param <S> the type of {@link Source}
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type used to identify external data types
 * @param <M> the type of {@link MultiTableReadConfig}
 *
 * @author Paul Bärnreuther
 * @since 5.10
 */
@SuppressWarnings("restriction")
public abstract class NodeParametersConfigAndSourceSerializer<P extends NodeParameters, I, S extends Source<I>, //
        C extends ReaderSpecificConfig<C>, T, M extends MultiTableReadConfig<C, T>>
    implements ConfigAndSourceSerializer<I, S, C, T, M> {

    private final Class<P> m_paramsClass;

    /**
     * @param paramsClass the class of the node parameters
     */
    protected NodeParametersConfigAndSourceSerializer(final Class<P> paramsClass) {
        m_paramsClass = paramsClass;
    }

    private P m_params;

    @Override
    public final void validateSettings(final S source, final M config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_params = NodeParametersUtil.loadSettings(settings, m_paramsClass);
        m_params.validate();
    }

    @Override
    public final void saveSettingsTo(final S source, final M config, final NodeSettingsWO settings) {
        if (m_params == null) {
            // Could be improved by passing actual specs as soon as we need the default settings to depend on them
            m_params = NodeParametersUtil.createSettings(m_paramsClass, new PortObjectSpec[0]);
        }
        NodeParametersUtil.saveSettings(m_paramsClass, m_params, settings);
    }

    @Override
    public final void loadValidatedSettingsFrom(final S source, final M config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        // validateSettings is guaranteed to be called before this method
        saveToSourceAndConfig(m_params, source, config);
    }

    /**
     * Saves the values from the given parameters to the given source and config.
     *
     * @param params the node parameters
     * @param source the source
     * @param config the reader specific config
     */
    protected abstract void saveToSourceAndConfig(final P params, final S source, final M config);

}
