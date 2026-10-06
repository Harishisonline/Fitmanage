-- FitManage · MySQL 8.0+ schema
-- All tables use InnoDB, utf8mb4

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS activity_log;
DROP TABLE IF EXISTS progress_logs;
DROP TABLE IF EXISTS trainer_payments;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS memberships;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS members;
DROP TABLE IF EXISTS plans;
DROP TABLE IF EXISTS trainers;
SET FOREIGN_KEY_CHECKS = 1;

-- TRAINERS  (created first because users.linked_trainer_id FKs to it)
CREATE TABLE trainers (
  trainer_id     INT AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(80) NOT NULL,
  phone          VARCHAR(20) NOT NULL UNIQUE,
  email          VARCHAR(120) NULL UNIQUE,
  specialization VARCHAR(120) NULL,
  hire_date      DATE NOT NULL,
  join_date      DATE NULL,
  dob            DATE NULL,
  gender         ENUM('MALE','FEMALE','OTHER') NULL,
  availability   TEXT NULL,
  salary         DECIMAL(10,2) NOT NULL DEFAULT 0,
  notes          TEXT NULL,
  active         BOOLEAN NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_trainer_salary CHECK (salary >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_trainers_active ON trainers(active);

-- PLANS
CREATE TABLE plans (
  plan_id        INT AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(60) NOT NULL UNIQUE,
  duration_days  INT NOT NULL,
  price          DECIMAL(10,2) NOT NULL,
  description    VARCHAR(250) NULL,
  active         BOOLEAN NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_plan_duration CHECK (duration_days > 0),
  CONSTRAINT chk_plan_price    CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- MEMBERS
CREATE TABLE members (
  member_id      INT AUTO_INCREMENT PRIMARY KEY,
  member_code    VARCHAR(12) NOT NULL UNIQUE,
  first_name     VARCHAR(60) NOT NULL,
  last_name      VARCHAR(60) NOT NULL,
  dob            DATE NOT NULL,
  gender         ENUM('MALE','FEMALE','OTHER') NOT NULL,
  phone          VARCHAR(20) NOT NULL UNIQUE,
  email          VARCHAR(120) NULL UNIQUE,
  address        VARCHAR(250) NULL,
  emergency_name VARCHAR(80) NULL,
  emergency_phone VARCHAR(20) NULL,
  height_cm      INT NULL,
  weight_kg      DECIMAL(5,2) NULL,
  medical_notes  TEXT NULL,
  status         ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  joined_at      DATE NOT NULL,
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_member_height CHECK (height_cm IS NULL OR height_cm >= 50 AND height_cm <= 250),
  CONSTRAINT chk_member_weight CHECK (weight_kg IS NULL OR weight_kg >= 10 AND weight_kg <= 300)
  -- dob < CURDATE() is not enforceable as a CHECK in MySQL 8 (CURDATE is non-deterministic);
  -- the service layer enforces the same invariant via Validator.pastDate().
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_members_phone   ON members(phone);
CREATE INDEX idx_members_status  ON members(status);
CREATE INDEX idx_members_joined  ON members(joined_at);

-- USERS  (login accounts; FK to trainers so a TRAINER-role login can be linked to a trainer row)
CREATE TABLE users (
  user_id        INT AUTO_INCREMENT PRIMARY KEY,
  username       VARCHAR(40) NOT NULL UNIQUE,
  password_hash  VARCHAR(72) NOT NULL,
  display_name   VARCHAR(80) NOT NULL,
  role           ENUM('ADMIN','STAFF','TRAINER') NOT NULL,
  linked_trainer_id INT NULL,
  email          VARCHAR(120) NULL,
  active         BOOLEAN NOT NULL DEFAULT TRUE,
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until   DATETIME NULL,
  last_login_at  DATETIME NULL,
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_trainer FOREIGN KEY (linked_trainer_id) REFERENCES trainers(trainer_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- MEMBERSHIPS
CREATE TABLE memberships (
  membership_id  INT AUTO_INCREMENT PRIMARY KEY,
  member_id      INT NOT NULL,
  plan_id        INT NOT NULL,
  trainer_id     INT NULL,
  start_date     DATE NOT NULL,
  end_date       DATE NOT NULL,
  status         ENUM('ACTIVE','EXPIRED','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
  renewal_of_id  INT NULL,
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ms_member   FOREIGN KEY (member_id)   REFERENCES members(member_id)   ON DELETE CASCADE,
  CONSTRAINT fk_ms_plan     FOREIGN KEY (plan_id)     REFERENCES plans(plan_id),
  CONSTRAINT fk_ms_trainer  FOREIGN KEY (trainer_id)  REFERENCES trainers(trainer_id) ON DELETE SET NULL,
  CONSTRAINT fk_ms_renewal  FOREIGN KEY (renewal_of_id) REFERENCES memberships(membership_id) ON DELETE SET NULL,
  CONSTRAINT chk_ms_dates   CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_ms_member   ON memberships(member_id);
CREATE INDEX idx_ms_status   ON memberships(status);
CREATE INDEX idx_ms_enddate  ON memberships(end_date);
CREATE INDEX idx_ms_trainer  ON memberships(trainer_id);

-- ATTENDANCE
CREATE TABLE attendance (
  attendance_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id      INT NOT NULL,
  check_in_at    DATETIME NOT NULL,
  check_out_at   DATETIME NULL,
  trainer_id     INT NULL,
  notes          VARCHAR(250) NULL,
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_att_member  FOREIGN KEY (member_id)  REFERENCES members(member_id) ON DELETE CASCADE,
  CONSTRAINT fk_att_trainer FOREIGN KEY (trainer_id) REFERENCES trainers(trainer_id) ON DELETE SET NULL,
  CONSTRAINT chk_att_times CHECK (check_out_at IS NULL OR check_out_at >= check_in_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_att_member   ON attendance(member_id);
CREATE INDEX idx_att_checkin  ON attendance(check_in_at);

-- PAYMENTS
CREATE TABLE payments (
  payment_id     INT AUTO_INCREMENT PRIMARY KEY,
  receipt_no     VARCHAR(20) NOT NULL UNIQUE,
  member_id      INT NOT NULL,
  membership_id  INT NULL,
  amount         DECIMAL(10,2) NOT NULL,
  payment_type   ENUM('PAYMENT','REFUND') NOT NULL DEFAULT 'PAYMENT',
  method         ENUM('CASH','UPI','CARD','BANK_TRANSFER','CHEQUE') NOT NULL,
  reference      VARCHAR(80) NULL,
  payment_date   DATETIME NOT NULL,
  notes          VARCHAR(250) NULL,
  created_by     INT NOT NULL,
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pay_member  FOREIGN KEY (member_id)     REFERENCES members(member_id) ON DELETE CASCADE,
  CONSTRAINT fk_pay_ms      FOREIGN KEY (membership_id) REFERENCES memberships(membership_id) ON DELETE SET NULL,
  CONSTRAINT fk_pay_user    FOREIGN KEY (created_by)    REFERENCES users(user_id),
  CONSTRAINT chk_pay_amount CHECK (amount <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_pay_member  ON payments(member_id);
CREATE INDEX idx_pay_date    ON payments(payment_date);
CREATE INDEX idx_pay_method  ON payments(method);

-- TRAINER PAYMENTS
CREATE TABLE trainer_payments (
  trainer_payment_id INT AUTO_INCREMENT PRIMARY KEY,
  trainer_id    INT NOT NULL,
  amount        DECIMAL(10,2) NOT NULL,
  period_start  DATE NOT NULL,
  period_end    DATE NOT NULL,
  paid_on       DATE NOT NULL,
  method        ENUM('CASH','UPI','CARD','BANK_TRANSFER','CHEQUE') NOT NULL,
  reference     VARCHAR(80) NULL,
  notes         VARCHAR(250) NULL,
  created_by    INT NOT NULL,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tp_trainer FOREIGN KEY (trainer_id)  REFERENCES trainers(trainer_id) ON DELETE CASCADE,
  CONSTRAINT fk_tp_user    FOREIGN KEY (created_by)  REFERENCES users(user_id),
  CONSTRAINT chk_tp_amount CHECK (amount >= 0),
  CONSTRAINT chk_tp_period CHECK (period_end >= period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_tp_trainer ON trainer_payments(trainer_id);
CREATE INDEX idx_tp_period  ON trainer_payments(period_start, period_end);

-- PROGRESS LOGS
CREATE TABLE progress_logs (
  log_id     INT AUTO_INCREMENT PRIMARY KEY,
  member_id  INT NOT NULL,
  log_date   DATE NOT NULL,
  weight_kg  DECIMAL(5,2) NULL,
  chest_cm   INT NULL,
  waist_cm   INT NULL,
  body_fat_pct DECIMAL(4,2) NULL,
  notes      VARCHAR(500) NULL,
  logged_by  INT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pl_member FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE,
  CONSTRAINT fk_pl_user   FOREIGN KEY (logged_by) REFERENCES users(user_id)   ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_pl_member ON progress_logs(member_id);
CREATE INDEX idx_pl_date   ON progress_logs(log_date);

-- ACTIVITY LOG
CREATE TABLE activity_log (
  activity_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id        INT NULL,
  member_id      INT NULL,
  trainer_id     INT NULL,
  action         VARCHAR(40) NOT NULL,
  detail         VARCHAR(500) NULL,
  ip             VARCHAR(45) NULL,
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_al_user    FOREIGN KEY (user_id)    REFERENCES users(user_id)    ON DELETE SET NULL,
  CONSTRAINT fk_al_member  FOREIGN KEY (member_id)  REFERENCES members(member_id) ON DELETE SET NULL,
  CONSTRAINT fk_al_trainer FOREIGN KEY (trainer_id) REFERENCES trainers(trainer_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_al_created ON activity_log(created_at);
