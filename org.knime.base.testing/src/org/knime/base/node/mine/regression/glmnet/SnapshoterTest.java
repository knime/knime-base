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
 *   Jun 4, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.mine.regression.glmnet;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.knime.base.node.mine.regression.glmnet.data.Data;
import org.knime.base.node.mine.regression.glmnet.data.DefaultData;
import org.knime.base.node.mine.regression.glmnet.data.DenseFeature;
import org.knime.base.node.mine.regression.glmnet.data.Feature;
import org.knime.base.node.mine.regression.glmnet.data.VariableWeightContainer;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class SnapshoterTest {

    @Test
    public void testDestandardizeStandardize() throws Exception {
        final Feature feature1 = new DenseFeature(new double[]{1, 2, 3, 4}, false);
        final Feature feature2 = new DenseFeature(new double[]{15, 32, 94, 1}, false);
        final double[] target = new double[]{4, 6, 1, 5};
        final double[] weights = new double[]{0.1, 0.6, 0.2, 0.1};
        Data data =
            new DefaultData(new Feature[]{feature1, feature2}, target, new VariableWeightContainer(weights, false));
        final Snapshoter snapshoter = new Snapshoter(data);
        double[] standardizedCoefficients = new double[]{1.2, 8.5};
        LinearModel model = snapshoter.destandardize(standardizedCoefficients);
//        System.out.println(model);
        double[] restandardizedCoefficients = snapshoter.standardize(model);
//        System.out.println(Arrays.toString(restandardizedCoefficients));
        assertArrayEquals(standardizedCoefficients, restandardizedCoefficients, 1e-5);
    }

}
