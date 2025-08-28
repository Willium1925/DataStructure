package D0828;

import java.time.LocalDate;

/**
 * 交易資料模型 (POJO)
 * 實現 Comparable 介面以支援基於 customerId 的排序
 */
public class TransactionData implements Comparable<TransactionData> {
    private final LocalDate transactionDate;
    private final long customerId; // 使用 long 以便於插補搜尋
    private final String itemName;
    private final double price;

    public TransactionData(LocalDate transactionDate, long customerId, String itemName, double price) {
        this.transactionDate = transactionDate;
        this.customerId = customerId;
        this.itemName = itemName;
        this.price = price;
    }

    // --- Getters (必要) ---
    public long getCustomerId() {
        return customerId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public String getItemName() {
        return itemName;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public int compareTo(TransactionData other) {
        return Long.compare(this.customerId, other.customerId);
    }

    @Override
    public String toString() {
        return "TransactionData{" +
                "transactionDate=" + transactionDate +
                ", customerId=" + customerId +
                ", itemName='" + itemName + '\'' +
                ", price=" + price +
                '}';
    }
}
