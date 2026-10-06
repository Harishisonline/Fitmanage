# FitManage

FitManage is a desktop app for a single-location gym. Staff sign in, look up members, sell and change plans, take attendance, record payments, print receipts, and read a short set of reports. It is a Java Swing application. It does not run in a browser.

The demo gym is **Iron Pump Gym**. The first launch creates the MySQL schema and three demo accounts.

## Features

- **Members.** Add, edit, search, and deactivate members. Search works from the top bar and from the members list (name, phone, or member code such as `FM-000121`).
- **Plans.** Create and edit membership plans (price, duration, active or inactive).
- **Memberships.** Add a plan, renew it, upgrade to a different plan, cancel it, and reactivate a cancelled plan while its end date is still today or later. A plan that starts in the future shows as **Upcoming**. A plan that covers today shows as **Active** or **Expiring**. A plan whose end date has passed shows as **Expired**.
- **Payments.** Record a payment against the plan that covers today, list payments, and write a PDF receipt.
- **Attendance.** Check a member in and out, and export the day’s check-ins to CSV.
- **Trainers.** Keep trainer profiles, see assigned members, and record a monthly payout.
- **Progress.** Log body measurements for a member and chart that member’s own history.
- **Reports.** Revenue and attendance summaries for a chosen date range.
- **Users.** Admins create staff and trainer logins. Five failed passwords lock the account for 15 minutes.
- **Settings.** Profile, theme, gym details, database backup and restore, and an about page. Saved settings live in `~/.fitmanage/application.properties` and override the file shipped in the jar.
- **Health check.** While the app is open, `http://127.0.0.1:8080/health` returns JSON. `200` means MySQL is reachable. `503` means it is not. `http://127.0.0.1:8080/version` returns the app version.

Roles:

| Role | What they can open |
| --- | --- |
| ADMIN | Everything, including Users |
| STAFF | Members, plans, attendance, payments, reports, and their own settings |
| TRAINER | The trainer's own pages and settings. The Users screen is hidden |

## Tech stack

| Layer | Technology | Version | Role |
| --- | --- | --- | --- |
| Language | Java | 17 | Application language and runtime |
| UI | Java Swing | JDK 17 | Desktop windows, tables, and dialogs |
| Look and feel | FlatLaf | 3.4 | Theme for the Swing UI |
| Database | MySQL | 8 | Application database `fitmanage` on `localhost:3306` |
| Database access | JDBC | JDK 17 | SQL access from Java |
| Build | Apache Maven | — | Compile, test, and package |
| App version | FitManage | 1.0.0 | Packaged jar name `fitmanage-1.0.0.jar` |

| Library | Version | Scope | Used for |
| --- | --- | --- | --- |
| mysql-connector-j | 8.4.0 | runtime | MySQL JDBC driver |
| jBCrypt | 0.4 | runtime | Password hashing |
| FlatLaf | 3.4 | runtime | UI theme |
| JFreeChart | 1.5.4 | runtime | Attendance and progress charts |
| Apache PDFBox | 3.0.3 | runtime | Receipts and member-summary PDFs |
| SLF4J API | 2.0.13 | runtime | Logging API |
| Logback Classic | 1.5.6 | runtime | Logging implementation |
| JUnit Jupiter | 5.10.2 | test | Unit tests |
| AssertJ | 3.25.3 | test | Test assertions |
| H2 Database | 2.2.224 | test | In-memory database for tests |

| Build plugin | Version | Role |
| --- | --- | --- |
| maven-compiler-plugin | 3.13.0 | Compiles Java 17 sources |
| maven-surefire-plugin | 3.2.5 | Runs the unit tests |
| maven-shade-plugin | 3.6.0 | Builds one runnable jar |

Design references for the screens live in `assets/ui-mockups/`. The product specification is `Fitmanage-prd.md`.

## Prerequisites

1. **JDK 17.** Check with `java -version`. The line must say version `17`. A newer JDK on your `PATH` can fail to run this jar, and Java 8 will refuse it (`class file version 61`).
2. **Apache Maven 3.8+.** Check with `mvn -version`.
3. **MySQL 8** listening on `localhost:3306`, with a database and user that match `src/main/resources/application.properties`.
4. **`mysqldump` and `mysql` on your `PATH`** only if you want Settings → Database backup and restore. The rest of the app does not need them.

