-- Idempotent migration: trainer demographics + payment type / refund amounts.
-- Safe to re-run. Each block checks information_schema first.

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trainers' AND COLUMN_NAME = 'dob');
SET @stmt := IF(@col_exists = 0,
  'ALTER TABLE trainers ADD COLUMN dob DATE NULL AFTER join_date',
  'SELECT ''trainers.dob already present''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trainers' AND COLUMN_NAME = 'gender');
SET @stmt := IF(@col_exists = 0,
  'ALTER TABLE trainers ADD COLUMN gender ENUM(''MALE'',''FEMALE'',''OTHER'') NULL AFTER dob',
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
SET @stmt := IF(@needs > 0,
  'ALTER TABLE payments DROP CHECK chk_pay_amount',
  'SELECT ''chk_pay_amount already allows non-zero''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

SET @has := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
             WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND CONSTRAINT_NAME = 'chk_pay_amount');
SET @stmt := IF(@has = 0,
  'ALTER TABLE payments ADD CONSTRAINT chk_pay_amount CHECK (amount <> 0)',
  'SELECT ''chk_pay_amount present''');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;
