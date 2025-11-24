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
 *   Nov 21, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.webui.reader2;

import java.util.Map;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.filehandling.core.node.table.reader.CommonTableReaderNodeFactory;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.paths.Source;
import org.knime.node.parameters.NodeParameters;

/**
 * Use this factory as base class for a reader node factory that uses {@link NodeParameters} for configuration.
 *
 * @param <P> the type of node [P]arameters
 * @param <I> the [I]tem type to read from
 * @param <S> the type of {@link Source [S]ource}
 * @param <C> the type of {@link ReaderSpecificConfig ReaderSpecific[C]onfig}
 * @param <T> the type used to identify external data [T]ypes
 * @param <V> the type of the [V]alues read from the source
 * @param <M> the type of {@link MultiTableReadConfig [M]ultiTableReadConfig}
 * @author Paul Bärnreuther
 * @since 5.10
 */
@SuppressWarnings("restriction")
public abstract class WebUITableReaderNodeFactory<P extends NodeParameters, I, S extends Source<I>, //
        C extends ReaderSpecificConfig<C>, T, V, //
        M extends MultiTableReadConfig<C, T>>
    extends CommonTableReaderNodeFactory<I, S, C, T, M, V> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private final Class<P> m_nodeParametersClass;

    /**
     * Constructor.
     *
     * @param nodeParametersClass the class of the node parameters
     */
    protected WebUITableReaderNodeFactory(final Class<P> nodeParametersClass) {
        m_nodeParametersClass = nodeParametersClass;
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, m_nodeParametersClass);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, m_nodeParametersClass));
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) { // NOSONAR
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    protected abstract NodeParametersConfigAndSourceSerializer<P, I, S, C, T, M> createSerializer();
}
