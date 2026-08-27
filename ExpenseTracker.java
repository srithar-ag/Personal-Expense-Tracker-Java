```java
import java.util.ArrayList;
import java.util.Scanner;

class Expense {
    String category;
    double amount;

    Expense(String category, double amount) {
        this.category = category;
        this.amount = amount;
    }
}

public class ExpenseTracker {

    static ArrayList<Expense> expenses = new ArrayList<>();
    static Scanner scanner = new Scanner(System.in);

    // Add a new expense
    static void addExpense() {
        System.out.print("Enter expense category: ");
        String category = scanner.nextLine();

        System.out.print("Enter amount: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();

        expenses.add(new Expense(category, amount));

        System.out.println("Expense added successfully!\n");
    }

    // Display all expenses
    static void viewExpenses() {
        if (expenses.isEmpty()) {
            System.out.println("No expenses recorded.\n");
            return;
        }

        System.out.println("\n--- Expense List ---");

        for (int i = 0; i < expenses.size(); i++) {
            Expense expense = expenses.get(i);

            System.out.println(
                (i + 1) + ". " +
                expense.category +
                " - ₹" +
                expense.amount
            );
        }

        System.out.println();
    }

    // Calculate total expenses
    static void calculateTotal() {
        double total = 0;

        for (Expense expense : expenses) {
            total += expense.amount;
        }

        System.out.println("Total Expenses: ₹" + total + "\n");
    }

    public static void main(String[] args) {

        while (true) {

            System.out.println("===== PERSONAL EXPENSE TRACKER =====");
            System.out.println("1. Add Expense");
            System.out.println("2. View Expenses");
            System.out.println("3. Calculate Total");
            System.out.println("4. Category Report");
            System.out.println("5. Exit");

            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {

                case 1:
                    addExpense();
                    break;

                case 2:
                    viewExpenses();
                    break;

                case 3:
                    calculateTotal();
                    break;

                case 4:
                    ExpenseReport.showCategoryReport();
                    break;

                case 5:
                    System.out.println("Thank you for using Expense Tracker!");
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid choice. Try again.\n");
            }
        }
    }
}
```
