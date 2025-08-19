package D0819;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 股票資料產生器，用於生成模擬的股票交易資料並儲存為 CSV 檔案。
 * 每個記錄包含股票代碼、名稱、交易日期、成交量、成交金額、單筆最大/最小成交量及金額。
 */
public class StockDataGenerator {
    private static final int NUM_STOCKS = 18000; // 總股票數量
    private static final int NUM_DAYS = 2000;   // 總交易天數
    // 可用的中文字符，用於生成有意義的股票名稱
    private static final String[] CHINESE_CHARS = {
            "台", "積", "電", "聯", "發", "科", "技", "股", "份", "有", "限", "公", "司",
            "金", "融", "控", "工", "業", "信", "託", "資", "產", "通", "訊", "網"
    };

    public static void main(String[] args) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream("stock_data.csv"), StandardCharsets.UTF_8))) {
            // 寫入 CSV 標頭，並添加 UTF-8 BOM 以確保中文正確顯示
            writer.write('\uFEFF');
            writer.write("股票代碼,股票名稱,交易日期,成交量,成交金額," +
                    "當日單筆最大成交量,當日單筆最大成交金額," +
                    "當日單筆最小成交量,當日單筆最小成交金額\n");

            Random random = new Random(); // 用於生成隨機數據
            LocalDate startDate = LocalDate.of(2015, 1, 1); // 起始日期
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // 遍歷每檔股票
            for (int stock = 1; stock <= NUM_STOCKS; stock++) {
                String stockCode = String.format("%06d", stock); // 格式化股票代碼為六位數
                String stockName = generateStockName(random); // 隨機生成股票名稱
                // 為每檔股票生成 2000 天的交易資料
                for (int day = 0; day < NUM_DAYS; day++) {
                    LocalDate date = startDate.plusDays(day); // 計算當天日期
                    // 隨機生成成交量 (1000 到 100萬)
                    long volume = 1000 + random.nextInt(1000000);
                    // 成交金額 = 成交量 * 單價（隨機 100 到 10100）
                    long amount = volume * (100 + random.nextInt(10000));
                    // 單筆最大成交量：100 到當日總成交量
                    long maxSingleVolume = 100 + random.nextInt((int) volume);
                    // 單筆最大成交金額：基於最大成交量計算
                    long maxSingleAmount = maxSingleVolume * (100 + random.nextInt(1000));
                    // 單筆最小成交量：100 到最大成交量
                    long minSingleVolume = 100 + random.nextInt((int) Math.min(maxSingleVolume, 10000));
                    // 單筆最小成交金額：基於最小成交量計算
                    long minSingleAmount = minSingleVolume * (50 + random.nextInt(500));

                    // 寫入 CSV 記錄
                    writer.write(String.format("%s,%s,%s,%d,%d,%d,%d,%d,%d\n",
                            stockCode, stockName, date.format(formatter),
                            volume, amount, maxSingleVolume, maxSingleAmount,
                            minSingleVolume, minSingleAmount));
                }
            }
            System.out.println("Data generation completed: stock_data.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成隨機股票名稱，長度為 2 到 6 個中文字符。
     * @param random 隨機數生成器
     * @return 股票名稱
     */
    private static String generateStockName(Random random) {
        int length = 2 + random.nextInt(5); // 隨機長度 2-6
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < length; i++) {
            name.append(CHINESE_CHARS[random.nextInt(CHINESE_CHARS.length)]);
        }
        return name.toString();
    }
}
