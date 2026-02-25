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
 * ------------------------------------------------------------------------
 */
    
package org.knime.base.node.preproc.manipulator;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;

/**
 * Node parameters for Table Manipulator.
 * 
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
class TableManipulatorNodeParameters implements NodeParameters {

    private static final String CFG_SETTINGS_TAB = "settings";

    private static final String CFG_HAS_ROW_ID = "has_row_id";

    private static final String CFG_PREPEND_TABLE_IDX_TO_ROWID = "prepend_table_index_to_row_id";

    // ----- RowID handling -----

    interface UseExistingRowIDRef extends ParameterReference<Boolean> {
    }

    static final class UseExistingRowIDPersistor implements NodeParametersPersistor<Boolean> {
        @Override
        public Boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getNodeSettings(CFG_SETTINGS_TAB).getBoolean(CFG_HAS_ROW_ID);
        }

        @Override
        public void save(final Boolean value, final NodeSettingsWO settings) {
            settings.addNodeSettings(CFG_SETTINGS_TAB).addBoolean(CFG_HAS_ROW_ID, value);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_SETTINGS_TAB, CFG_HAS_ROW_ID}};
        }
    }

    static final class PrependTableIndexPersistor implements NodeParametersPersistor<Boolean> {
        @Override
        public Boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getNodeSettings(CFG_SETTINGS_TAB).getBoolean(CFG_PREPEND_TABLE_IDX_TO_ROWID);
        }

        @Override
        public void save(final Boolean value, final NodeSettingsWO settings) {
            settings.addNodeSettings(CFG_SETTINGS_TAB).addBoolean(CFG_PREPEND_TABLE_IDX_TO_ROWID, value);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_SETTINGS_TAB, CFG_PREPEND_TABLE_IDX_TO_ROWID}};
        }
    }

    static final class IsUseExistingRowID implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UseExistingRowIDRef.class).isTrue();
        }
    }

    @Widget(title = "Use existing RowID",
        description = "If checked, the RowIDs from the input tables are used for the output table. "
            + "If unchecked, a new RowID is generated following the schema \"Row0\", \"Row1\", and so on.")
    @Persistor(UseExistingRowIDPersistor.class)
    @ValueReference(UseExistingRowIDRef.class)
    boolean m_useExistingRowID = true;

    @Widget(title = "Prepend table index to RowID",
        description = "Only enabled if the existing RowIDs are used. If checked, a prefix is prepended "
            + "to the RowIDs that indicates which table the row came from. "
            + "The format of the prefix is \"Table_0_\", \"Table_1_\", and so on.")
    @Persistor(PrependTableIndexPersistor.class)
    @Effect(predicate = IsUseExistingRowID.class, type = EffectType.SHOW)
    boolean m_prependTableIndexToRowID = false;

    /**
     * TODO: Implement the actual parameters based on the dialog configuration
     * Use the following descriptions extracted from the node factory xml for
     * the description tag in @Widget annotations of the respective parameters.
     * IMPORTANT: Due to previous decoupling of node description and actually used 
     * parameters, the following options might be 
     * a) incomplete -> Sometimes there exist tooltips in the dialog however
     * b) irrelevant/removed -> interesting to investigate why
     * c) of slightly different structure (e.g. previously, descriptions of parameters inside array layouts might have been listed top-level)
     * 
     * Option: Use existing RowID
     * Check this box if the RowIDs from the input tables should be used for the output tables. If
     * unchecked, a new RowID is generated. The generated RowID follows the schema "Row0", "Row1" and so
     * on.
     *
     * Option: Prepend table index to RowID
     * Only enabled if the existing RowIDs are used. If checked, a prefix is prepended to the RowIDs
     * that indicates which table the row came from. The format of the prefix is "Table_0_", "Table_1_"
     * and so on.
     *
     * Option: Transformations
     * This option displays every column as a row in a table that allows modifying the structure of the
     * output table. It supports reordering, filtering and renaming columns. It is also possible to
     * change the type of the columns. Reordering is done via drag-and-drop. Just drag a column to the
     * position it should have in the output table. Whether and where to add unknown columns during
     * execution is specified via the special row &lt;any unknown new column&gt;.
     *
     * Option: Reset order
     * Resets the order of columns to the order in the input input tables.
     *
     * Option: Reset filter
     * Clicking this button will reset the filters i.e. all columns will be included.
     *
     * Option: Reset names
     * Resets the names to the names that are read from file or created if the file/folder doesn't
     * contain column names.
     *
     * Option: Reset types
     * Resets the output types to the default types guessed from the input table specification.
     *
     * Option: Reset all
     * Resets all transformations.
     *
     * Option: Enforce types
     * Controls how columns whose type changes are dealt with. If selected, we attempt to map to the
     * KNIME type you configured and fail if that's not possible. If unselected, the KNIME type
     * corresponding to the new type is used.
     *
     * Option: Take columns from
     * Only enabled in several input tables are available. Specifies which set of columns are considered
     * for the output table. <ul> <li>Union: Any column that is part of any input table is considered.
     * If an input table is missing a column, it's filled up with missing values. </li>
     * <li>Intersection: Only columns that appear in all input tables are considered for the output
     * table.</li> </ul> <b>NOTE:</b> <p> This setting has special implications if you are changing the
     * input table without reconfiguring the node. If Intersection is selected any column that moves
     * into the intersection during execution will be considered to be new, even if it was previously
     * part of the union of columns. </p> <p> It is also important to note that the transformation
     * matching during execution is based on name. That means if there was a column [A, Integer] during
     * configuration in the dialog and this column becomes [A, String] during execution, then the stored
     * transformation is applied to it. For filtering, ordering and renaming, this is straight forward.
     * For type mapping the following is done: If there is an alternative converter to the specified
     * KNIME type, then this converter is used, otherwise we default to the default KNIME type for the
     * new type. In our example we might have specified that [A, Integer] should be mapped to Long. For
     * the changed column [A, String] there is no converter to Long, so we default back to String and A
     * becomes a String column in the output table. </p>
     * */
    
}
