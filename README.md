# 🎬 CineBook

A terminal-based movie ticket booking system built with **Java** and **SQLite**. CineBook combines Object-Oriented Programming with relational database management to make booking a movie ticket quick and simple.

---

## 📌 Features

- View the list of available movies
- Check show timings and seat availability
- Book tickets for a selected movie and show
- Store and retrieve booking data using an SQLite database
- View booking details after confirmation
- Menu-driven interface that is easy to use from the terminal

> ✏️ Edit this list to match your project (for example: cancel booking, user login, admin options).

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java |
| Database | SQLite |
| Connectivity | JDBC |
| Concepts used | OOP (classes, objects, encapsulation, inheritance), DBMS (tables, queries, CRUD) |

---

## 🧱 Project Structure

```
CineBook/
├── src/
│   ├── Main.java          # Entry point and menu
│   ├── Movie.java         # Movie class
│   ├── Booking.java       # Booking class
│   └── DatabaseHelper.java # SQLite connection and queries
├── cinebook.db            # SQLite database
└── README.md
```

> ✏️ Replace the file names with your real ones.

---

## 🗄️ Database Design

The application stores its data in SQLite tables such as:

- **movies**: movie id, title, show time, available seats
- **bookings**: booking id, customer name, movie id, number of seats

> ✏️ Update the table and column names to match your schema.

---

## ▶️ How to Run

1. **Install** Java JDK 8 or above.
2. **Clone** the repository:
   ```bash
   git clone https://github.com/<your-username>/CineBook.git
   cd CineBook
   ```
3. **Add** the SQLite JDBC driver (`sqlite-jdbc.jar`)
