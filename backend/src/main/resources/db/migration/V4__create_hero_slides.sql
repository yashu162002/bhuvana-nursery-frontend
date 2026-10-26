CREATE TABLE hero_slides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    image_url VARCHAR(2000) NOT NULL,
    alt_text VARCHAR(300),
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hero_slides_active_order ON hero_slides(active, display_order, created_at ASC);

INSERT INTO hero_slides (image_url, alt_text, display_order, active) VALUES
('https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=1600&q=90', 'Lush green forest landscape', 1, TRUE),
('https://images.unsplash.com/photo-1416879595882-3373a0480b5b?auto=format&fit=crop&w=1600&q=90', 'Young plants in pots', 2, TRUE),
('https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=1600&q=90', 'Greenhouse with plants', 3, TRUE),
('https://images.unsplash.com/photo-1523348837708-15d4a09cfac2?auto=format&fit=crop&w=1600&q=90', 'Monstera leaves close-up', 4, TRUE),
('https://images.unsplash.com/photo-1509937528035-ad76254b0356?auto=format&fit=crop&w=1600&q=90', 'Garden nursery rows', 5, TRUE);
