-- FitManage v1 -> v2 migration. Run this ONCE on any pre-existing v1 database
-- before launching v2 code. It is idempotent (safe to re-run).

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'linked_member_id');
SET @stmt := IF(@col_exists > 0,
  'ALTER TABLE users DROP FOREIGN KEY fk_users_member, DROP INDEX fk_users_member, CHANGE linked_member_id linked_trainer_id INT NULL, ADD CONSTRAINT fk_users_trainer FOREIGN KEY (linked_trainer_id) REFERENCES trainers(trainer_id) ON DELETE SET NULL',
  'SELECT ''users.linked_member_id already migrated''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity_log' AND COLUMN_NAME = 'trainer_id');
SET @stmt := IF(@col_exists = 0,
  'ALTER TABLE activity_log ADD COLUMN trainer_id INT NULL, ADD CONSTRAINT fk_al_trainer_v2 FOREIGN KEY (trainer_id) REFERENCES trainers(trainer_id) ON DELETE SET NULL',
  'SELECT ''activity_log.trainer_id already present''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @tbl_exists := (SELECT COUNT(*) FROM information_schema.TABLES
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'progress_logs');
SET @stmt := IF(@tbl_exists = 0,
  'CREATE TABLE progress_logs ( log_id INT AUTO_INCREMENT PRIMARY KEY, member_id INT NOT NULL, log_date DATE NOT NULL, weight_kg DECIMAL(5,2) NULL, chest_cm INT NULL, waist_cm INT NULL, body_fat_pct DECIMAL(4,2) NULL, notes VARCHAR(500) NULL, logged_by INT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, CONSTRAINT fk_pl_member_v2 FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE, CONSTRAINT fk_pl_user_v2 FOREIGN KEY (logged_by) REFERENCES users(user_id) ON DELETE SET NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4',
  'SELECT ''progress_logs already present''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'progress_logs' AND INDEX_NAME = 'idx_pl_member_v2');
SET @stmt := IF(@idx_exists = 0,
  'CREATE INDEX idx_pl_member_v2 ON progress_logs(member_id)',
  'SELECT ''idx_pl_member_v2 already present''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE users SET linked_trainer_id = 1 WHERE username = 'trainer1' AND linked_trainer_id IS NULL;

-- v2.0.0: new trainer fields
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trainers' AND COLUMN_NAME = 'join_date');
SET @stmt := IF(@col_exists = 0,
  'ALTER TABLE trainers ADD COLUMN join_date DATE NULL AFTER hire_date',
  'SELECT ''trainers.join_date already present''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trainers' AND COLUMN_NAME = 'availability');
SET @stmt := IF(@col_exists = 0,
  'ALTER TABLE trainers ADD COLUMN availability TEXT NULL AFTER join_date',
  'SELECT ''trainers.availability already present''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- v2.0.0: last_login_at on users
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'last_login_at');
SET @stmt := IF(@col_exists = 0,
  'ALTER TABLE users ADD COLUMN last_login_at DATETIME NULL AFTER locked_until',
  'SELECT ''users.last_login_at already present''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- trainer demographics + refundable payments
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trainers' AND COLUMN_NAME = 'dob');
SET @stmt := IF(@col_exists = 0,
  'ALTER TABLE trainers ADD COLUMN dob DATE NULL',
  'SELECT ''trainers.dob already present''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trainers' AND COLUMN_NAME = 'gender');
SET @stmt := IF(@col_exists = 0,
  'ALTER TABLE trainers ADD COLUMN gender ENUM(''MALE'',''FEMALE'',''OTHER'') NULL',
  'SELECT ''trainers.gender already present''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND COLUMN_NAME = 'payment_type');
SET @stmt := IF(@col_exists = 0,
  'ALTER TABLE payments ADD COLUMN payment_type ENUM(''PAYMENT'',''REFUND'') NOT NULL DEFAULT ''PAYMENT'' AFTER amount',
  'SELECT ''payments.payment_type already present''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @needs := (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
               WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'payments'
                 AND CONSTRAINT_NAME = 'chk_pay_amount' AND CHECK_CLAUSE NOT LIKE '%<>%');
SET @stmt := IF(@needs > 0, 'ALTER TABLE payments DROP CHECK chk_pay_amount', 'SELECT 1');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
             WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND CONSTRAINT_NAME = 'chk_pay_amount');
SET @stmt := IF(@has = 0, 'ALTER TABLE payments ADD CONSTRAINT chk_pay_amount CHECK (amount <> 0)', 'SELECT 1');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;
