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
 *   May 27, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader.simple;

import java.nio.file.Path;
import java.util.Optional;

import org.knime.base.node.io.filehandling.csv.reader.AbstractCSVTableReaderNodeFactory;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AbstractPathTableReaderNodeDialog;

/**
 * Node factory for the simple file reader node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
public final class SimpleFileReaderNodeFactory extends AbstractCSVTableReaderNodeFactory {//NOSONAR

    @Override
    protected final Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        return Optional.empty();
    }

    @Override
    protected PathAwareFileHistoryPanel createPathSettings(final NodeCreationConfiguration nodeCreationConfig) {
        final PathAwareFileHistoryPanel settings = new PathAwareFileHistoryPanel();
        final Optional<? extends URLConfiguration> urlConfig = nodeCreationConfig.getURLConfig();
        if (urlConfig.isPresent()) {
            settings.setPath(urlConfig.get().getUrl().toString());
        }
        return settings;
    }

    @Override
    protected AbstractPathTableReaderNodeDialog<CSVTableReaderConfig, Class<?>> createNodeDialogPane(
        final NodeCreationConfiguration creationConfig, final MultiTableReadFactory<Path, CSVTableReaderConfig, Class<?>> readFactory,
        final ProductionPathProvider<Class<?>> productionPathProvider) {
        return new SimpleFileReaderNodeDialog(createPathSettings(creationConfig), createConfig(creationConfig),
            readFactory, productionPathProvider);
    }


}
