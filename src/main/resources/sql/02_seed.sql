-- FitManage seed data. The bcrypt hashes below are illustrative.
-- On first launch, SchemaInitializer runs `--seed` to regenerate them with real values
-- matching the passwords the operator types in.

INSERT INTO plans (name, duration_days, price, description) VALUES
  ('Monthly',       30,  1499.00, '1-month access, all hours'),
  ('Quarterly',     90,  3499.00, '3-month access, includes 1 PT session'),
  ('Half-yearly',  180,  5999.00, '6-month access, includes 2 PT sessions'),
  ('Annual',       365,  9999.00, '12-month access, includes 4 PT sessions + diet plan');

INSERT INTO trainers (name, phone, email, specialization, hire_date, join_date, dob, gender, salary) VALUES
  ('Karan Singh',  '+919899912345', 'karan@ironpump.in', 'Strength,HIIT,CrossFit,Olympic lifting', '2024-01-15', '2024-01-15', '1990-04-12', 'MALE',   45000.00),
  ('Priya Nair',  '+919876501234', 'priya@ironpump.in', 'Yoga,Mobility,Pilates',                  '2024-03-01', '2024-03-01', '1993-09-08', 'FEMALE', 40000.00),
  ('Rahul Verma', '+919876598765', 'rahul@ironpump.in', 'Cardio,Functional',                      '2025-06-10', '2025-06-10', '1995-01-22', 'MALE',   35000.00);

-- ILLUSTRATIVE hashes — SchemaInitializer regenerates these on first run
-- using BCrypt.hashpw(plain, BCrypt.gensalt(12)) for the password "admin123" / "staff123" / "train123".
-- Placeholders kept here so the file is parseable when SchemaInitializer has not yet run.
INSERT INTO users (username, password_hash, display_name, role, linked_trainer_id, email) VALUES
  ('admin',    '$2a$12$5v8pB8H0V4qf2bS5g.W7NuGd6yTmVnaGq1H/IcKhJaBpCvH1k5tLW', 'Arjun R.',  'ADMIN',   NULL, 'arjun@ironpump.in'),
  ('staff1',   '$2a$12$Y3h6m9Wf2k8a7nQ0PzZ9O.1q1d5S5b8u6m7X5aA6h4kG7w7e3sCpG', 'Sneha P.',  'STAFF',   NULL, 'sneha@ironpump.in'),
  ('trainer1', '$2a$12$rT9f8K2j5e1n6o3pQ4r5T6yU7i8O9p0A1s2D3f4G5h6J7k8L9z0X1y', 'Karan S.',  'TRAINER', 1,    'karan@ironpump.in');

INSERT INTO members (member_code, first_name, last_name, dob, gender, phone, email, address, emergency_name, emergency_phone, height_cm, weight_kg, status, joined_at) VALUES
  ('FM-000121', 'Priya',  'Sharma', '1996-08-14', 'FEMALE', '+919876543210', 'priya.sharma@example.com', 'Andheri West, Mumbai', 'Amit Sharma', '+919800011122', 162, 56.20, 'ACTIVE', '2026-03-15'),
  ('FM-000122', 'Anjali', 'Verma',  '1998-02-02', 'FEMALE', '+919811122233', 'anjali.verma@example.com', 'Bandra West, Mumbai',  'Ravi Verma',  '+919800011133', 158, 52.40, 'ACTIVE', '2026-01-10'),
  ('FM-000123', 'Rohit',  'Mehta',   '1992-11-20', 'MALE',   '+919822233344', 'rohit.mehta@example.com',  'Juhu, Mumbai',         'Neha Mehta',  '+919800011144', 175, 78.00, 'ACTIVE', '2025-06-01');

INSERT INTO memberships (member_id, plan_id, trainer_id, start_date, end_date, status) VALUES
  (1, 2, 1, '2026-07-08', '2026-10-07', 'ACTIVE'),
  (2, 1, 2, '2026-09-20', '2026-10-20', 'ACTIVE'),
  (3, 4, 1, '2026-01-01', '2026-12-31', 'ACTIVE');

INSERT INTO payments (receipt_no, member_id, membership_id, amount, payment_type, method, reference, payment_date, notes, created_by) VALUES
  ('FM-2026-00001', 1, 1, 3499.00, 'PAYMENT', 'UPI',  'UPI-PRIYA-01', '2026-07-08 09:30:00', 'Quarterly renewal', 1),
  ('FM-2026-00002', 2, 2, 1499.00, 'PAYMENT', 'CASH', NULL,            '2026-09-20 11:05:00', 'Monthly', 1);
