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
 * History
 *   06.12.2005 (dill): created
 */
package org.knime.base.node.preproc.bitvector.create;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;
import org.knime.node.impl.description.ViewDescription;

/**
 * The factory for the Create Bit Vector Node.
 *
 * @author Tobias Koetter
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class CreateBitVectorNodeFactory extends NodeFactory<CreateBitVectorNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public CreateBitVectorNodeModel createNodeModel() {
        return new CreateBitVectorNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 1;
    }

    @Override
    public NodeView<CreateBitVectorNodeModel> createNodeView(final int viewIndex,
        final CreateBitVectorNodeModel nodeModel) {
        return new CreateBitVectorView(nodeModel);
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Create Bit Vector";

    private static final String NODE_ICON = "./bitVector.png";

    private static final String SHORT_DESCRIPTION = """
            Generates bit vectors either from multiple string or numerical columns, from a single string column
            containing the bit positions to set, hexadecimal, or binary strings or a single collection column.
            """;
    private static final String FULL_DESCRIPTION = """
            Generates for each row of a given input table a bit vector. The bit vectors are either generated from
            multiple numerical or string columns, a string column containing the bit positions to set, hexadecimal
            or binary strings or a collection column. In order to adjust the node settings please select first the
            source column object e.g. if the bit vector should be created from multiple string/numerical columns or
            from a single string/collection column. Depending on the selected option the corresponding dialog
            elements are enabled.<br/>

            <h3>Bit vectors from a single column</h3> In the case of a single input column
            only the selected single column to be parsed is considered for the generation of the bit vectors.
            <h4>Single string column</h4> In the case of a string input only the column containing the string is
            considered for the generation of the bit vectors. The string is parsed and converted into a bit vector.
            There are three valid input formats which can be parsed and converted:
            <ul>
                <li>Hexadecimal strings: Strings consisting only of the characters 0-9 and A - F
                (where lower- or uppercase is not important). The represented hexadecimal number is converted into a
                binary number which is represented by the resulting bit vector.
                </li>
                <li>Binary strings: Strings consisting only of 0s and 1s are parsed and converted into the according bit
                vectors.
                </li>
                <li>ID strings: Strings consisting of numbers (separated by spaces) where the numbers refer to those
                positions in the bit vector which should be set. (Typical input format for association rule mining).
                </li>
            </ul>

            <h4>Single collection column</h4> In the case of a
            single collection column each unique collection element gets a bit position assigned. The length of the
            bit vectors corresponds to the number of unique elements in a collection cells. For example if the input
            table contains two rows with the collections {a,b} and {b,c} the corresponding bit vectors will be [110]
            and [011].

            <h3>Bit vectors from multiple columns</h3> In the case of multiple columns the bit
            positions in the resulting bit vector correspond to the column position in the input table. For example
            if the second and third column of a given input table is selected and the first column is omitted the
            bit vectors of each row will have length 2. The first bit of the bit vector is set if the value of the
            second column matches the selected criterion likewise the second bit of the bit vector is set if the
            value of the third column matches the selected criterion. The columns to consider when creating the bit
            vector can be specified in the multiple column selection section. Using the enforce exclusion/inclusion
            option the node can be configured to handle previously unknown columns. If the enforce exclusion option
            is selected all unknown columns are added automatically to the include list whereas if the enforce
            inclusion option is selected all unknown columns are added to the exclude list. The columns to include
            can be also defined by a pattern if the Wildcard/Regex Selection option is selected.

            <h4>Multiple string columns</h4> The bit of a vector is set if the corresponding column value does
            match/does not match the specified pattern depending on the "Set bit if pattern does match/does not match"
            option. The pattern may contain wildcards such as '?' or '*' to match any one character or any sequence
            (including none) of characters. It can also be a complex <a
            href="http://java.sun.com/javase/6/docs/api/java/util/regex/Pattern.html#sum">regular expression</a>.

            <h4>Multiple numeric columns</h4> There are two options to determine if the bit is set for the value in
            the corresponding column or not:
            <ul>
                <li>Either a global threshold is defined, then all values which
                are above or equal to the threshold are converted into set bits, all other bit positions remain 0, or
                </li>
                <li>A certain percentage of the mean of each column is used as a threshold, then all values which are
                above or equal to the percentage of the mean are converted into set bits. As an example let's say the
                mean percentage is set to 50% and the mean of col1 is 2 and the mean of col2 is 8. Then the
                corresponding bit for col1 is set if the value is above or equal to 1 and for col2 if the value is
                above or equal to 4.
                </li>
            </ul>

            <h4>Missing values</h4> For numeric data the incoming missing values will result in 0s. For
            multiple string columns a missing values will also result in 0s. For the string input missing values
            will also result in a missing value in the output table. If a string could not be parsed it will also
            result in a missing cell in the output table and an error message with detailed information is printed
            in the console. For a collection column all missing collection elements are ignored.
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Input data to create bit vectors from", """
                Data table with numerical data or a string column to be parsed.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Bit vector data", """
                Data table with the generated bit vectors.
                """)
    );

    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Statistics View", """
                Provides information about the generation of the bit vectors from the data. In particular this is the
                number of processed rows, the total number of generated zeros and ones and the resulting ratio of 1s to
                0s.
                """)
    );

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, CreateBitVectorNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription( //
            NODE_NAME, //
            NODE_ICON, //
            INPUT_PORTS, //
            OUTPUT_PORTS, //
            SHORT_DESCRIPTION, //
            FULL_DESCRIPTION, //
            List.of(), //
            CreateBitVectorNodeParameters.class, //
            VIEWS, //
            NodeType.Manipulator, //
            List.of(), //
            null //
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, CreateBitVectorNodeParameters.class));
    }

}
