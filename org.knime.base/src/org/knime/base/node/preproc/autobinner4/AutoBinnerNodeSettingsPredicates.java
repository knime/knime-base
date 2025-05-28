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
 *   Jul 14, 2025 (david): created
 */
package org.knime.base.node.preproc.autobinner4;

import org.knime.base.node.preproc.autobinner4.AutoBinnerNodeSettings.BinNamesRef;
import org.knime.base.node.preproc.autobinner4.AutoBinnerNodeSettings.BinningTypeRef;
import org.knime.base.node.preproc.autobinner4.AutoBinnerNodeSettings.FixLowerBoundRef;
import org.knime.base.node.preproc.autobinner4.AutoBinnerNodeSettings.FixUpperBoundRef;
import org.knime.base.node.preproc.autobinner4.AutoBinnerNodeSettings.NumberFormat;
import org.knime.base.node.preproc.autobinner4.AutoBinnerNodeSettingsEnums.BinNaming;
import org.knime.base.node.preproc.autobinner4.AutoBinnerNodeSettingsEnums.BinningType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;

/**
 * We have such a huge number of predicates for the AutoBinner node settings that it makes sense to group them in a
 * separate class.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class AutoBinnerNodeSettingsPredicates {

    private AutoBinnerNodeSettingsPredicates() {
        // Utility class
    }

    static final class NumberOfBinsShouldBeShown implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i //
                .getEnum(BinningTypeRef.class) //
                .isOneOf(BinningType.CUSTOM_CUTOFFS, BinningType.CUSTOM_QUANTILES) //
                .negate();
        }
    }

    static final class BinningTypeIsCustomCutoffs implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i //
                .getEnum(BinningTypeRef.class) //
                .isOneOf(BinningType.CUSTOM_CUTOFFS);
        }
    }

    static final class BinningTypeIsCustomQuantiles implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i //
                .getEnum(BinningTypeRef.class) //
                .isOneOf(BinningType.CUSTOM_QUANTILES);
        }
    }

    static final class BinningTypeIsNotCustomCutoffs implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i //
                .getPredicate(BinningTypeIsCustomCutoffs.class) //
                .negate();
        }
    }

    static final class ShouldShowFixedUpperBoundField implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i //
                .getPredicate(BinningTypeIsNotCustomCutoffs.class) //
                .and(i.getBoolean(FixUpperBoundRef.class).isTrue());
        }
    }

    static final class ShouldShowFixedLowerBoundField implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i //
                .getPredicate(BinningTypeIsNotCustomCutoffs.class) //
                .and(i.getBoolean(FixLowerBoundRef.class).isTrue());
        }
    }

    static final class ShouldShowUpperOutlierName implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i //
                .getBoolean(FixUpperBoundRef.class).isTrue() //
                .or(i.getEnum(BinningTypeRef.class).isOneOf(BinningType.CUSTOM_CUTOFFS,
                    BinningType.CUSTOM_QUANTILES));
        }
    }

    static final class ShouldShowLowerOutlierName implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i //
                .getBoolean(FixLowerBoundRef.class).isTrue() //
                .or(i.getEnum(BinningTypeRef.class).isOneOf(BinningType.CUSTOM_CUTOFFS,
                    BinningType.CUSTOM_QUANTILES));
        }
    }

    static final class BinNamesIsNumbered implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i //
                .getEnum(BinNamesRef.class) //
                .isOneOf(BinNaming.NUMBERED);
        }
    }

    static final class ShouldDisplayCustomNumberFormatSettings implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i //
                .getPredicate(BinNamesIsNumbered.class).negate() //
                .and(i.getEnum(NumberFormat.Ref.class).isOneOf(NumberFormat.CUSTOM));
        }
    }
}
