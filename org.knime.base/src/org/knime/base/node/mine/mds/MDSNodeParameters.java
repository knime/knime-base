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

import java.util.List;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter.ColumnBasedExclListProvider;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter.ColumnBasedInclListProvider;
import org.knime.node.parameters.widget.choices.ChoicesStateProvider;
import org.knime.node.parameters.widget.choices.TypedStringChoice;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * The node parameters for the MDS node.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class MDSNodeParameters implements NodeParameters {

    @PersistWithin.PersistEmbedded
    MDSParametersCommonMDS m_mdsParameters = new MDSParametersCommonMDS();

    @PersistWithin.PersistEmbedded
    MDSParameterDistanceMetric m_distanceMetric = new MDSParameterDistanceMetric();

    @PersistWithin.PersistEmbedded
    MDSParametersUseRows m_common = new MDSParametersUseRows();

    @Persist(configKey = MDSConfigKeys.CFGKEY_COLS)
    @Modification(DataToProjectModification.class)
    LegacyStringFilter m_columnsToProject = new LegacyStringFilter(new String[0], new String[0]);

    private static final class DataToProjectModification extends LegacyStringFilter.LegacyStringFilterModification {
        DataToProjectModification() {
            super(true, "Columns to project", "Specifies the columns to use by the mapping.", null, null,
                InclListChoicesProvider.class, InclListProvider.class, ExclListProvider.class);
        }

        private static final class InclListChoicesProvider extends CompatibleColumnsProvider {
            InclListChoicesProvider() {
                super(List.of(DoubleValue.class, FuzzyIntervalValue.class));
            }
        }

        private static final class InclListProvider extends ColumnBasedInclListProvider {
            @Override
            public Class<? extends ChoicesStateProvider<TypedStringChoice>> getChoicesProviderClass() {
                return InclListChoicesProvider.class;
            }
        }

        private static final class ExclListProvider extends ColumnBasedExclListProvider {
            @Override
            public Class<? extends ChoicesStateProvider<TypedStringChoice>> getChoicesProviderClass() {
                return InclListChoicesProvider.class;
            }
        }

    }

}
