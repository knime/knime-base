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
 *   May 12, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location.variable;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableTypeExtension;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.data.location.internal.FSLocationUtils;

/**
 * Singleton type of {@link FlowVariable} for handling {@link FSLocationSpec} values. The singleton instance is
 * accessible via the {@link FSLocationSpecVariableType#INSTANCE} field.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class FSLocationSpecVariableType extends VariableType<FSLocationSpec> {

    /**
     * The singleton instance of the {@link FSLocationSpecVariableType}.
     */
        public static final FSLocationSpecVariableType INSTANCE = new FSLocationSpecVariableType();

    private FSLocationSpecVariableType() {
        // singleton
    }

    private static final class FSLocationSpecValue extends VariableValue<FSLocationSpec> {

        private FSLocationSpecValue(final FSLocationSpec value) {
            super(INSTANCE, value);
        }

        @Override
        public FSLocationSpec get() {
            return super.get();
        }

    }

    @Override
    protected Class<FSLocationSpec> getSimpleType() {
        return FSLocationSpec.class;
    }

    @Override
    protected VariableValue<FSLocationSpec> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
        return new FSLocationSpecValue(FSLocationUtils.loadFSLocationSpec(settings));
    }

    @Override
    protected void saveValue(final NodeSettingsWO settings, final VariableValue<FSLocationSpec> v) {
        FSLocationSpecValue value = (FSLocationSpecValue)v;
        FSLocationUtils.saveFSLocationSpec(value.get(), settings);
    }

    @Override
    protected VariableValue<FSLocationSpec> newValue(final FSLocationSpec v) {
        return new FSLocationSpecValue(v);
    }

    @Override
    protected VariableValue<FSLocationSpec> defaultValue() {
        return newValue(FSLocationSpec.NULL);
    }

    @Override
    public String getIdentifier() {
        return "FSLocationSpec";
    }

    @Override
    protected boolean canOverwrite(final Config config, final String configKey) {
        return isFSLocationSpec(config, configKey);
    }

    private static boolean isFSLocationSpec(final Config config, final String configKey) {
        try {
            return FSLocationUtils.isFSLocationSpec(config.getConfig(configKey));
        } catch (InvalidSettingsException ex) {
            // the key did not correspond to a config -> this can't be an FSLocationSpec
            return false;
        }
    }

    @Override
    protected void overwrite(final FSLocationSpec value, final Config config, final String configKey)
        throws InvalidConfigEntryException {
        if (!canOverwrite(config, configKey)) {
            throw new InvalidConfigEntryException(
                "The provided config does not correspond to a FSLocationSpec.",
                v -> String.format(
                    "The variable '%s' can't overwrite the setting '%s' because it is not a FSLocationSpec.",
                    v, config.getEntry(configKey)));
        }
        FSLocationUtils.saveFSLocationSpec(value, config.addConfig(configKey));
    }

    @Override
    protected boolean canCreateFrom(final Config config, final String configKey) {
        return isFSLocationSpec(config, configKey);
    }

    @Override
    protected FSLocationSpec createFrom(final Config config, final String configKey)
        throws InvalidSettingsException, InvalidConfigEntryException {
        if (!canCreateFrom(config, configKey)) {
            throw new InvalidConfigEntryException(
                "The provided config does not correspond to a FSLocationSpec.",
                v -> String.format("The settings stored in '%s' can't be exposed as flow variable '%s'.",
                    config.getEntry(configKey), v));
        }
        return FSLocationUtils.loadFSLocationSpec(config.getConfig(configKey));
    }

    /**
     * Used to register {@link FSLocationSpecVariableType} at the Flow Variable extension point.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class FSLocationSpecVariableTypeExtension implements VariableTypeExtension {

        @Override
        public VariableType<?> getVariableType() {
            return INSTANCE;
        }

    }

}
