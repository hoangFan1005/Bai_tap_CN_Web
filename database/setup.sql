CREATE DATABASE IF NOT EXISTS rally_sport CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rally_sport;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(120) CHARACTER SET ascii NOT NULL,
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER'
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(140) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    price DECIMAL(12,0) NOT NULL CHECK (price > 0),
    image_path VARCHAR(128) CHARACTER SET ascii NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_variants (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku VARCHAR(48) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL UNIQUE,
    size_label VARCHAR(32) NOT NULL,
    stock INT NOT NULL CHECK (stock >= 0),
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(120) NOT NULL,
    phone VARCHAR(24) NOT NULL,
    address VARCHAR(350) NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    total DECIMAL(16,0) NOT NULL CHECK (total >= 0),
    checkout_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX ix_orders_user_id (user_id, id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    product_name VARCHAR(140) NOT NULL,
    sku VARCHAR(48) CHARACTER SET ascii NOT NULL,
    size_label VARCHAR(32) NOT NULL,
    unit_price DECIMAL(12,0) NOT NULL CHECK (unit_price > 0),
    quantity INT NOT NULL CHECK (quantity > 0),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_item_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    UNIQUE KEY uq_order_variant (order_id, variant_id)
) ENGINE=InnoDB;

INSERT IGNORE INTO categories (id, name) VALUES
    (1, 'Vợt cầu lông'), (2, 'Cầu lông'), (3, 'Trang phục'), (4, 'Lưới cầu lông'), (5, 'Quấn cán');

INSERT IGNORE INTO products (id, category_id, name, brand, description, price, image_path) VALUES
    (1, 1, 'Vợt Rally Aero 88', 'Rally', 'Vợt carbon 4U cho lối đánh nhanh. Điểm cân bằng trung bình phù hợp với cả đánh đơn và đánh đôi.', 1490000, 'racket.webp'),
    (2, 2, 'Cầu lông Rally Flight 12', 'Rally', 'Ống 12 quả cầu lông vũ. Sử dụng cho các buổi tập và thi đấu phong trào trong nhà.', 275000, 'shuttlecock.webp'),
    (3, 3, 'Áo cầu lông Rally Serve', 'Rally', 'Áo tập cổ tròn, chất liệu nhẹ và thoát mồ hôi. Có các cỡ S, M, L và XL.', 345000, 'apparel.webp'),
    (4, 4, 'Lưới cầu lông Rally Court 6,1 m', 'Rally', 'Lưới tập dài 6,1 m có viền trắng, phù hợp cho sân cầu lông trong nhà.', 485000, 'net.webp'),
    (5, 5, 'Quấn cán Rally Grip', 'Rally', 'Bộ ba cuộn quấn cán mềm, có vân bề mặt để tăng độ bám tay khi vận động.', 89000, 'grip.webp'),
    (6, 1, 'Vợt Rally Control 5', 'Rally', 'Vợt cân bằng nhẹ đầu cho các pha đỡ cầu nhanh và điều cầu sát lưới.', 990000, 'racket.webp'),
    (7, 2, 'Cầu lông Rally Club 6', 'Rally', 'Ống 6 quả cầu lông vũ dành cho tập luyện và chơi giải trí.', 145000, 'shuttlecock.webp'),
    (8, 3, 'Áo tập Rally Court Air', 'Rally', 'Áo tập kiểu dáng thoải mái, co giãn khi vung vợt và dễ giặt.', 320000, 'apparel.webp');

INSERT IGNORE INTO product_variants (id, product_id, sku, size_label, stock) VALUES
    (1, 1, 'AERO-88', '', 12),
    (2, 2, 'FLIGHT-12', '', 40),
    (3, 3, 'SERVE-S', 'S', 14), (4, 3, 'SERVE-M', 'M', 18),
    (5, 3, 'SERVE-L', 'L', 16), (6, 3, 'SERVE-XL', 'XL', 10),
    (7, 4, 'COURT-NET', '', 8),
    (8, 5, 'RALLY-GRIP', '', 32),
    (9, 6, 'CONTROL-5', '', 11),
    (10, 7, 'CLUB-6', '', 24),
    (11, 8, 'COURT-AIR-M', 'M', 12), (12, 8, 'COURT-AIR-L', 'L', 12);
