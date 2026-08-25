ALTER TABLE users
ADD COLUMN role TEXT NOT NULL DEFAULT 'owner'
CHECK (role IN ('owner', 'admin'));

CREATE UNIQUE INDEX idx_users_singleton_admin
  ON users(role)
  WHERE role = 'admin';
