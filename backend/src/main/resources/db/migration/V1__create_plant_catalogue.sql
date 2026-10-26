CREATE TABLE plant_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_kn VARCHAR(150) NOT NULL,
    name_en VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    description_kn TEXT,
    description_en TEXT,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES plant_categories(id),
    name_kn VARCHAR(200) NOT NULL,
    name_en VARCHAR(200) NOT NULL,
    scientific_name VARCHAR(250),
    slug VARCHAR(220) NOT NULL UNIQUE,
    description_kn TEXT,
    description_en TEXT,
    price NUMERIC(12, 2),
    enquiry_only BOOLEAN NOT NULL DEFAULT TRUE,
    availability VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    published BOOLEAN NOT NULL DEFAULT FALSE,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_plant_price CHECK (price IS NULL OR price >= 0),
    CONSTRAINT chk_plant_availability CHECK (
        availability IN (
            'AVAILABLE',
            'OUT_OF_STOCK',
            'UNKNOWN'
        )
    ),
    CONSTRAINT chk_enquiry_price CHECK (enquiry_only OR price IS NOT NULL)
);

CREATE INDEX idx_plants_category ON plants(category_id);
CREATE INDEX idx_plants_public_listing ON plants(published, archived, created_at DESC);
CREATE INDEX idx_plants_availability ON plants(availability);
