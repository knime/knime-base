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

package org.knime.base.node.mine.mds;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Common row limit related parameters for all MDS nodes.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 * @since 5.10
 */
@LoadDefaultsForAbsentFields
@Layout(MDSParametersUseRows.DataSection.class)
public final class MDSParametersUseRows implements NodeParameters {

    @Section(title = "Data")
    interface DataSection {
    }

    @Persist(configKey = MDSConfigKeys.CFGKEY_USE_ROWS)
    @Widget(title = "Use all rows", description = """
            If enabled, all rows in the input table will be used for the MDS computation.
            If disabled, only the specified number of rows will be used.
            """)
    @ValueReference(UseAllRows.class)
    boolean m_useAllRows = MDSNodeModel.DEF_USE_MAX_ROWS;

    @Persist(configKey = MDSConfigKeys.CFGKEY_ROWS)
    @Widget(title = "Number of rows to use", description = """
            Specifies the number of rows to apply the MDS on.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class, stepSize = 100)
    @Effect(predicate = UseAllRows.class, type = EffectType.HIDE)
    int m_numberOfRows = MDSNodeModel.DEF_NO_ROWS;

    private static final class UseAllRows implements BooleanReference {
    }

}
