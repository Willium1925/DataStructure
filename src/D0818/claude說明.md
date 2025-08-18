讓我詳細說明排序程式碼的運作邏輯：
排序邏輯詳細解析
1. 資料過濾 (filterStockData 方法)
   java// 逐一檢查每筆股票記錄
   for (StockRecord record : stockData) {
   // 檢查股票代碼是否符合條件
   boolean stockCodeMatch = targetStockCode.isEmpty() || record.stockCode.equals(targetStockCode);

   // 檢查日期是否在指定範圍內
   boolean dateInRange = (record.date.compareTo(startDate) >= 0) &&
   (record.date.compareTo(endDate) <= 0);
   }
2. 資料分組與聚合 (groupAndSummarizeData 方法)
   java// 使用HashMap存儲每支股票的統計資料
   Map<String, StockSummary> summaryMap = new HashMap<>();

   for (StockRecord record : filteredData) {
   // 取得或建立該股票的摘要物件
   StockSummary summary = summaryMap.get(record.stockCode);
   if (summary == null) {
   summary = new StockSummary(record.stockCode);
   summaryMap.put(record.stockCode, summary);
   }
   // 累加成交量和成交金額
   summary.totalVolume += record.volume;
   summary.totalAmount += record.amount;
   }
3. 核心排序邏輯 (sortStockSummaries 方法)
   javaCollections.sort(summaryList, new Comparator<StockSummary>() {
   @Override
   public int compare(StockSummary s1, StockSummary s2) {
   if (sortByVolume) {
   // 成交量由大到小：s2與s1順序對調
   return Long.compare(s2.totalVolume, s1.totalVolume);
   } else {
   // 成交金額由大到小：s2與s1順序對調
   return Long.compare(s2.totalAmount, s1.totalAmount);
   }
   }
   });
   系統功能特色

資料產生器：自動產生400個交易日、1800檔股票的測試資料
彈性查詢：支援單一日期或日期區間查詢
雙重排序：可依成交量或成交金額排序
即時顯示：透過GUI即時顯示排行榜結果
資料聚合：自動將同一股票在指定期間的資料進行加總

使用步驟

先執行 StockDataGenerator 產生測試資料
執行 StockAnalyzerGUI 開啟主程式
設定查詢條件（股票代碼可留空查詢全部）
選擇日期類型和排序方式
點擊「開始分析」查看結果

排序演算法使用了Java內建的 Collections.sort()，時間複雜度為 O(n log n)，適合處理大量股票資料的排序需求。