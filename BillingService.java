import java.util.ArrayList;
import java.util.List;

public class BillingService {
    public static class BillLine {
        final Object[] medicineRow;
        final int quantity;
        final double price;
        final double amount;
        final int updatedStock;
        final String updatedStatus;

        public BillLine(Object[] medicineRow, int quantity, double price, double amount, int updatedStock, String updatedStatus) {
            this.medicineRow = medicineRow;
            this.quantity = quantity;
            this.price = price;
            this.amount = amount;
            this.updatedStock = updatedStock;
            this.updatedStatus = updatedStatus;
        }
    }

    public static int saveBillAndUpdateStock(String customer, String mobile, String payment, double subtotal, double tax, double total, List<BillLine> lines) {
        List<Object[]> billItems = new ArrayList<>();
        for (BillLine line : lines) {
            billItems.add(new Object[]{
                    line.medicineRow[0],
                    line.medicineRow[1],
                    line.quantity,
                    line.price,
                    line.amount
            });
        }

        int billId = DatabaseManager.saveBill(customer, mobile, payment, subtotal, tax, total, billItems);
        for (BillLine line : lines) {
            DatabaseManager.updateMedicine(new Object[]{
                    line.medicineRow[0],
                    line.medicineRow[1],
                    line.medicineRow[2],
                    line.updatedStock,
                    line.medicineRow[4],
                    line.medicineRow[5],
                    line.updatedStatus
            });
        }
        return billId;
    }
}
