package D0715.SparseMatrix;

import java.util.Random;

public class SparseMatrixPractice {

    public static void main(String[] args) {
        // 建立一個5x6的稀疏矩陣，非零元素比例約20%
        int[][] matrix = generateSparseMatrix(5, 6, 0.2);

        System.out.println("原始稀疏矩陣:");
        printMatrix(matrix);

        System.out.println("\n矩陣資訊:");
        printMatrixInfo(matrix);

        // 你可以在這裡實作轉置的方法
        System.out.println("\n縮減後的矩陣:");
         smallerMatrix(matrix);
    }

    /**
     * 生成隨機稀疏矩陣
     * @param rows 行數
     * @param cols 列數
     * @param density 非零元素的密度（0.0到1.0之間）
     * @return 稀疏矩陣
     */
    public static int[][] generateSparseMatrix(int rows, int cols, double density) {
        Random random = new Random();
        int[][] matrix = new int[rows][cols];

        // 計算需要填入的非零元素數量
        int totalElements = rows * cols;
        int nonZeroCount = (int) (totalElements * density);

        // 隨機填入非零元素
        for (int i = 0; i < nonZeroCount; i++) {
            int row, col;
            // 找到一個還是0的位置
            do {
                row = random.nextInt(rows);
                col = random.nextInt(cols);
            } while (matrix[row][col] != 0);

            // 填入1到9之間的隨機數
            matrix[row][col] = random.nextInt(9) + 1;
        }

        return matrix;
    }

    /**
     * 列印矩陣
     * @param matrix 要列印的矩陣
     */
    public static void printMatrix(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.printf("%3d ", matrix[i][j]);
            }
            System.out.println();
        }
    }

    /**
     * 列印矩陣資訊
     * @param matrix 矩陣
     */
    public static void printMatrixInfo(int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        int nonZeroCount = 0;

        // 計算非零元素數量
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (matrix[i][j] != 0) {
                    nonZeroCount++;
                }
            }
        }

        double density = (double) nonZeroCount / (rows * cols);

        System.out.println("矩陣大小: " + rows + " x " + cols);
        System.out.println("非零元素數量: " + nonZeroCount);
        System.out.println("稀疏度: " + String.format("%.2f%%", (1 - density) * 100));
        System.out.println("密度: " + String.format("%.2f%%", density * 100));
    }

    // 留給你實作的轉置方法框架

    public static void smallerMatrix(int[][] matrix) {
        // 如果原矩陣是 m列 x n行，轉置後會是 n x m

        int cols = matrix[0].length; // 獲取列數m
        int rows = matrix.length; // 獲取行數n

        // 先消化稀疏矩陣，原本列m行n，轉成行n列m值
        int[][][] smallMatrix = new int[rows][cols][1];

        for (int n = 0; n < rows; n++) {
            for (int m = 0; m <cols; m++) {
                if(matrix[m][n] != 0){
                    smallMatrix[n][m][0] = matrix[m][n];
                }
                System.out.printf("%3d ", n, "%3d ", m, "%3d ", smallMatrix[n][m][0]);
            }
            System.out.println();
        }

//        return smallMatrix; // 請替換成你的實作
    }

}
