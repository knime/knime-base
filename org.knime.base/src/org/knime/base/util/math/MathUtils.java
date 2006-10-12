/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   07.10.2006 (sieb): created
 */
package org.knime.base.util.math;

/**
 * Implements basic mathematical functions.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public final class MathUtils {

    private MathUtils() {

    }

    /**
     * Calculates sqrt(x^2 + y^2) reducing the risk of over- or underflow. The
     * default equation is transformed as follows:<br>
     * result = sqrt(x^2 + y^2)<br>
     * result^2 = x^2 + y^2<br>
     * result^2 = x^2 * (1 + y^2/x^2)<br>
     * result^2 = x^2 * (1 + (y/x)^2)<br>
     * result = |x| * sqrt(1 + (y/x)^2)<br>
     * 
     * It is important to perform a case differentiation. The formula is
     * transformed the same way but by extracting y instead of x. The advantage
     * is that the ^2 is performed on a mostly smaller number due to the
     * division.
     * 
     * @param x the x value
     * @param y the y value
     * @return sqrt(x^2 + y^2)
     */
    public static double hypotenuse(final double x, final double y) {

        double result;

        if (Math.abs(x) > Math.abs(y)) {
            result = y / x;
            result = Math.abs(x) * Math.sqrt(1 + result * result);
        } else if (y != 0) {
            result = x / y;
            result = Math.abs(y) * Math.sqrt(1 + result * result);
        } else {
            result = 0.0;
        }
        
        return result;
    }

    /**
     * Multiplies two matrices. Matrix 1 is multiplied from the left to matrix
     * 2. Therefore, result matrix = matrix 1 * matrix 2. The matrices must be
     * compatible, i.e. the number of columns of matrix 1 must equal to the
     * number of rows of matrix 2.
     * 
     * @param matrix1 the matrix on the left side
     * @param matrix2 the matrix on the right side
     * @return the result matrix
     * @throws IllegalArgumentException if the matrices are not compatible
     */
    public static double[][] multiply(final double[][] matrix1,
            final double[][] matrix2) throws IllegalArgumentException {

        // set the number of columns for both matrices M1 and M2
        int numColsM1 = matrix1[0].length;
        int numColsM2 = matrix2[0].length;

        // set the number of rows for both matrices M1 and M2
        int numRowsM1 = matrix1.length;
        int numRowsM2 = matrix2.length;

        // check matrix compatibility
        if (numColsM1 != numRowsM2) {
            throw new IllegalArgumentException(
                    "Uncompatible matrices for multiplication.");
        }

        // the result matrix has the number of rows of matrix 1 and the
        // number of columns of matrix 2
        double[][] resultMatrix = new double[numRowsM1][numColsM2];

        // the result matrix is created row by row, i.e. it is iterated
        // over the rows of matrix 1
        for (int rowM1 = 0; rowM1 < numRowsM1; rowM1++) {

            // now it is iterated over the number of columns of matrix 2
            for (int colM2 = 0; colM2 < numColsM2; colM2++) {

                // finally perform the multiplication for all columns of the
                // current row of matrix 1 with all row fields of the current
                // column of matrix 2, as the number of cols of matrix 1
                // must be equal with the number of rows of matrix 2 (matrix
                // compatibility) it does not matter which variable is
                // used for the loop as termination value (here: numColM1)
                double tmp = 0;
                for (int k = 0; k < numColsM1; k++) {
                    tmp += matrix1[rowM1][k] * matrix2[k][colM2];
                }
                resultMatrix[rowM1][colM2] = tmp;
            }
        }

        return resultMatrix;
    }

    /**
     * Transposes the given matrix.
     * 
     * @param inputMatrix the matrix to transposed
     * @return the transposed matrix where the number of rows and columns is
     *         changed according to the given matrix
     */
    public static double[][] transpose(final double[][] inputMatrix) {

        int numCols = inputMatrix[0].length;
        int numRows = inputMatrix.length;

        // create the result matrix
        double[][] transposedMatrix = new double[numCols][numRows];

        for (int row = 0; row < numRows; row++) {

            for (int col = 0; col < numCols; col++) {

                transposedMatrix[col][row] = inputMatrix[row][col];
            }
        }

        return transposedMatrix;
    }

    /**
     * Calculates the inverse matrix of a given matrix. The implementation
     * applies the decomposition according to Gauss-Jordan identifying pivot
     * elements.
     * 
     * @param aOrig the original matrix
     * @return the inverse matrix
     * @throws ArithmeticException if the matrix is not a square matrix or the
     *             inverse can not be computed (because of linear dependencies)
     * @throws NullPointerException if the argument is <code>null</code> or
     *             contains <code>null</code> elements
     */
    public static double[][] inverse(final double[][] aOrig) {
        final int size = aOrig.length;
        double[][] a = new double[size][];
        for (int r = 0; r < size; r++) {
            if (aOrig[r].length != size) {
                throw new ArithmeticException(
                        "Can't compute inverse of non-square matrix.");
            }
            double[] buf = new double[size];
            System.arraycopy(aOrig[r], 0, buf, 0, size);
            a[r] = buf;
        }
        double[][] e = new double[size][size];
        int[] rowOrder = new int[size];
        for (int i = 0; i < size; i++) {
            rowOrder[i] = i;
            e[i][i] = 1.0;
        }
        // over all columns
        for (int c = 0; c < size; c++) {
            // (a) determine pivot row, pivot element is at (c, P[c])
            int l = c;
            double max = Math.abs(a[rowOrder[l]][c]);
            // over all rows
            for (int r = c + 1; r < size; r++) {
                double value = Math.abs(a[rowOrder[r]][c]);
                if (value > max) {
                    max = value;
                    l = r;
                }
            }
            if (max == 0.0) {
                throw new ArithmeticException("No solution.");
            }
            int swap = rowOrder[c];
            rowOrder[c] = rowOrder[l];
            rowOrder[l] = swap;
            l = rowOrder[c];
            // normalize pivot row
            double pivotValue = a[l][c];
            for (int c1 = 0; c1 < size; c1++) {
                a[l][c1] = a[l][c1] / pivotValue;
                e[l][c1] = e[l][c1] / pivotValue;
            }
            for (int r1 = 0; r1 < size; r1++) {
                if (rowOrder[r1] != l) {
                    for (int c1 = 0; c1 < size; c1++) {
                        if (c1 != c) {
                            a[rowOrder[r1]][c1] -= a[rowOrder[r1]][c]
                                    * a[l][c1];
                        }
                        e[rowOrder[r1]][c1] -= a[rowOrder[r1]][c] * e[l][c1];
                    }
                }
            }
            for (int r1 = 0; r1 < size; r1++) {
                if (rowOrder[r1] != l) {
                    a[rowOrder[r1]][c] = 0.0;
                }
            }
        }
        double[][] inverse = new double[size][];
        for (int r = 0; r < size; r++) {
            inverse[r] = e[rowOrder[r]];
        }
        return inverse;
    }
    
    /**
     * Normalizes the matrix relative to the mean of the input data and to the
     * standard deviation.
     * 
     * @param matrix the matrix to normalize
     * @param standardDev the standard deviation for all columns used to
     *            normalize the matrix
     * @param mean the mean for all columns used to normalize the matrix
     * @return the normalized matrix
     */
    public static double[][] normalizeMatrix(final double[][] matrix,
            final double[] standardDev, final double[] mean) {

        double[][] normMatrix = new double[matrix.length][matrix[0].length];

        for (int row = 0; row < normMatrix.length; row++) {

            for (int column = 0; column < normMatrix[row].length; column++) {

                normMatrix[row][column] = (matrix[row][column] - mean[column])
                        / standardDev[column];
            }
        }

        return normMatrix;
    }

    /**
     * Normalizes the matrix relative to the mean of the input data.
     * 
     * @param matrix the matrix to normalize
     * @param mean the mean for all columns used to normalize the matrix
     * 
     * @return the normalized matrix
     */
    public static double[][] normalizeMatrix(final double[][] matrix,
            final double[] mean) {

        double[][] normMatrix = new double[matrix.length][matrix[0].length];

        for (int row = 0; row < normMatrix.length; row++) {

            for (int column = 0; column < normMatrix[row].length; column++) {

                normMatrix[row][column] = matrix[row][column] - mean[column];
            }
        }

        return normMatrix;
    }

    /**
     * Denormalizes the matrix relativ to the mean of the input data and to the
     * standard deviation.
     * 
     * @param y the matrix to denormalize
     * @param standardDev the standard deviation for all columns used to
     *            denormalize the matrix
     * @param mean the mean for all columns used to denormalize the matrix
     * @return the denormalized matrix
     */
    public static double[][] denormalizeMatrix(final double[][] y,
            final double[] standardDev, final double[] mean) {

        double[][] denormMatrix = new double[y.length][y[0].length];

        for (int i = 0; i < denormMatrix.length; i++) {

            for (int j = 0; j < denormMatrix[i].length; j++) {

                denormMatrix[i][j] = (y[i][j] * standardDev[j]) + mean[j];
            }
        }

        return denormMatrix;
    }

    /**
     * Denormalizes the vector relative to the mean of the input data and to the
     * standard deviation.
     * 
     * @param vector the input array to denormalize
     * @param standardDev the standard deviation for all columns used to
     *            denormalize the matrix
     * @param mean the mean for all columns used to denormalize the matrix
     * @return the denormalized vector
     */
    public static double[] denormalizeVector(final double[] vector,
            final double standardDev, final double mean) {

        return denormalizeMatrix(new double[][]{vector},
                new double[]{standardDev}, new double[]{mean})[0];
    }

    /**
     * Denormalizes the vector relative to the mean of the input data.
     * 
     * @param vector the input array to denormalize
     * @param mean the mean for all columns used to denormalize the matrix
     * @return the denormalized vector
     */
    public static double[] denormalizeVector(final double[] vector,
            final double mean) {

        return denormalizeMatrix(new double[][]{vector}, new double[]{mean})[0];
    }

    /**
     * Denormalizes the matrix relativ to the mean of the input data.
     * 
     * @param y the matrix to denormalize
     * @param mean the mean for all columns used to denormalize the matrix
     * @return the denormalized matrix
     */
    public static double[][] denormalizeMatrix(final double[][] y,
            final double[] mean) {

        double[][] denormMatrix = new double[y.length][y[0].length];

        for (int i = 0; i < denormMatrix.length; i++) {

            for (int j = 0; j < denormMatrix[i].length; j++) {

                denormMatrix[i][j] = y[i][j] + mean[j];
            }
        }

        return denormMatrix;
    }
    
    
    /**
     * Computes the spectral norm of the given matrix.
     * It is defined as the square root of the maximum absolute value of the
     * eigenvalues of the product of the matrix with its transposed form.
     * @param matrix the matrix to compute the norm for.
     * @return the spectral norm of the matrix.
     */
    public static double spectralNorm(final double[][] matrix) {
        double[][] matrixTransposed = transpose(matrix);
        EigenvalueDecomposition evd = new EigenvalueDecomposition(
                            multiply(matrix, matrixTransposed));
        double maxR = Double.MIN_VALUE;
        double[] real = evd.get1DRealD();
        double[] imag = evd.get1DImagD();
        for (int i = 0; i < real.length; i++) {
            double r = Math.sqrt((real[i] * real[i]) + (imag[i] * imag[i]));
            maxR = Math.max(r, maxR);
        }
        return Math.sqrt(maxR);
    }
    
}
