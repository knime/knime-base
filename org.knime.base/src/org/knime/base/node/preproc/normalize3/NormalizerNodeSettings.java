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
 *   Jan 22, 2024 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.normalize3;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.DoubleColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class NormalizerNodeSettings implements DefaultNodeSettings {

    @Persist(configKey = "data-column-filter", customPersistor = LegacyColumnFilterPersistor.class)
    @Widget(title = "Number columns", description = "Select the numerical columns to normalize.")
    @ChoicesWidget(choices = DoubleColumnChoicesProvider.class)
    ColumnFilter m_dataColumnFilterConfig = new ColumnFilter().withIncludeUnknownColumns();

    interface NormalizerModeRef extends Reference<NormalizerMode> {
    }

    static final class NormalizerByMinMax implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(NormalizerModeRef.class).isOneOf(NormalizerMode.MINMAX);
        }
    }

    @Persist(configKey = "mode")
    @ValueSwitchWidget
    @Widget(title = "Normalization method", description = "The normalization method to use.")
    @ValueReference(NormalizerModeRef.class)
    NormalizerMode m_mode = NormalizerMode.MINMAX;

    @Persist(configKey = "new-min")
    @Effect(predicate = NormalizerByMinMax.class, type = EffectType.SHOW)
    @Widget(title = "Minimum",
        description = "Specifies the new minimum for the normalized columns. Only active for min-max normalization.")
    double m_min = 0;//NOSONAR make it explicit

    @Persist(configKey = "new-max")
    @Effect(predicate = NormalizerByMinMax.class, type = EffectType.SHOW)
    @Widget(title = "Maximum",
        description = "Specifies the new maximum for the normalized columns. Only active for min-max normalization.")
    double m_max = 1;

    /**
     * Normalization Mode.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    enum NormalizerMode {
            /**
             * Min-Max normalization.
             */
            @Label(value = "Min-max", description = """
                    Linear transformation of all values such that the minimum and maximum in each column correspond
                    to the values set below.
                    """)
            MINMAX,

            /**
             * Z-Score.
             */
            @Label(value = "Z-score", description = """
                    Linear transformation such that the values in each column are Gaussian-(0,1)-distributed, i.e.
                    mean is 0.0 and standard deviation is 1.0.
                    """)
            Z_SCORE,
            /**
             * Decimal Scaling.
             */
            @Label(value = "Decimal scaling", description = """
                    The maximum value in a column (both positive and negative) is divided j-times by 10 until its
                    absolute value is smaller or equal to 1. All values in the column are then divided by 10 to the
                    power of j.
                    """)
            DECIMALSCALING;
    }
}
