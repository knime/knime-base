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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.extractcontextprop;

import java.util.Arrays;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.ContextProperties;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;

/**
 * Settings for the {@link ReadContextProperty2NodeFactory Extract Context Properties} node.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
@Persistor(ReadContextProperty2NodeSettings.SettingsPersistor.class)
final class ReadContextProperty2NodeSettings implements NodeParameters {

    private static final String[] ALL_PROP_KEYS = ContextProperties.getContextProperties().toArray(String[]::new);

    @Widget(title = "Selected Properties",
        description = "Properties that should be extracted from the workflow context.")
    @ChoicesProvider(ContextPropsChoicesProvider.class)
    @Effect(predicate = IsExtractAllProps.class, type = EffectType.DISABLE)
    String[] m_selectedProps;

    interface IsExtractAllPropsRef extends ParameterReference<Boolean> {
    }

    static final class IsExtractAllProps implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(IsExtractAllPropsRef.class).isTrue();
        }
    }

    @Widget(title = "Select All Properties",
        description = "Extracts all available properties, may export additional "
            + "properties in the future if any are added.")
    @ValueReference(IsExtractAllPropsRef.class)
    boolean m_isExtractAllProps = true;

    /** Provider for the extractable properties' names. */
    private static final class ContextPropsChoicesProvider implements StringChoicesProvider {
        @Override
        public List<String> choices(final NodeParametersInput context) {
            return Arrays.asList(ALL_PROP_KEYS);
        }
    }

    /** Custom persistor for backwards compatibility. */
    private static final class SettingsPersistor implements NodeParametersPersistor<ReadContextProperty2NodeSettings> {

        static final String IS_EXTRACT_ALL_PROPS = "isExtractAllProps";

        static final String SELECTED_PROPS = "selectedProps";

        @Override
        public ReadContextProperty2NodeSettings load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var loaded = new ReadContextProperty2NodeSettings();
            loaded.m_isExtractAllProps = settings.getBoolean(IS_EXTRACT_ALL_PROPS);
            if (loaded.m_isExtractAllProps) {
                loaded.m_selectedProps = ALL_PROP_KEYS;
            } else {
                final var selected = settings.getStringArray(SELECTED_PROPS);
                loaded.m_selectedProps = selected == null ? new String[0] : selected;
            }
            return loaded;
        }

        @Override
        public void save(final ReadContextProperty2NodeSettings selectedProps, final NodeSettingsWO settings) {
            settings.addBoolean(IS_EXTRACT_ALL_PROPS, selectedProps.m_isExtractAllProps);
            if (!selectedProps.m_isExtractAllProps) {
                settings.addStringArray(SELECTED_PROPS, selectedProps.m_selectedProps);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{IS_EXTRACT_ALL_PROPS}, {SELECTED_PROPS}};
        }
    }
}
