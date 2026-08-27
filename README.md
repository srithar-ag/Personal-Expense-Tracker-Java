# Personal Expense Tracker / ExpenseFlow Dashboard

This project now contains two runnable experiences:

- The original console application in `ExpenseTracker.java` and `ExpenseReport.java` is preserved.
- `dashboard/` adds an ExpenseFlow dashboard with a Java REST API, seeded demo data, responsive browser UI, transaction CRUD, analytics, services, budgets, subscriptions, reports, import placeholders, and integration-safe connection messaging.

## Run the dashboard

Requires JDK 17 or newer. From the project root:

```powershell
javac dashboard/DashboardServer.java
java -cp dashboard DashboardServer
```

Open `http://localhost:8080`.

The dashboard uses in-memory demo data and does not request or store website passwords. CSV, email receipt, bank statement, and official API import paths are represented by integration-ready UI placeholders.

## REST API

`GET /api/transactions`, `POST /api/transactions`, `PUT /api/transactions/{id}`, `DELETE /api/transactions/{id}`

`GET /api/dashboard`, `/api/analytics`, `/api/categories`, `/api/services`, `/api/budgets`, `/api/subscriptions`, and `/api/reports`

POST transaction bodies use URL-encoded fields such as `amount`, `merchant`, `category`, `website`, `date`, `paymentMethod`, and `description`.

## Run the original console app

```powershell
javac ExpenseTracker.java ExpenseReport.java
java ExpenseTracker
```
