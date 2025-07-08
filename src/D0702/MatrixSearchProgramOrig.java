package D0702;

import java.util.*;

public class MatrixSearchProgramOrig {
    private int[][] matrix;
    private int size;
    private HashMap<Integer, Position> hashMap;

    // 位置類別，用於儲存座標
    static class Position {
        int row, col;

        Position(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public String toString() {
            return "(" + row + ", " + col + ")";
        }
    }

    // 建構子
    public MatrixSearchProgramOrig(int size) {
        this.size = size;
        this.matrix = new int[size][size];
        this.hashMap = new HashMap<>();
        generateMatrix();
        buildHashMap();
        prepareBinarySearch(); // 預先準備二元搜尋所需的排序資料
    }

    // 二元搜尋的預處理資料
    private List<NumberWithPosition> sortedList;

    // 預先準備二元搜尋所需的排序資料
    private void prepareBinarySearch() {
        sortedList = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                sortedList.add(new NumberWithPosition(matrix[i][j], new Position(i, j)));
            }
        }

        // 依據數值排序
        sortedList.sort((a, b) -> Integer.compare(a.value, b.value));
    }

    // 生成不重複的N×N矩陣
    private void generateMatrix() {
        List<Integer> numbers = new ArrayList<>();

        // 生成1到N²的數字
        for (int i = 1; i <= size * size; i++) {
            numbers.add(i);
        }

        // 打亂順序
        Collections.shuffle(numbers);

        // 填入矩陣
        int index = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = numbers.get(index++);
            }
        }
    }

    // 建立雜湊表
    private void buildHashMap() {
        hashMap.clear();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                hashMap.put(matrix[i][j], new Position(i, j));
            }
        }
    }

    // 顯示矩陣
    public void displayMatrix() {
        System.out.println("生成的 " + size + "×" + size + " 矩陣:");
        System.out.println("=" + "=".repeat(size * 6));

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.printf("%5d ", matrix[i][j]);
            }
            System.out.println();
        }
        System.out.println("=" + "=".repeat(size * 6));
    }

    // 1. 循序搜尋 (Sequential Search)
    public SearchResult sequentialSearch(int target) {
        long startTime = System.nanoTime();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (matrix[i][j] == target) {
                    long endTime = System.nanoTime();
                    return new SearchResult(true, new Position(i, j), endTime - startTime);
                }
            }
        }

        long endTime = System.nanoTime();
        return new SearchResult(false, null, endTime - startTime);
    }

    // 2. 二元搜尋 (Binary Search)
    public SearchResult binarySearch(int target) {
        // 開始計時 - 只計算搜尋階段
        long startTime = System.nanoTime();

        // 執行二元搜尋
        int left = 0, right = sortedList.size() - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            int midValue = sortedList.get(mid).value;

            if (midValue == target) {
                long endTime = System.nanoTime();
                return new SearchResult(true, sortedList.get(mid).position, endTime - startTime);
            } else if (midValue < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        long endTime = System.nanoTime();
        return new SearchResult(false, null, endTime - startTime);
    }

    // 3. 雜湊搜尋 (Hash Search)
    public SearchResult hashSearch(int target) {
        long startTime = System.nanoTime();

        Position position = hashMap.get(target);

        long endTime = System.nanoTime();

        if (position != null) {
            return new SearchResult(true, position, endTime - startTime);
        } else {
            return new SearchResult(false, null, endTime - startTime);
        }
    }

    // 輔助類別：數字與位置的組合
    static class NumberWithPosition {
        int value;
        Position position;

        NumberWithPosition(int value, Position position) {
            this.value = value;
            this.position = position;
        }
    }

    // 搜尋結果類別
    static class SearchResult {
        boolean found;
        Position position;
        long timeNanos;

        SearchResult(boolean found, Position position, long timeNanos) {
            this.found = found;
            this.position = position;
            this.timeNanos = timeNanos;
        }

        public double getTimeMicros() {
            return timeNanos / 1000.0;
        }
    }

    // 執行所有搜尋方法並比較時間
    public void searchAndCompare(int target) {
        System.out.println("\n搜尋目標數字: " + target);
        System.out.println("-".repeat(50));

        // 循序搜尋
        SearchResult seqResult = sequentialSearch(target);
        System.out.printf("循序搜尋: ");
        if (seqResult.found) {
            System.out.printf("找到於位置 %s, 時間: %.2f 微秒\n",
                    seqResult.position, seqResult.getTimeMicros());
        } else {
            System.out.printf("找不到, 時間: %.2f 微秒\n", seqResult.getTimeMicros());
        }

        // 二元搜尋
        SearchResult binResult = binarySearch(target);
        System.out.printf("二元搜尋: ");
        if (binResult.found) {
            System.out.printf("找到於位置 %s, 時間: %.2f 微秒\n",
                    binResult.position, binResult.getTimeMicros());
        } else {
            System.out.printf("找不到, 時間: %.2f 微秒\n", binResult.getTimeMicros());
        }

        // 雜湊搜尋
        SearchResult hashResult = hashSearch(target);
        System.out.printf("雜湊搜尋: ");
        if (hashResult.found) {
            System.out.printf("找到於位置 %s, 時間: %.2f 微秒\n",
                    hashResult.position, hashResult.getTimeMicros());
        } else {
            System.out.printf("找不到, 時間: %.2f 微秒\n", hashResult.getTimeMicros());
        }

        // 時間比較
        System.out.println("\n時間比較:");
        System.out.printf("循序搜尋: %.2f 微秒\n", seqResult.getTimeMicros());
        System.out.printf("二元搜尋: %.2f 微秒\n", binResult.getTimeMicros());
        System.out.printf("雜湊搜尋: %.2f 微秒\n", hashResult.getTimeMicros());
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("請輸入矩陣大小 N (N×N): ");
        int n = scanner.nextInt();

        // 建立矩陣
        MatrixSearchProgramOrig program = new MatrixSearchProgramOrig(n);

        // 顯示矩陣
        program.displayMatrix();

        while (true) {
            System.out.print("\n請輸入要搜尋的數字 (輸入 0 結束程式): ");
            int target = scanner.nextInt();

            if (target == 0) {
                System.out.println("程式結束，謝謝使用！");
                break;
            }

            // 執行搜尋並比較
            program.searchAndCompare(target);
        }

        scanner.close();
    }
}