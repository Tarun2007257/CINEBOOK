# CineBook - Movie Ticket Booking (Java + SQLite)

Console app linking **OOP** (Java classes) with **DBMS** (SQLite through JDBC).

## Run on a MacBook

### Step 1 - Install Java JDK (one time)
Check first:  `javac -version`
If "command not found", install either way:
- Download the JDK installer (Temurin 21) from https://adoptium.net and double-click it, or
- `brew install --cask temurin`

Close and reopen Terminal, then check `javac -version` again.

### Step 2 - Run
```bash
cd path/to/CineBook
./run.sh
```
(If "permission denied": `chmod +x run.sh`.)
The first run downloads the SQLite driver into `lib/` (internet needed once).
A file `cinebook.db` is created; your bookings survive restarts.
Delete `cinebook.db` to reset everything.

### Manual commands (what run.sh does)
```bash
javac -cp lib/sqlite-jdbc-3.36.0.3.jar -d out src/*.java
java -cp "out:lib/sqlite-jdbc-3.36.0.3.jar" CineApp
```

## How OOP and DBMS connect

| OOP (Java)              | DBMS (SQLite)                                   |
|-------------------------|-------------------------------------------------|
| `Show` object           | row in `shows`                                  |
| `Booking` object        | row in `bookings` (+ rows in `booked_seats`)    |
| `Show.grid` char[][]    | rebuilt at startup from `booked_seats`          |
| `SeatType` enum price   | copied into `booked_seats.price` when booked    |
| Static booking counter  | restarts after the largest stored `booking_id`  |
| All-or-nothing booking  | SQL transaction (commit / rollback)             |
| Cancel + 80% refund     | UPDATE status, DELETE seats (one transaction)   |

## Database design
- `shows(show_id PK, movie, show_time)`
- `bookings(booking_id PK, show_id FK, seat_list, total_amount, coupon, status CHECK, refund_amount, booked_at)`
- `booked_seats(show_id, seat_code) composite PK, booking_id FK, seat_type, price)`

The composite key on (show_id, seat_code) makes the database itself reject a double booking.
Menu option 4 runs JOIN / GROUP BY / aggregate queries (sales by show, seats by category).

## Class overview
- `SeatType` enum (price field), `Coupon` enum (CINE10 = 10%)
- `Bookable` interface (`bookSeats`, `cancelBooking`), implemented by `Show`
- `Show` - 5x8 `char[][]` grid, validation, overloaded `calculateTotal()`
- `Booking` - static ID counter (BK1001...), refund logic
- `Theatre` - ArrayList of shows and bookings, coordinates grid + database
- `Database` - all SQL/JDBC code
- `CineApp` - `main()`, menu and input only

## Note on the assignment rules
The brief says "No file handling, database or GUI". Since your team has been asked to link DBMS,
check with your faculty. If a pure-OOP version is needed, the seat grid and ArrayList already work
without the database; only `Database.java` and the calls to it in `Theatre.java` need removing.
