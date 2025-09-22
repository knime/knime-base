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
 *   Jan 26, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.columnheaderextract;

import static org.knime.base.node.preproc.columnheaderextract.ColumnHeaderExtractorNodeModel.CFG_COLTYPE;
import static org.knime.base.node.preproc.columnheaderextract.ColumnHeaderExtractorNodeModel.CFG_ONE_BASED_INDEXING;
import static org.knime.base.node.preproc.columnheaderextract.ColumnHeaderExtractorNodeModel.CFG_TRANSPOSE_COL_HEADER;

import org.knime.base.node.preproc.columnheaderextract.ColumnHeaderExtractorNodeModel.ColType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Settings of the Column Header Extractor dialog. Not used by the NodeModel, yet. If it ever is please double check
 * backwards compatibility.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 */
public final class ColumnHeaderExtractorNodeSettings implements NodeParameters {

    interface DontReplaceColHeader {
    }

    @Persistor(OutputFormatPersistor.class)
    @Widget(title = "Output format for column names",
        description = "The format in which the first output table provides the extracted column names:" //
            + "<ul>"//
            + "<li><b>Row</b>: The column names are output as a single row with a column per name.</li>"//
            + "<li><b>Column</b>: The column names are output as a single column with a row per name.</li>"//
            + "</ul>")
    @ValueSwitchWidget
    OutputFormat m_transposeColHeader;

    static final class ReplaceColHeader implements BooleanReference {

    }

    @Widget(title = "Generate new column names",
        description = "If selected, the column names of both output tables will be replaced "//
            + "with automatically generated names by combining the prefix provided below with the corresponding "//
            + "column number (e.g. \"Column 1\", \"Column 2\", and so on). "//
            + "<br><br>Otherwise, the original column names will be used.")
    @ValueReference(ReplaceColHeader.class)
    boolean m_replaceColHeader;

    @Persist(configKey = "unifyHeaderPrefix")
    @Widget(title = "Prefix", description = "Prefix to use when generating new column names.")
    @Effect(type = EffectType.SHOW, predicate = ReplaceColHeader.class)
    String m_unifyHeaderPrefix;

    @Persistor(ColTypePersistor.class)
    @Widget(title = "Restrain column types", description = "Select the type of the columns to extract the names from:"//
        + "<ul>"//
        + "<li><b>All</b>: All columns are processed.</li>"//
        + "<li><b>String</b>: Only string-compatible columns are processed, "//
        + "this includes e.g. XML columns.</li>"//
        + "<li><b>Integer</b>: Only integer-compatible columns are processed.</li>"//
        + "<li><b>Double</b>: Only double-compatible columns are processed. "//
        + "This includes integer and long columns.</li>"//
        + "</ul>", advanced = true)
    @ValueSwitchWidget
    ColType m_colTypeFilter = ColType.ALL;

    // Hidden field for backward compatibility - controls indexing behavior
    @Persistor(UseOneBasedIndexingPersistor.class)
    boolean m_useOneBasedIndexing = true;

    enum OutputFormat {
            @Label("Row")
            ROW, //
            @Label("Column")
            COLUMN;
    }

    private static final class OutputFormatPersistor implements NodeParametersPersistor<OutputFormat> {

        @Override
        public OutputFormat load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(CFG_TRANSPOSE_COL_HEADER, false) ? OutputFormat.COLUMN : OutputFormat.ROW;
        }

        @Override
        public void save(final OutputFormat obj, final NodeSettingsWO settings) {
            settings.addBoolean(CFG_TRANSPOSE_COL_HEADER, obj == OutputFormat.COLUMN);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_TRANSPOSE_COL_HEADER}};
        }
    }

    private static final class ColTypePersistor implements NodeParametersPersistor<ColType> {

        @Override
        public ColType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return ColType.fromDisplayString(settings.getString(CFG_COLTYPE));
        }

        @Override
        public void save(final ColType obj, final NodeSettingsWO settings) {
            settings.addString(CFG_COLTYPE, obj.displayString());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_COLTYPE}};
        }
    }

    private static final class UseOneBasedIndexingPersistor implements NodeParametersPersistor<Boolean> {

        @Override
        public Boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
            // added in 5.0. Old workflows should use 0-based indexing, new workflows 1-based
            return settings.getBoolean(CFG_ONE_BASED_INDEXING, false);
        }

        @Override
        public void save(final Boolean obj, final NodeSettingsWO settings) {
            settings.addBoolean(CFG_ONE_BASED_INDEXING, obj);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_ONE_BASED_INDEXING}};
        }
    }
}