On macOS with Homebrew:

```bash
brew install openjdk@17 maven
```

Put JDK 17 first on your `PATH` for the shell you use to build and run:

```bash
export PATH="$(brew --prefix openjdk@17)/bin:$PATH"
java -version
```

## Start here

### 1. Clone the repository

```bash
git clone https://github.com/<your-org>/Fitmanage.git
cd Fitmanage
```

### 2. Start MySQL 8

**Docker** (matches the defaults already in `application.properties`):

```bash
docker run -d --name fitmanage-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpw \
  -e MYSQL_DATABASE=fitmanage \
  -e MYSQL_USER=fitmanage \
  -e MYSQL_PASSWORD=fitpw \
  -p 3306:3306 \
  mysql:8
```

Wait until the container accepts connections:

```bash
docker exec fitmanage-mysql mysqladmin ping -h 127.0.0.1 -ufitmanage -pfitpw --silent
```

If you already created this container earlier:

```bash
docker start fitmanage-mysql
```

**MySQL installed on the machine** instead of Docker:

```sql
CREATE DATABASE fitmanage CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'fitmanage'@'localhost' IDENTIFIED BY 'fitpw';
GRANT ALL PRIVILEGES ON fitmanage.* TO 'fitmanage'@'localhost';
FLUSH PRIVILEGES;
```

You do not run the SQL scripts by hand. The app applies `src/main/resources/sql/01_schema.sql`, `02_seed.sql`, and `03_trainer_demographics.sql` the first time it connects and the tables are missing.

### 3. Point the app at that database

Defaults in `src/main/resources/application.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/fitmanage?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
db.user=fitmanage
db.password=fitpw
```

Change the password before you share a machine or put this on a network. The checked-in password is for a local demo only. After you change it in MySQL, change `db.password` to the same value.

Gym name, theme, backup folder, and idle timeout in that file are the defaults. Anything you change inside the app is written to `~/.fitmanage/application.properties`, which wins on the next launch.

### 4. Build

From the repository root, with JDK 17 on `PATH`:

```bash
mvn clean package
```

Maven writes `target/fitmanage-1.0.0.jar`. That jar contains the app and its libraries.

### 5. Run

```bash
java -jar target/fitmanage-1.0.0.jar
```

If `java -version` is not 17, call the JDK 17 binary directly. With Homebrew:

```bash
"$(brew --prefix openjdk@17)/bin/java" -jar target/fitmanage-1.0.0.jar
```

A login window opens. The health server also starts on `127.0.0.1:8080`. Only one copy of the app can bind that port. Quit the running window before you start another.

### 6. Sign in

These accounts are created on first launch. Passwords are hashed with jBCrypt at startup. They are not stored in the seed file.

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `staff1` | `staff123` | STAFF |
| `trainer1` | `train123` | TRAINER |

Change these passwords from **Users** (as admin) before anyone else uses the database.

## What you can do after login

1. **Dashboard.** Counts for members, plans ending soon, today’s check-ins, and recent payments. **Record payment** opens the payment form.
2. **Members.** **Add member** saves a profile. Date of birth defaults to 25 years ago. Leave email blank if you do not have one. Open a member for their plan, payments, attendance, progress, and activity.
3. **Plans on a member.** **Add plan** when they have no plan covering today. **Renew** schedules the next period after the current end date. **Upgrade** replaces the current plan with another active plan. **Cancel** marks the current plan cancelled. **Reactivate** puts that plan back when the end date is still today or later and no other plan covers today.
4. **Payments.** **Record payment** on the payments screen, the member page, or the dashboard. **Print** writes a PDF receipt.
5. **Attendance.** Check in with a phone number or member code. **Export CSV** writes today’s rows. A check-in requires a plan that covers today.
6. **Trainers.** Add a trainer, open their overview, and use **Record payout** on the salary page.
7. **Reports.** Pick a range and read revenue and attendance.
8. **Settings → Database.** **Backup now** runs `mysqldump` into `~/fitmanage-backups`. **Restore selected** runs `mysql` against the chosen dump. Both commands must be on your `PATH`, or set `app.mysqldump.path` in `application.properties`.

The top-right search accepts a name, phone, or member code. One match opens that member. Several matches open the members list already filtered.

Lists for members, plans, users, and payments filter as you type.

## Project layout

