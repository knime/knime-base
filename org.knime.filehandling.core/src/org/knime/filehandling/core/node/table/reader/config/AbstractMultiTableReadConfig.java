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
 *   Nov 15, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;

/**
 * Abstract implementation of a {@link MultiTableReadConfig} that provides getters and getters.<br>
 * It also handles serialization via a {@link ConfigSerializer}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig} used in the node implementation
 * @param <TC> the type of {@link TableReadConfig} used in the node implementation
 * @param <T> the type used to identify external data types
 * @param <S> The concrete implementation type (S for self)
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class AbstractMultiTableReadConfig<C extends ReaderSpecificConfig<C>,
TC extends TableReadConfig<C>, T, S extends AbstractMultiTableReadConfig<C, TC, T, S>>
    implements StorableMultiTableReadConfig<C, T> {

    private final ConfigSerializer<S> m_serializer;

    private final TC m_tableReadConfig;

    private TableSpecConfig<T> m_tableSpecConfig = null;

    private boolean m_failOnDifferingSpecs = true;

    private boolean m_skipEmptyColumns = false;

    /**
     * @deprecated Only used as fallback if no TableSpecConfig is available
     */
    @Deprecated
    private SpecMergeMode m_specMergeMode = SpecMergeMode.UNION;

    /**
     * Constructor.
     *
     * @param tableReadConfig {@link TableReadConfig} to use
     * @param serializer the {@link ConfigSerializer} typed on the concrete implementation
     *
     */
    public AbstractMultiTableReadConfig(final TC tableReadConfig, final ConfigSerializer<S> serializer) {
        m_tableReadConfig = tableReadConfig;
        m_serializer = serializer;
    }

    /**
     * The code for this in the subclass should read {@code return this;}
     *
     * @return the object instance
     */
    protected abstract S getThis();

    @Override
    public final void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_serializer.loadInModel(getThis(), settings);
    }

    @Override
    public final void loadInDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_serializer.loadInDialog(getThis(), settings, specs);
    }

    @Override
    public final void saveInModel(final NodeSettingsWO settings) {
        m_serializer.saveInModel(getThis(), settings);
    }

    @Override
    public final void saveInDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_serializer.saveInDialog(getThis(), settings);
    }

    @Override
    public final void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_serializer.validate(getThis(), settings);
    }

    @Override
    public final TC getTableReadConfig() {
        return m_tableReadConfig;
    }

    @Override
    public final boolean failOnDifferingSpecs() {
        return m_failOnDifferingSpecs;
    }

    /**
     * Allows to set whether the node should fail if the specs differ in case multiple files are read.
     *
     * @param failOnDifferingSpecs {@code true} if the node should fail if multiple files are read and they have
     *            differing specs
     */
    public final void setFailOnDifferingSpecs(final boolean failOnDifferingSpecs) {
        m_failOnDifferingSpecs = failOnDifferingSpecs;
    }

    @Override
    public boolean skipEmptyColumns() {
        return m_skipEmptyColumns;
    }

    /**
     * Allows to set whether empty columns should be filtered out.
     *
     * @param skipEmptyColumns {@code true} if empty columns should be skipped
     */
    public void setSkipEmptyColumns(final boolean skipEmptyColumns) {
        m_skipEmptyColumns = skipEmptyColumns;
    }

    @Override
    public final TableSpecConfig<T> getTableSpecConfig() {
        return m_tableSpecConfig;
    }

    @Override
    public final boolean hasTableSpecConfig() {
        return m_tableSpecConfig != null;
    }

    @Override
    public final void setTableSpecConfig(final TableSpecConfig<T> config) {
        m_tableSpecConfig = config;
    }

    /**
     * @return the specMergeMode
     * @deprecated only used as fallback if there was no TableSpecConfig
     */
    @Override
    @Deprecated
    public final SpecMergeMode getSpecMergeMode() {
        return m_specMergeMode;
    }

    /**
     * Sets the {@link SpecMergeMode}.
     *
     * @param specMergeMode set the spec merge mode
     * @deprecated only used as fallback if there is no TableSpecConfig
     */
    @Deprecated
    public final void setSpecMergeMode(final SpecMergeMode specMergeMode) {
        m_specMergeMode = specMergeMode;
    }

    /**
     * Convenience getter for the {@link ReaderSpecificConfig}.
     *
     * @return the {@link ReaderSpecificConfig}
     */
    public final C getReaderSpecificConfig() {
        return m_tableReadConfig.getReaderSpecificConfig();
    }

}