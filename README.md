# Pharmacy Inventory System

A premium Java Swing desktop application for managing pharmacy inventory.

## Features
- **Interactive Dashboard**: Real-time stats for total medicines, expiring items, and low stock.
- **Medicine Management**: Add, delete, and view medicines with auto-status detection.
- **Live Search**: Instant filtering of inventory by name or category.
- **Stock Alerts**: Dedicated panel for items requiring immediate attention.
- **Modern UI**: Pink-themed design with rounded components, hover effects, and a live clock.

## Screenshots
![Dashboard Preview](screenshot.png) 
*(Note: Replace this with your own screenshot after running the app!)*

## How to Run
1. **Prerequisites**: Ensure you have [JDK 11](https://learn.microsoft.com/en-us/java/openjdk/download) or higher installed.
2. **Clone**: Clone this repository to your local machine.
3. **Compile**: Open a terminal in the project folder and run:
   ```bash
   javac -encoding UTF-8 *.java
   ```
4. **Run**: 
   ```bash
   java -Dfile.encoding=UTF-8 PharmacyInventorySystem
   ```
   *Note: The `-Dfile.encoding=UTF-8` flag ensures emojis and special characters render correctly.*


## Tech Stack
- Java Standard Edition (JDK 11+)
- Java Swing & AWT (No external libraries)
