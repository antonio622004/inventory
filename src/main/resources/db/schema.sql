CREATE TABLE IF NOT EXISTS inventory (
    product_id TEXT PRIMARY KEY,
    quantity INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS reserved_inventory (
    reservation_id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id TEXT NOT NULL,
    product_id TEXT NOT NULL,
    customer_id TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    UNIQUE(order_id, product_id)
);

DELETE FROM reserved_inventory;

INSERT OR REPLACE INTO inventory (product_id, quantity) VALUES ('P-8821', 5);
INSERT OR REPLACE INTO inventory (product_id, quantity) VALUES ('P-3344', 5);
