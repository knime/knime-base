/*
 * ------------------------------------------------------------------------
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
package org.knime.base.node.preproc.targetshuffling;

import org.knime.core.node.InvalidSettingsException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Settings for the Target Shuffling node (Y-Scrambling) modern UI dialog.
 *
 * This node randomly permutates the values of one column in the table, which is useful
 * for creating negative controls in machine learning scenarios by breaking the relationship
 * between features and target variables.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Tim-Oliver Buchholz, University of Konstanz
 */
@SuppressWarnings("restriction")
@Layout(TargetShufflingNodeSettings2.RandomizationSection.class)
public final class TargetShufflingNodeSettings2 implements NodeParameters {

    /**
     * Constructor for persistence and conversion from JSON.
     */
    public TargetShufflingNodeSettings2() {
    }

    /**
     * Constructor with context input for dynamic choices.
     *
     * @param context the node parameters input context
     */
    TargetShufflingNodeSettings2(final NodeParametersInput context) {
        this();
    }

    @Section(title = "Column Selection")
    interface ColumnSelectionSection {
    }

    @Section(title = "Randomization Options")
    @After(ColumnSelectionSection.class)
    interface RandomizationSection {
    }

    /**
     * The column to shuffle.
     */
    @Persist(configKey = "columnName")
    @Widget(title = "Column to shuffle",
            description = "Select the column whose values should be randomly shuffled. This breaks the relationship "
                + "between this column and other columns in the table, which is useful for creating negative controls "
                + "in machine learning experiments.")
    @ChoicesProvider(AllColumnsProvider.class)
    @Layout(ColumnSelectionSection.class)
    String columnName;

    /**
     * Whether to use a fixed seed for reproducible shuffling.
     */
    @Persist(configKey = "useSeed")
    @Widget(title = "Use fixed seed for reproducible results",
            description = "When enabled, the shuffling will use a fixed seed value, making the randomization "
                + "reproducible across multiple executions. When disabled, each execution will produce different "
                + "random shuffling results.")
    @Layout(RandomizationSection.class)
    boolean useSeed = false;

    /**
     * The seed value for reproducible shuffling.
     */
    @Persist(configKey = "seed")
    @Widget(title = "Random seed",
            description = "The seed value used for random number generation. Use the same seed to get identical "
                + "shuffling results across multiple executions. You can enter a custom value or use the random "
                + "seed generation button in the legacy dialog.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(RandomizationSection.class)
    long seed = 0L;

    /**
     * Provider for all columns that can be shuffled.
     */
//    private static final class AllColumnsProvider extends CompatibleColumnsProvider {
//
//        @Override
//        protected Class<? extends DataValue>[] getCompatibleDataValueClasses() {
//            // Accept all data types since any column can be shuffled
//            return new Class[]{DataValue.class};
//        }
//    }

    /**
     * Validates the settings.
     */
    @Override
    public void validate() throws InvalidSettingsException {
        if (columnName == null || columnName.trim().isEmpty()) {
            throw new InvalidSettingsException("No column selected for shuffling");
        }
    }
}
