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
 *   02.09.2014 (Marcel Hanser): created
 */
package org.knime.base.node.preproc.tablestructure;

import static org.knime.core.node.util.CheckUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.base.node.preproc.tablestructure.TableStructureValidatorNodeParameters.ColumnExistenceHandling;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

/**
 * The validation configuration object for the {@link TableStructureValidatorNodeModel}
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
final class TableStructureValidatorConfiguration {

    private TableStructureValidatorColConfiguration m_validatorColConfig;

    /**
     * Default Constructor, reference spec enforced.
     */
    TableStructureValidatorConfiguration() {
    }

    /**
     * Loads the configuration for the model.
     *
     * @param modelSettings the {@link TableStructureValidatorNodeParameters} to load the configuration from
     * @throws InvalidSettingsException if the settings are invalid
     */
    void loadConfigurationInModel(final TableStructureValidatorNodeParameters modelSettings)
        throws InvalidSettingsException {
        m_validatorColConfig = TableStructureValidatorColConfiguration.load(modelSettings);
    }

    /**
     * Returns the column names mapped to the corresponding {@link TableStructureValidatorColConfiguration}.
     *
     * @param in the input {@link DataTableSpec}
     * @param conflicts {@link TableStructureValidatorColConflicts}
     * @return the column names mapped to the corresponding {@link TableStructureValidatorColConfiguration}
     */
    @SuppressWarnings("java:S3047") // first two for loops need to stay separated
    Map<String, ConfigurationContainer> applyConfiguration(final DataTableSpec in,
        final TableStructureValidatorColConflicts conflicts) {
        Map<String, ConfigurationContainer> toReturn = new LinkedHashMap<>();
        Map<String, ConfigurationContainer> directMatches = new LinkedHashMap<>();
        List<ConfigurationContainer> caseInsensitiveMatches = new ArrayList<>();
        applyConfig(directMatches, caseInsensitiveMatches, m_validatorColConfig);

        // try first to find direct matches.
        for (DataColumnSpec s : in) {
            String name = s.getName();
            ConfigurationContainer directColConfig = directMatches.remove(name);

            if (directColConfig != null) {
                directColConfig.setInputColName(name);
                toReturn.put(name, directColConfig);
            }
        }

        // now check the case insensitive matches
        for (DataColumnSpec s : in) {
            String name = s.getName();
            if (!toReturn.containsKey(name)) {
                ConfigurationContainer caseInsensitiveMatch =
                    removeFirstMatchingCaseInsensitiveConfig(name, caseInsensitiveMatches);
                if (caseInsensitiveMatch != null) {
                    caseInsensitiveMatch.setInputColName(name);
                    toReturn.put(name, caseInsensitiveMatch);
                }
            }
        }

        // check for not satisfied configurations
        for (Entry<String, ConfigurationContainer> configs : directMatches.entrySet()) {
            if (!configs.getValue().isSatisfied()
                && ColumnExistenceHandling.FAIL == configs.getValue().getConfiguration().getColumnExistenceHandling()) {
                conflicts.addConflict(TableStructureValidatorColConflicts.missingColumn(configs.getKey()));
            }
        }

        return toReturn;
    }

    public Set<String> getConfiguredColumns() {
        Set<String> toReturn = new HashSet<>();
        toReturn.addAll(Arrays.asList(m_validatorColConfig.getNames()));
        return toReturn;
    }

    private static void applyConfig(final Map<String, ConfigurationContainer> directMatches,
        final List<ConfigurationContainer> caseInsensitiveMatches,
        final TableStructureValidatorColConfiguration config) {
        for (String name : config.getNames()) {
            directMatches.computeIfAbsent(name, matchedName -> {
                ConfigurationContainer configurationContainer = new ConfigurationContainer(matchedName, null, config);
                if (config.isCaseInsensitiveNameMatching()) {
                    caseInsensitiveMatches.add(configurationContainer);
                }
                return configurationContainer;
            });
        }
    }

    private static ConfigurationContainer removeFirstMatchingCaseInsensitiveConfig(final String name,
        final List<ConfigurationContainer> caseInsensitiveMatches) {
        Iterator<ConfigurationContainer> items = caseInsensitiveMatches.iterator();

        String upperName = name.toUpperCase(Locale.ROOT);

        while (items.hasNext()) {
            ConfigurationContainer next = items.next();
            if (next.isSatisfied()) {
                items.remove();
            }
            if (!next.isSatisfied() && upperName.equals(next.getRefColName().toUpperCase(Locale.ROOT))) {
                next.setInputColName(name);
                items.remove();
                return next;
            }
        }
        return null;
    }

    static final class ConfigurationContainer {

        private final String m_refColName;
        private final TableStructureValidatorColConfiguration m_configuration;
        private String m_inputColName;

        private ConfigurationContainer(final String refColName, final String inputColName,
            final TableStructureValidatorColConfiguration configuration) {
            this.m_refColName = checkNotNull(refColName);
            this.m_inputColName = inputColName;
            this.m_configuration = checkNotNull(configuration);
        }

        String getRefColName() {
            return m_refColName;
        }

        String getInputColName() {
            return m_inputColName;
        }

        void setInputColName(final String inputColName) {
            this.m_inputColName = inputColName;
        }

        boolean isSatisfied() {
            return m_inputColName != null;
        }

        TableStructureValidatorColConfiguration getConfiguration() {
            return m_configuration;
        }

    }

}
