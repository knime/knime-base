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
 *   Feb 27, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location.variable;

import static org.knime.filehandling.core.connections.FSLocation.NULL;

import java.util.Set;

import javax.swing.Icon;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableTypeExtension;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.location.internal.FSLocationUtils;

import com.google.common.collect.Sets;

/**
 * Singleton type of {@link FlowVariable} for handling {@link FSLocation} values. The singleton instance is accessible
 * via the {@link FSLocationVariableType#INSTANCE} field.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class FSLocationVariableType extends VariableType<FSLocation> {

    /**
     * The singleton instance of the {@link FSLocationVariableType}.
     */
    public static final FSLocationVariableType INSTANCE = new FSLocationVariableType();

    /** Singleton icon to be used to display this variable type. */
    private static final Icon ICON = SharedIcons.FLOWVAR_PATH.get();

    private FSLocationVariableType() {
        // singleton
    }

    private static final class FSLocationValue extends VariableValue<FSLocation> {

        private FSLocationValue(final FSLocation fsLocation) {
            super(INSTANCE, fsLocation);
        }

        @Override
        public FSLocation get() {
            return super.get();
        }
    }

    @Override
    public Class<FSLocation> getSimpleType() {
        return FSLocation.class;
    }

    @Override
    public Icon getIcon() {
        return ICON;
    }

    @Override
    protected VariableValue<FSLocation> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
        return new FSLocationValue(FSLocationUtils.loadFSLocation(settings));
    }

    @Override
    protected VariableValue<FSLocation> newValue(final FSLocation v) {
        return new FSLocationValue(CheckUtils.checkArgumentNotNull(v, "The value must not be null."));
    }

    @Override
    protected void saveValue(final NodeSettingsWO settings, final VariableValue<FSLocation> v) {
        FSLocationValue value = (FSLocationValue)v;
        FSLocationUtils.saveFSLocation(value.get(), settings);
    }

    @Override
    public String getIdentifier() {
        return "FSLocation";
    }

    /**
     * Used to register {@link FSLocationVariableType} at the Flow Variable extension point.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class FSLocationVariableTypeExtension implements VariableTypeExtension {

        @Override
        public VariableType<?> getVariableType() {
            return INSTANCE;
        }

    }

    @Override
    protected VariableValue<FSLocation> defaultValue() {
        return newValue(NULL);
    }

    @Override
    protected boolean canOverwrite(final Config config, final String configKey) {
        try {
            return FSLocationUtils.canOverwriteWithFSLocation(config.getConfig(configKey));
        } catch (InvalidSettingsException ex) {// NOSONAR
            // the key did not correspond to a config -> this can't be a FSLocation
            return false;
        }
    }

    @Override
    protected void overwrite(final FSLocation value, final Config config, final String configKey)
        throws InvalidConfigEntryException {
        if (!canOverwrite(config, configKey)) {
            throw new InvalidConfigEntryException("The provided config does not correspond to a FSLocation.",
                v -> String.format("The variable '%s' can't overwrite the setting '%s' because it is not a FSLocation.",
                    v, config.getEntry(configKey)));
        }
        FSLocationUtils.saveFSLocation(value, config.addConfig(configKey));
    }

    @Override
    protected boolean canCreateFrom(final Config config, final String configKey) {
        try {
            return FSLocationUtils.canCreateFromFSLocation(config.getConfig(configKey));
        } catch (InvalidSettingsException ex) {//NOSONAR
            // the key did not correspond to a config -> this can't be a FSLocation
            return false;
        }
    }

    @Override
    protected FSLocation createFrom(final Config config, final String configKey)
        throws InvalidSettingsException, InvalidConfigEntryException {
        if (!canCreateFrom(config, configKey)) {
            throw new InvalidConfigEntryException("The provided config does not correspond to a FSLocation.",
                v -> String.format("The settings stored in '%s' can't be exposed as flow variable '%s'.",
                    config.getEntry(configKey), v));
        }
        return FSLocationUtils.loadFSLocation(config.getConfig(configKey));
    }

    @Override
    public Set<VariableType<?>> getConvertibleTypes() {
        return Sets.newHashSet(this, FSLocationSpecVariableType.INSTANCE);
    }

    @Override
    protected <U> U getAs(final FSLocation value, final VariableType<U> conversionTarget) {
        if (conversionTarget == this || conversionTarget == FSLocationSpecVariableType.INSTANCE) {
            return conversionTarget.getSimpleType().cast(value);
        }
        throw new IllegalArgumentException(
            String.format("Flow variables of the type '%s' can't be converted to flow variables of the type '%s'.",
                this, conversionTarget));
    }

}
