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
 *   Feb 6, 2025 (david): created
 */
package org.knime.base.node.preproc.filter.rowref;

import java.util.Optional;

import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelColumnNamePersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;

/**
 * Common utilities for settings for row splitter nodes.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class SettingsUtils {

    /** Value saved in settings to indicate we should include selected rows. */
    static final String INCLUDE = "Include rows from reference table";

    /** Value saved in settings to indicate we should exclude selected rows. */
    static final String EXCLUDE = "Exclude rows from reference table";

    private SettingsUtils() {
        // no instantiation
    }

    abstract static class AllColumnChoices implements ChoicesProvider {

        private final int m_portIdx;

        AllColumnChoices(final int portIdx) {
            m_portIdx = portIdx;
        }

        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            // This check is needed for the settings tests, which creates a dummy node
            // with no ports.
            Optional<DataTableSpec> specs = context.getDataTableSpecs().length > 0 //
                ? context.getDataTableSpec(m_portIdx) //
                : Optional.empty();

            return specs //
                .map(DataTableSpec::getColumnNames) //
                .orElse(new String[0]);
        }
    }

    static final class DataColumnChoices extends AllColumnChoices {
        public DataColumnChoices() {
            super(0);
        }
    }

    static final class ReferenceColumnChoices extends AllColumnChoices {
        public ReferenceColumnChoices() {
            super(1);
        }
    }

    static final class DataColumnPersistor extends SettingsModelColumnNamePersistor {
        DataColumnPersistor() {
            super("dataTableColumn");
        }
    }

    static final class ReferenceColumnPersistor extends SettingsModelColumnNamePersistor {
        ReferenceColumnPersistor() {
            super("referenceTableColumn");
        }
    }

    static final class UpdateDomainsPersistor extends SettingsModelBooleanPersistor {

        private static final String KEY_UPDATE_DOMAINS = "updateDomains";

        UpdateDomainsPersistor() {
            super(KEY_UPDATE_DOMAINS);
        }
    }
}
