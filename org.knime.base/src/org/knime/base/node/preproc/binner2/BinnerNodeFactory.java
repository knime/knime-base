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
 *   Apr 16, 2025 (david): created
 */
package org.knime.base.node.preproc.binner2;

import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * Node factory for the web UI version of the AutoBinner node (which is the 4th version of this node!).
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class BinnerNodeFactory extends WebUINodeFactory<BinnerNodeModel> {

    /**
     * Constructor for the AutoBinner node factory.
     */
    public BinnerNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public BinnerNodeModel createNodeModel() {
        return new BinnerNodeModel(CONFIGURATION);
    }

    static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Binner") //
        .icon("binner.png") //
        .shortDescription("Groups numeric values into labeled bins using automatic or custom-defined intervals.") //
        .fullDescription("""
                Groups numeric columns into discrete intervals, known as bins. This \
                node supports both automatic and manual binning methods, including \
                equal-width, equal-frequency, custom cutoffs, and quantile-based \
                binning. Each bin is assigned a label based on its position, boundary \
                values, or midpoint. The resulting binned values are output as \
                string-type columns, either replacing the original data or appended \
                alongside it. This node combines the functionality of the Auto-Binner \
                and Numeric Binner nodes.
                """) //
        .modelSettingsClass(BinnerNodeSettings.class) //
        .addInputTable("Input Data", "Data to be categorized") //
        .addOutputTable("Binned Data", "Data with bins defined") //
        .addOutputPort("PMML Processing Fragment", PMMLPortObject.TYPE,
            "The PMML Model fragment containing information how to bin") //
        .keywords("Auto Binner", "Numeric Binner") //
        .build();
}
