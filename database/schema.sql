-- Lost & Found Management System

CREATE DATABASE IF NOT EXISTS lostfound
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE lostfound;

-- 1. Users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Categories
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE
);

-- 3. Locations
CREATE TABLE IF NOT EXISTS locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    campus VARCHAR(120) NOT NULL,
    building VARCHAR(120),
    room VARCHAR(120)
);

-- 4. Items
CREATE TABLE IF NOT EXISTS items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description TEXT,
    type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'FOUND',
    category_id INT NOT NULL,
    location_id INT,
    reporter_id INT NOT NULL,
    reported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    image_path VARCHAR(255),

    CONSTRAINT fk_items_category
        FOREIGN KEY (category_id) REFERENCES categories(id),

    CONSTRAINT fk_items_location
        FOREIGN KEY (location_id) REFERENCES locations(id),

    CONSTRAINT fk_items_reporter
        FOREIGN KEY (reporter_id) REFERENCES users(id),

    CONSTRAINT chk_items_type
        CHECK (type IN ('LOST', 'FOUND')),

    CONSTRAINT chk_items_status
        CHECK (status IN ('LOST', 'FOUND', 'CLAIMED', 'RETURNED', 'CLOSED'))
);

-- 5. Claims
CREATE TABLE IF NOT EXISTS claims (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    item_id INT NOT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at DATETIME,

    CONSTRAINT fk_claims_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_claims_item
        FOREIGN KEY (item_id) REFERENCES items(id)
);

-- 6. Moderation
CREATE TABLE IF NOT EXISTS moderation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    claim_id INT NOT NULL UNIQUE,
    moderator_id INT NOT NULL,
    decision VARCHAR(20) NOT NULL,
    comment TEXT,
    decided_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_moderation_claim
        FOREIGN KEY (claim_id) REFERENCES claims(id),

    CONSTRAINT fk_moderation_moderator
        FOREIGN KEY (moderator_id) REFERENCES users(id),

    CONSTRAINT chk_moderation_decision
        CHECK (decision IN ('APPROVED', 'REJECTED'))
);

-- 7. Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    text TEXT NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Indexes recommended in the technical notes
CREATE INDEX idx_items_status ON items(status);
CREATE INDEX idx_items_category ON items(category_id);
CREATE INDEX idx_claims_item ON claims(item_id);
CREATE INDEX idx_claims_user ON claims(user_id);
CREATE INDEX idx_items_status_cat ON items(status, category_id);
