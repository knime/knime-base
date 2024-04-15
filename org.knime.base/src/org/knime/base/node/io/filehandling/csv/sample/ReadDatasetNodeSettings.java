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
 *   Dec 11, 2023 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.sample;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class ReadDatasetNodeSettings implements DefaultNodeSettings {

    enum Dataset {
            @Label(value = "Worldbank countries and regions", description = """
                    Data taken from the 1960-2022 World Development Indicators dataset as released by the World Bank at
                    <a href="https://datacatalog.worldbank.org/search/dataset/0037712/World-Development-Indicators">
                    https://datacatalog.worldbank.org/search/dataset/0037712/World-Development-Indicators</a> under the
                    <a href="https://creativecommons.org/licenses/by/4.0/deed.en">Creative Commons Attribution 4.0
                    license</a>. The data provided by the node is a copy of the table WDICountry.csv of this dataset.
                    """)
            WORLDBANK_COUNTRIES("WDICountry.table"), //
            @Label(value = "Worldbank population data", description = """
                    The 2022 Population Ranking table as released by the World Bank at
                    <a href="https://datacatalog.worldbank.org/search/dataset/0038126/Population-ranking">
                    https://datacatalog.worldbank.org/search/dataset/0038126/Population-ranking</a> under the
                    <a href="https://creativecommons.org/licenses/by/4.0/deed.en">Creative Commons Attribution 4.0
                    license</a>. Note that this dataset is raw and needs to undergo cleaning before use.
                    """)
            WORLDBANK_POPULATION("POP.table");

        private final String m_path;

        String getPath() {
            return m_path;
        }

        Dataset(final String path) {
            m_path = path;
        }
    }

    @Widget(title = "Dataset", description = "Select one of several pre-defined datasets.")
    Dataset m_dataset = Dataset.WORLDBANK_COUNTRIES;
}
