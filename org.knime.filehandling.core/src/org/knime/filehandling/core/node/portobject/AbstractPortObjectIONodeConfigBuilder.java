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
 *   Feb 23, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.portobject;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * An abstract implementation of a Builder for classes extending {@link PortObjectIONodeConfig}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <B> the concrete type of builder
 */
public abstract class AbstractPortObjectIONodeConfigBuilder<B extends AbstractPortObjectIONodeConfigBuilder<B>> {

    private final NodeCreationConfiguration m_creationConfig;

    private SelectionMode m_selectionMode = SelectionMode.FILE;

    private Set<FSCategory> m_convenienceFS = EnumSet.allOf(FSCategory.class);

    private String[] m_fileSuffixes = new String[0];

    /**
     * Constructor.
     *
     * @param creationConfig of the node
     */
    protected AbstractPortObjectIONodeConfigBuilder(final NodeCreationConfiguration creationConfig) {
        m_creationConfig = creationConfig;
    }

    /**
     * Convenience method for type safety.
     *
     * @return this instance
     */
    protected abstract B getThis();

    /**
     * Sets the provided {@link SelectionMode} and returns this builder.
     *
     * @param selectionMode to use
     * @return this builder
     */
    public B withSelectionMode(final SelectionMode selectionMode) {
        m_selectionMode = CheckUtils.checkArgumentNotNull(selectionMode, "The selectionMode must not be null.");
        return getThis();
    }

    /**
     * Sets the provided <b>fileSuffixes</b> overwriting any previously set suffixes and returns this builder.
     *
     * @param fileSuffixes the file extensions (e.g. csv) to use
     * @return this builder
     * @throws IllegalArgumentException if <b>fileSuffixes</b> or any of its elements is {@code null}
     */
    public B withFileSuffixes(final String ... fileSuffixes) {
        checkNoNullsInArray(fileSuffixes, "fileSuffixes");
        m_fileSuffixes = fileSuffixes.clone();
        return getThis();
    }

    /**
     * Checks that neither the array nor any of its elements are {@code null}.
     *
     * @param array to check
     * @param arrayName name used to refer to the array
     */
    protected static void checkNoNullsInArray(final Object[] array, final String arrayName) {
        CheckUtils.checkArgument(Arrays.stream(//
            CheckUtils.checkArgumentNotNull(array, "The %s must not be null.", arrayName))//
            .noneMatch(Objects::isNull), "The %s must not contain null elements.", arrayName);
    }

    /**
     * Sets the provided {@link FSCategory convenience file systems} and returns
     *
     * @param convenienceFS the set of convenience file systems supported by the node
     * @return this builder
     */
    public B withConvenienceFS(final FSCategory... convenienceFS) {
        CheckUtils.checkArgumentNotNull(convenienceFS, "The convenienceFS must not be null.");
        final Set<FSCategory> convenienceFSSet = Arrays.stream(convenienceFS)//
            .filter(f -> f != FSCategory.CONNECTED)//
            .collect(Collectors.toSet());
        CheckUtils.checkArgument(!convenienceFSSet.isEmpty(), "At least convenience file system must be provided.");
        m_convenienceFS = convenienceFSSet;
        return getThis();
    }

    /**
     * Returns the {@link PortsConfiguration}.
     *
     * @return the {@link PortsConfiguration}
     */
    public PortsConfiguration getPortConfig() {
        return m_creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
    }

    /**
     * Returns the file suffixes/extensions.
     *
     * @return the file suffixes/extensions
     */
    public String[] getFileSuffixes() {
        return m_fileSuffixes.clone();
    }

    /**
     * Returns the {@link Set} of supported convenience file systems.
     *
     * @return the {@link Set} of supported convenience file systems
     */
    public Set<FSCategory> getConvenienceFS() {
        return Collections.unmodifiableSet(m_convenienceFS);
    }

    /**
     * Returns the {@link EnumConfig} for the {@link FilterMode FilterModes}.
     *
     * @return the {@link EnumConfig} for the {@link FilterMode FilterModes}
     */
    public EnumConfig<FilterMode> getFilterModeConfig() {
        return m_selectionMode.getFilterModeConfig();
    }

}
