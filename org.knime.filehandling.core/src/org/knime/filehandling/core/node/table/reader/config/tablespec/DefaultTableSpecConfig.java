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
 *   Nov 13, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.data.DataTableSpec;
import org.knime.filehandling.core.node.table.reader.ImmutableTableTransformation;
import org.knime.filehandling.core.node.table.reader.SourceGroup;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformationUtils;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * Configuration storing all the information needed to create a {@link DataTableSpec}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify external data types
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class DefaultTableSpecConfig<T> implements TableSpecConfig<T> {

    private final ConfigID m_configID;

    private final String m_sourceGroupID;

    private final Map<String, TypedReaderTableSpec<T>> m_individualSpecs;

    private final TableTransformation<T> m_tableTransformation;

    <I> DefaultTableSpecConfig(final String sourceGroupID, final ConfigID configID,
        final Map<I, TypedReaderTableSpec<T>> individualSpecs, final TableTransformation<T> tableTransformation) {
        m_sourceGroupID = sourceGroupID;
        m_configID = configID;
        m_individualSpecs = individualSpecs.entrySet().stream()//
            .collect(Collectors.toMap(//
                e -> e.getKey().toString()//
                , Map.Entry::getValue//
                , (x, y) -> y//
                , LinkedHashMap::new));
        m_tableTransformation = ImmutableTableTransformation.copy(tableTransformation);
    }

    DefaultTableSpecConfig(final String sourceGroupID, final ConfigID configID, final String[] items,
        final Collection<TypedReaderTableSpec<T>> individualSpecs,
        final ImmutableTableTransformation<T> tableTransformation) {
        m_configID = configID;
        m_sourceGroupID = sourceGroupID;
        m_individualSpecs = createIndividualSpecsMap(items, individualSpecs);
        m_tableTransformation = tableTransformation;
    }

    private static <T> LinkedHashMap<String, TypedReaderTableSpec<T>> createIndividualSpecsMap(final String[] items,
        final Collection<TypedReaderTableSpec<T>> individualSpecs) {
        final LinkedHashMap<String, TypedReaderTableSpec<T>> map = new LinkedHashMap<>();
        int i = 0;
        for (TypedReaderTableSpec<T> spec : individualSpecs) {
            map.put(items[i], spec);
            i++;
        }
        return map;
    }

    /**
     * Creates a {@link DefaultTableSpecConfig} that corresponds to the provided parameters.
     *
     * @param sourceGroupID the ID of the {@link SourceGroup} that the individualSpecs are based on
     * @param configID the root item
     * @param individualSpecs a map from the path/file to its individual {@link ReaderTableSpec}
     * @param tableTransformation defines the transformation (type-mapping, filtering, renaming and reordering) of the
     *            output spec
     *
     * @param <T> the type used to identify external types
     * @return a {@link DefaultTableSpecConfig} for the provided parameters
     */
    public static <I, T> TableSpecConfig<T> createFromTransformationModel(final String sourceGroupID,
        final ConfigID configID, final Map<I, TypedReaderTableSpec<T>> individualSpecs,
        final TableTransformation<T> tableTransformation) {
        return new DefaultTableSpecConfig<>(sourceGroupID, configID, individualSpecs, tableTransformation);
    }

    /**
     * Returns the raw {@link TypedReaderTableSpec} before type mapping, filtering, reordering or renaming.
     *
     * @return the raw spec
     */
    @Override
    public RawSpec<T> getRawSpec() {
        return m_tableTransformation.getRawSpec();
    }

    @Override
    public TableTransformation<T> getTableTransformation() {
        return m_tableTransformation;
    }

    @Override
    public boolean isConfiguredWith(final ConfigID configID, final String sourceGroupID) {
        return m_sourceGroupID.equals(sourceGroupID) && m_configID.equals(configID);
    }

    @Override
    public boolean isConfiguredWith(final ConfigID id, final SourceGroup<String> sourceGroup) {
        return isConfiguredWith(id, sourceGroup.getID()) //
            && m_individualSpecs.size() == sourceGroup.size() //
            && sourceGroup.stream()//
                .allMatch(m_individualSpecs::containsKey);
    }

    @Override
    public DataTableSpec getDataTableSpec() {
        return TableTransformationUtils.toDataTableSpec(getTableTransformation());
    }

    @Override
    public List<String> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(m_individualSpecs.keySet()));
    }

    @Override
    public TypedReaderTableSpec<T> getSpec(final String item) {
        return m_individualSpecs.get(item);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + m_individualSpecs.hashCode();
        result = prime * result + m_configID.hashCode();
        result = prime * result + m_sourceGroupID.hashCode();
        result = prime * result + m_tableTransformation.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            @SuppressWarnings("unchecked")
            DefaultTableSpecConfig<T> other = (DefaultTableSpecConfig<T>)obj;
            return m_sourceGroupID.equals(other.m_sourceGroupID) //
                && m_configID.equals(other.m_configID)//
                && m_tableTransformation.equals(other.m_tableTransformation)
                && m_individualSpecs.equals(other.m_individualSpecs);
        }
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder("[")//
            .append("Root item: ")//
            .append(m_configID)//
            .append("\nIndividual specs: ")//
            .append(m_individualSpecs.entrySet().stream()//
                .map(e -> e.getKey() + ": " + e.getValue())//
                .collect(joining(", ", "[", "]")))//
            .append("\nTableTransformation: ").append(m_tableTransformation).append("]\n").toString();
    }

    // Getters for DefaultTableSpecConfigSerializer

    @Override
    public ConfigID getConfigID() {
        return m_configID;
    }

    @Override
    public String getSourceGroupID() {
        return m_sourceGroupID;
    }

}