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
 * ------------------------------------------------------------------------
 */

package org.knime.base.data.filter.row.dialog;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.base.data.filter.row.dialog.model.Condition;
import org.knime.base.data.filter.row.dialog.model.Group;
import org.knime.base.data.filter.row.dialog.model.GroupType;
import org.knime.base.data.filter.row.dialog.model.Node;
import org.knime.base.data.filter.row.dialog.model.Operation;
import org.knime.base.data.filter.row.dialog.model.Operator;
import org.knime.base.data.filter.row.dialog.util.NodeTreeSerializer;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeSettings;

@SuppressWarnings("javadoc")
public class NodeTreeSerializerTest {

    private NodeSettings m_config;

    @Before
    public void setUp() throws IOException {
        m_config = new NodeSettings("row_filter");
    }

    @Test
    public void canSaveAndLoadOneCondition() throws Exception {
        // given
        final Condition condition = new Condition(1);
        final ColumnSpec columnSpec = new ColumnSpec("column_1", IntCell.TYPE);
        condition.setColumnSpec(columnSpec);
        final Operator operator = new Operator("equal", " = ");
        final String[] values = new String[]{"100"};
        final Operation operation = new Operation(operator, values);

        condition.setOperation(operation);

        final Node root = new Node(condition);

        // when
        NodeTreeSerializer.saveNodeToConfig(m_config, root);

        final Node actualNode = NodeTreeSerializer.loadNodeFromConfig(m_config);

        // then
        Assert.assertNotNull(actualNode);

        final Condition actualCondition = (Condition)actualNode.getElement();
        final ColumnSpec actualColumnSpec = actualCondition.getColumnSpec();
        final Operation actualOperation = actualCondition.getOperation();
        final Operator actualOperator = actualOperation.getOperator();
        final String[] actualValues = actualOperation.getValues();

        Assert.assertThat(actualValues, Matchers.equalTo(values));
        Assert.assertThat(actualOperator, Matchers.equalTo(operator));
        Assert.assertThat(operation, Matchers.equalTo(actualOperation));
        Assert.assertThat(columnSpec, Matchers.equalTo(actualColumnSpec));
        Assert.assertThat(actualCondition, Matchers.equalTo(condition));
        Assert.assertThat(actualNode, Matchers.equalTo(root));
    }

    @Test
    public void canSaveAndLoadOneGroup() throws Exception {
        // given

        final Group group = new Group(1);
        group.setType(new GroupType("or", "OR"));

        // Condition 1
        final Condition condition1 = new Condition(1);
        final ColumnSpec columnSpec1 = new ColumnSpec("column_name", StringCell.TYPE);
        condition1.setColumnSpec(columnSpec1);
        final Operator operator1 = new Operator("equal", " = ");
        final Operation operation1 = new Operation(operator1, new String[]{"John"});
        condition1.setOperation(operation1);

        // Condition 2
        final Condition condition2 = new Condition(2);
        final ColumnSpec columnSpec2 = new ColumnSpec("column_age", IntCell.TYPE);
        condition2.setColumnSpec(columnSpec2);
        final Operator operator2 = new Operator("between", "BETWEEN");
        final Operation operation2 = new Operation(operator2, new String[]{"50", "150"});
        condition2.setOperation(operation2);

        final Node root = new Node(group);
        final Node conditionNode1 = new Node(condition1);
        root.addChild(conditionNode1);
        final Node conditionNode2 = new Node(condition2);
        root.addChild(conditionNode2);

        // when
        NodeTreeSerializer.saveNodeToConfig(m_config, root);

        final Node actualNode = NodeTreeSerializer.loadNodeFromConfig(m_config);

        // then
        Assert.assertNotNull(root);

        final Group actualGroup = (Group)actualNode.getElement();
        final Condition actualCondition1 = (Condition)actualNode.getChildren().get(0).getElement();
        final Condition actualCondition2 = (Condition)actualNode.getChildren().get(1).getElement();

        Assert.assertThat(actualCondition1, Matchers.equalTo(condition1));
        Assert.assertThat(actualCondition2, Matchers.equalTo(condition2));
        Assert.assertThat(actualGroup, Matchers.equalTo(group));
        Assert.assertThat(actualNode, Matchers.equalTo(root));
    }

}
