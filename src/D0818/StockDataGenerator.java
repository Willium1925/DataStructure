package D0818;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 股票資料產生器
 * 用於產生測試用的股票交易資料
 */
public class StockDataGenerator {

    private static final Random random = new Random();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        generateStockData();
    }

    /**
     * 產生股票資料並寫入CSV檔案
     */
    public static void generateStockData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("stock_data.csv"))) {
            // 寫入CSV標題行
            writer.println("股票代碼,交易日期,成交量,成交金額");

            // 產生400個交易日期（排除週末）
            List<LocalDate> tradingDates = generateTradingDates(400);

            // 產生1800檔股票代碼
            List<String> stockCodes = generateStockCodes(1800);

            // 為每個交易日和每檔股票產生交易資料
            for (LocalDate date : tradingDates) {
                for (String stockCode : stockCodes) {
                    // 產生隨機成交量（1000到1000000之間）
                    long volume = 1000 + random.nextInt(999000);

                    // 產生隨機成交金額（基於成交量計算，每股20-200元）
                    double pricePerShare = 20 + (random.nextDouble() * 180);
                    long amount = Math.round(volume * pricePerShare);

                    // 寫入資料到CSV
                    writer.printf("%s,%s,%d,%d%n",
                            stockCode,
                            date.format(DATE_FORMAT),
                            volume,
                            amount);
                }
            }

            System.out.println("股票資料已成功產生到 stock_data.csv");
            System.out.println("共產生 " + (tradingDates.size() * stockCodes.size()) + " 筆資料");

        } catch (IOException e) {
            System.err.println("檔案寫入錯誤: " + e.getMessage());
        }
    }

    /**
     * 產生指定數量的交易日期（排除週末）
     * @param days 需要的交易日數量
     * @return 交易日期清單
     */
    private static List<LocalDate> generateTradingDates(int days) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = LocalDate.now().minusYears(2); // 從兩年前開始

        while (dates.size() < days) {
            // 檢查是否為工作日（週一到週五）
            if (currentDate.getDayOfWeek().getValue() <= 5) {
                dates.add(currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        return dates;
    }

    /**
     * 產生股票代碼
     * @param count 需要產生的股票代碼數量
     * @return 股票代碼清單
     */
    private static List<String> generateStockCodes(int count) {
        List<String> codes = new ArrayList<>();

        // 產生台股風格的股票代碼（4位數字）
        for (int i = 1001; i <= 1000 + count; i++) {
            codes.add(String.valueOf(i));
        }

        return codes;
    }
}