```
src/main/java/com/ironpump/fitmanage/
  App.java                 entry point
  auth/                    login, lockout, session
  config/                  application.properties loader
  dao/                     JDBC access (PreparedStatement only)
  db/                      connection pool and schema setup
  exception/               FitManageException, DatabaseException, ValidationException
  model/                   Member, Plan, Trainer, Payment, User, Membership
  service/                 business rules used by the screens
  ui/
    login/                 login window
    main/                  main window, sidebar, top search
    dashboard/             home
    members/               list, form, detail, memberships, payments, attendance, progress, activity
    trainers/              list, form, overview, personal, members, salary, activity
    plans/                 list and form
    attendance/            check-in
    payments/              list and record-payment dialog
    reports/               reports
    users/                 staff accounts
    settings/              profile, preferences, gym, database, about
    router/                30 routes and the back stack
    components/            tables, badges, charts, search field
    theme/                 FlatLaf dark and light
    error/                 friendly error dialog
  util/                    validation, CSV, PDF receipts, health server
src/main/resources/
  application.properties  database URL, gym defaults, health port
  sql/                     schema, seed, and small migrations
assets/ui-mockups/         screen references
```

There are 30 routes in `ui/router/Route.java`. Dialogs such as record-payment and choose-plan are not routes.

## Configuration reference

| Key | Default | Meaning |
|---|---|---|
| `db.url` | `jdbc:mysql://localhost:3306/fitmanage?...&serverTimezone=Asia/Kolkata` | JDBC URL |
| `db.user` | `fitmanage` | MySQL user |
| `db.password` | `fitpw` | MySQL password. Change this outside a local demo |
| `app.theme` | `dark` | `dark` or `light` |
| `app.gym.name` | `Iron Pump Gym` | Name printed on receipts |
| `app.backup.dir` | `${user.home}/fitmanage-backups` | Where SQL dumps are written |
| `app.mysqldump.path` | `mysqldump` | Backup program |
| `app.idle.timeout.minutes` | `30` | Idle time before the session ends |
| `app.currency` | `INR` | Currency label |
| `app.health.port` | `8080` | Health server port |
| `app.health.bind` | `127.0.0.1` | Health server address. It is not exposed on the network |

## Tests

Unit tests do not need MySQL. They use H2 in MySQL compatibility mode.

```bash
mvn test
```

That runs 7 tests: 5 in `ValidatorSmokeTest` and 2 in `DaoSmokeTest`.

## Continuous integration

`.github/workflows/build.yml` runs on pushes and pull requests to `main` and `develop`. The job uses Temurin JDK 17, runs `mvn package` and `mvn test`, and uploads `target/fitmanage-1.0.0.jar`. A MySQL 8 service container is started for the job. The unit tests themselves still run on H2.

## How errors are shown

Data access throws `DatabaseException`. Services throw `FitManageException`, which carries a short message for the screen. Anything that escapes a button is caught by `GlobalExceptionHandler`, installed before the rest of `App.main` runs. The dialog shows that message and a way to copy technical details. The screen does not print a raw stack trace.

## Contributing

1. Fork the repository and create a branch from `main`.
2. Keep changes small and match the style of the surrounding Java.
3. Do not concatenate SQL. Use `PreparedStatement` parameters.
4. Do not log passwords or password hashes.
5. Run `mvn test` before you open a pull request.
6. Describe what a user can do after the change, and how you checked it.

There is no `LICENSE` file in the repository yet. Add one before you treat the code as reusable by others. GitHub will show the project as “no license” until that file exists.

## Troubleshooting

| What you see | What to do |
|---|---|
| `UnsupportedClassVersionError` or “class file version 61.0” | `java -version` is older than 17. Run the jar with a JDK 17 binary. |
| Login window never appears and the log says the database is down | MySQL is not running, or `db.url`, `db.user`, or `db.password` do not match the server. |
| `Communications link failure` | Nothing is listening on port 3306, or Docker has not finished starting. |
| Port 8080 already in use | Another FitManage window is still open. Quit it, then start again. |
| Backup fails | `mysqldump` is not on `PATH`. Set `app.mysqldump.path` to the full path of the binary. |
| Settings you changed in the app came back | `~/.fitmanage/application.properties` overrides the file inside the jar. Edit or delete that file to return to the repository defaults. |
