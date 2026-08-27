```java
import java.util.HashMap;
import java.util.Map;

public class ExpenseReport {

    public static void showCategoryReport() {

        if (ExpenseTracker.expenses.isEmpty()) {
            System.out.println("No expenses available for the report.\n");
            return;
        }

        HashMap<String, Double> categoryTotals = new HashMap<>();

        // Calculate total for each category
        for (Expense expense : ExpenseTracker.expenses) {

            categoryTotals.put(
                expense.category,
                categoryTotals.getOrDefault(expense.category, 0.0)
                    + expense.amount
            );
        }

        System.out.println("\n===== EXPENSE CATEGORY REPORT =====");

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {

            System.out.println(
                entry.getKey() + " : ₹" + entry.getValue()
            );
        }

        System.out.println("===================================\n");
    }
}
```
