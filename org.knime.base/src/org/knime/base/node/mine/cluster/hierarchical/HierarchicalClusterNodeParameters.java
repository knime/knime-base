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

package org.knime.base.node.mine.cluster.hierarchical;

import org.knime.base.node.mine.cluster.hierarchical.HierarchicalClusterNodeModel.Linkage;
import org.knime.base.node.mine.cluster.hierarchical.distfunctions.DistanceFunction.Names;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for Hierarchical Clustering.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
final class HierarchicalClusterNodeParameters implements NodeParameters {

    @Persist(configKey = HierarchicalClusterNodeModel.NRCLUSTERS_KEY)
    @Widget(title = "Number of output clusters", description = """
            Which level of the hierarchy to use for the output column.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_numberOfClusters = 3;

    @Persist(configKey = HierarchicalClusterNodeModel.DISTFUNCTION_KEY)
    @Widget(title = "Distance function", description = """
            Which distance measure to use for the distance between points.
            """)
    Names m_distanceFunction = Names.Euclidean;

    @Persist(configKey = HierarchicalClusterNodeModel.LINKAGETYPE_KEY)
    @Widget(title = "Linkage type", description = """
            Which method to use to measure the distance between points.
            """)
    Linkage m_linkageType = Linkage.SINGLE;

    @Persist(configKey = HierarchicalClusterNodeModel.USE_CACHE_KEY)
    @Widget(title = "Cache distances", description = """
            Caching the distances between the data points drastically improves performance especially for
            high-dimensional datasets. However, it needs much memory, so you can switch it off for large datasets.
            """)
    boolean m_cacheDistances = true;

    @Persist(configKey = HierarchicalClusterNodeModel.SELECTED_COLUMNS_KEY)
    @Modification(ColumnSelectionModification.class)
    LegacyStringFilter m_columnSelection = new LegacyStringFilter(new String[0], new String[0]);

    static final class ColumnSelectionModification extends LegacyStringFilter.LegacyStringFilterModification {

        ColumnSelectionModification() {
            super(false, """
                    Move the columns to include in the clustering into the include list. Only numeric columns are
                    considered for the hierarchical clustering algorithm. Other column types will be ignored.
                    """);
        }

    }

}
