# Pharmacy Inventory System

A Java Swing desktop application for pharmacy inventory management with a modern pink-themed interface.

---

## Features
- Interactive dashboard with medicine statistics.
- Medicine management with add, edit, delete, CSV export, and PDF export.
- Live smart search by medicine name or category.
- Stock alerts for low and critical items.
- SQLite database persistence for saved medicines.

---

## How to Run
1. Install JDK 11 or higher.
2. Open a terminal in this project folder.
3. Compile:
   ```bash
   javac -encoding UTF-8 *.java
   ```
4. Run:
   ```bash
   java PharmacyInventorySystem
   ```

## Save Newly Added Medicines After Closing
If you want newly added, edited, or deleted medicines to be saved after closing the app, run with the SQLite JDBC dependency.

Required dependency:
- SQLite JDBC Driver: `org.xerial:sqlite-jdbc:3.42.0.0`
- Local jar path used by this project: `lib/sqlite-jdbc-3.42.0.0.jar`

Compile with SQLite:
```bash
javac -cp ".;lib\sqlite-jdbc-3.42.0.0.jar" *.java
```

Run with SQLite:
```bash
java -cp ".;lib\sqlite-jdbc-3.42.0.0.jar" PharmacyInventorySystem
```

This creates or uses `pharmacy.db` in the app folder. Medicines added through the app will be saved there and loaded again next time.

You can also use Maven because this project includes `pom.xml` with the SQLite dependency:
```bash
mvn compile exec:java
```

---

## Dependencies
- JDK 11 or higher
- SQLite JDBC Driver `3.42.0.0` for database persistence

---

## Tech Stack
- Java SE
- Swing and AWT
- SQLite

---

Created by jayshree
