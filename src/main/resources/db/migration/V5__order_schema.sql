-- Orders (spec 004, data-model.md §7). Only order_* tables: store_id and sku are copies of catalog
-- ids without foreign keys (constitution IV); the catalog validates them through catalog.api.

CREATE TABLE order_order (
    id            UUID          PRIMARY KEY,
    session_id    UUID          NOT NULL,
    first_name    VARCHAR(100)  NOT NULL,
    last_name     VARCHAR(100)  NOT NULL,
    store_id      VARCHAR(20)   NOT NULL,
    extra_pickups VARCHAR(500),
    sub_total     NUMERIC(10,2) NOT NULL CHECK (sub_total >= 0),
    tax           NUMERIC(10,2) NOT NULL CHECK (tax >= 0),
    total         NUMERIC(10,2) NOT NULL CHECK (total >= 0),
    status        VARCHAR(20)   NOT NULL CHECK (status IN ('PLACED')),
    placed_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_line (
    order_id     UUID          NOT NULL REFERENCES order_order (id),
    position     INTEGER       NOT NULL CHECK (position >= 0),
    sku          VARCHAR(20)   NOT NULL,
    product_id   VARCHAR(20)   NOT NULL,
    product_name VARCHAR(100)  NOT NULL,
    variant_name VARCHAR(100)  NOT NULL,
    unit_price   NUMERIC(10,2) NOT NULL CHECK (unit_price > 0),
    quantity     INTEGER       NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    image        VARCHAR(255)  NOT NULL,
    PRIMARY KEY (order_id, position)
);
