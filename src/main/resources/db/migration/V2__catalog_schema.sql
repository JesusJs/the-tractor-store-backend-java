-- Catalog schema (spec 002, data-model.md §4). Only catalog_* tables, no foreign keys to other modules.
-- Types match the JPA mapping exactly for ddl-auto=validate: VARCHAR (not CHAR) and INTEGER (not SMALLINT).
-- Child "position" columns start at 0 and are contiguous (@OrderColumn).

CREATE TABLE catalog_category (
    code         VARCHAR(20)  PRIMARY KEY,
    brand_label  VARCHAR(50)  NOT NULL,
    teaser_title VARCHAR(100) NOT NULL,
    teaser_image VARCHAR(255) NOT NULL,
    position     INTEGER      NOT NULL UNIQUE
);

CREATE TABLE catalog_product (
    id            VARCHAR(20)  PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    category_code VARCHAR(20)  NOT NULL REFERENCES catalog_category (code),
    position      INTEGER      NOT NULL UNIQUE
);

CREATE INDEX catalog_product_category_code_idx ON catalog_product (category_code);

CREATE TABLE catalog_product_highlight (
    product_id VARCHAR(20)  NOT NULL REFERENCES catalog_product (id),
    position   INTEGER      NOT NULL CHECK (position >= 0),
    text       VARCHAR(255) NOT NULL,
    PRIMARY KEY (product_id, position)
);

CREATE TABLE catalog_variant (
    sku        VARCHAR(20)   PRIMARY KEY,
    product_id VARCHAR(20)   NOT NULL REFERENCES catalog_product (id),
    position   INTEGER       NOT NULL CHECK (position >= 0),
    name       VARCHAR(100)  NOT NULL,
    color      VARCHAR(7)    NOT NULL CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    price      NUMERIC(10,2) NOT NULL CHECK (price > 0),
    image      VARCHAR(255)  NOT NULL,
    UNIQUE (product_id, position)
);

CREATE TABLE catalog_store (
    id       VARCHAR(20)  PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    address  VARCHAR(150) NOT NULL,
    city     VARCHAR(100) NOT NULL,
    image    VARCHAR(255) NOT NULL,
    position INTEGER      NOT NULL UNIQUE
);
