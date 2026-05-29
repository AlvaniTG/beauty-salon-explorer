CREATE TABLE salons (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    district VARCHAR(255),
    phone_number VARCHAR(50),
    social_media_links JSONB,
    services_offered JSONB,
    price_range VARCHAR(10),
    rating NUMERIC(3, 2),
    reviews INT,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    is_manually_edited BOOLEAN DEFAULT FALSE,
    UNIQUE (name, address)
);