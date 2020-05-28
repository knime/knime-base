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
 *   May 22, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.filehandling.core.connections.FSLocationSpec;

/**
 * Abstract implementation of a {@link FSLocationSpecConfig} that provides getter and setters and handles listeners.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <S> The concrete type of {@link FSLocationSpec} handled by the subclass
 * @param <L> The concrete type of the subclass
 */
public abstract class AbstractLocationSpecConfig<S extends FSLocationSpec, L extends AbstractLocationSpecConfig<S, L>>
    implements FSLocationSpecConfig<L> {

    private final List<ChangeListener> m_listeners = new LinkedList<>();

    private final ChangeEvent m_changeEvent = new ChangeEvent(this);

    private S m_location;

    /**
     * Constructor.
     */
    protected AbstractLocationSpecConfig() {
    }

    /**
     * Copy constructor.
     *
     * @param toCopy instance to copy
     */
    protected AbstractLocationSpecConfig(final AbstractLocationSpecConfig<S, L> toCopy) {
        m_location = toCopy.m_location;
    }

    @Override
    public S getLocationSpec() {
        return m_location;
    }

    @Override
    public void setLocationSpec(final FSLocationSpec locationSpec) {
        if (!Objects.equals(m_location, locationSpec)) {
            m_location = adapt(locationSpec);
            notifyChangeListeners();
        }
    }

    /**
     * Converts the provided {@link FSLocationSpec locationSpec} into the concrete type handled by the subclass.</br>
     *
     * @param locationSpec to convert to the concrete type of the subclass
     * @return the converted locationSpec
     */
    protected abstract S adapt(final FSLocationSpec locationSpec);

    @Override
    public final void addChangeListener(final ChangeListener listener) {
        m_listeners.add(listener);
    }

    private final void notifyChangeListeners() {
        m_listeners.forEach(l -> l.stateChanged(m_changeEvent));
    }

}
