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
 *   Apr 14, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.partition;

import org.knime.base.node.preproc.partition.PartitionNodeSettings.PartitionModification;
import org.knime.base.node.preproc.sample.AbstractSamplingNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup.Modification;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 * @since 5.5
 */
@SuppressWarnings("restriction")
@Modification(PartitionModification.class)
final class PartitionNodeSettings extends AbstractSamplingNodeSettings {

    PartitionNodeSettings() {
        super();
    }

    PartitionNodeSettings(final DefaultNodeSettingsContext context) {
        super(context);
    }

    static final class PartitionModification extends AbstractSamplingNodeSettings.SamplingModification {

        private static final String DESCRIPTION = "description";

        @Override
        public void modify(final WidgetGroupModifier group) {
            getCountModeWidgetModifier(group).withProperty("title", "First partition type")
                .withProperty(DESCRIPTION, """
                        Defines how the size of the first partition is specified: as a percentage of total rows \
                        (relative) or as an absolute number of rows.""").modify();
            getPercentageWidgetModifier(group)//
                .withProperty(DESCRIPTION, """
                        Specifies the percentage of rows from the input table to be included in the first \
                        partition. Must be between 0 and 100 (inclusive).""").modify();
            getRowCountWidgetModifier(group)//
                .withProperty(DESCRIPTION, """
                        Specifies the absolute number of rows to include in the first partition. If the input \
                        table contains fewer rows than specified, all rows are placed in the first table, and \
                        the second table will be empty.""").modify();
            getSamplingModeWidgetModifier(group)//
                .withProperty(DESCRIPTION, """
                        Determines how rows are selected for the first partition. Strategies include random, \
                        linear, stratified, and first rows (sequential).""").modify();
            getClassColumnWidgetModifier(group)//
                .withProperty(DESCRIPTION, """
                        Specifies the column whose value distribution should be preserved in stratified sampling. \
                        Ensures both output tables reflect the same distribution of values.""").modify();
        }
    }

}
