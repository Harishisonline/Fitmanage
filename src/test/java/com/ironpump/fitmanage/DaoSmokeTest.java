package com.ironpump.fitmanage;

import com.ironpump.fitmanage.db.DatabaseManager;
import com.ironpump.fitmanage.model.Plan;
import com.ironpump.fitmanage.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the DAO + auth layer using H2 in MySQL-compat mode.
 * Verifies the wiring without needing a real MySQL server.
 */
class DaoSmokeTest {

    private static final String H2_DDL = """
            DROP TABLE IF EXISTS attendance, payments, progress_logs, trainer_payments, memberships,
                                  members, plans, trainers, users, activity_log;
            CREATE TABLE users (
                user_id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(40) NOT NULL UNIQUE,
                password_hash VARCHAR(72) NOT NULL,
                display_name VARCHAR(80) NOT NULL,
                role VARCHAR(20) NOT NULL,
                linked_trainer_id INT NULL,
                email VARCHAR(120) NULL,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                failed_attempts INT NOT NULL DEFAULT 0,
                locked_until TIMESTAMP NULL,
                last_login_at TIMESTAMP NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            CREATE TABLE plans (
                plan_id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(60) NOT NULL UNIQUE,
                duration_days INT NOT NULL,
                price DECIMAL(10,2) NOT NULL,
                description TEXT,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            CREATE TABLE trainers (
                trainer_id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(80) NOT NULL,
                phone VARCHAR(20) NOT NULL UNIQUE,
                email VARCHAR(120) NULL UNIQUE,
                specialization VARCHAR(120) NULL,
                hire_date DATE NOT NULL,
                join_date DATE NULL,
                availability TEXT NULL,
                salary DECIMAL(10,2) NOT NULL DEFAULT 0,
                notes TEXT,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;

    @BeforeAll
    static void setup() throws Exception {
        // Each test class gets its own in-memory H2 DB
        String url = "jdbc:h2:mem:fitmanage_test_" + System.nanoTime()
                   + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        DatabaseManager.initWithDriver(url, "sa", "", "org.h2.Driver");
        try (Connection c = DatabaseManager.get(); Statement s = c.createStatement()) {
            for (String stmt : H2_DDL.split(";")) {
                String t = stmt.trim();
                if (!t.isEmpty()) s.execute(t);
            }
        }
    }

    @Test
    void planDao_insert_and_findById_works() {
        Plan p = new Plan();
        p.setName("Monthly");
        p.setDurationDays(30);
        p.setPrice(new BigDecimal("1500.00"));
        p.setDescription("Standard month-to-month");
        p.setActive(true);
        int id = new com.ironpump.fitmanage.dao.PlanDao().insert(p);
        assertThat(id).isPositive();

        Optional<Plan> fetched = new com.ironpump.fitmanage.dao.PlanDao().findById(id);
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("Monthly");
        assertThat(fetched.get().getPrice()).isEqualByComparingTo("1500.00");
    }

    @Test
    void userDao_insert_and_authenticate_works() {
        User u = new User();
        u.setUsername("alice");
        u.setPasswordHash(com.ironpump.fitmanage.auth.PasswordHasher.hash("alicepass"));
        u.setDisplayName("Alice Admin");
        u.setRole(User.Role.ADMIN);
        u.setActive(true);
        int id = new com.ironpump.fitmanage.dao.UserDao().insert(u);
        assertThat(id).isPositive();

        Optional<User> fetched = new com.ironpump.fitmanage.dao.UserDao().findByUsername("alice");
        assertThat(fetched).isPresent();
        assertThat(com.ironpump.fitmanage.auth.PasswordHasher.verify("alicepass", fetched.get().getPasswordHash())).isTrue();
        assertThat(com.ironpump.fitmanage.auth.PasswordHasher.verify("wrong", fetched.get().getPasswordHash())).isFalse();
    }
}
