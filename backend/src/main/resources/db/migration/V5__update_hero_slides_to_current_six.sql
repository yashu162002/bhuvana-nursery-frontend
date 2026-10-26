ALTER TABLE hero_slides ADD COLUMN IF NOT EXISTS title_kn VARCHAR(200);
ALTER TABLE hero_slides ADD COLUMN IF NOT EXISTS title_en VARCHAR(200);

-- Clear previous test slides
DELETE FROM hero_slides;

-- Seed the 6 continuous slideshow images currently running in the hero section
INSERT INTO hero_slides (image_url, alt_text, title_kn, title_en, display_order, active) VALUES
('https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=2400&q=80', 'The green canopy', 'ಹಸಿರು ಛತ್ರ', 'The green canopy', 1, TRUE),
('https://images.unsplash.com/photo-1416879595882-3373a0480b5b?auto=format&fit=crop&w=2400&q=80', 'New beginnings', 'ಹೊಸ ಚಿಗುರು', 'New beginnings', 2, TRUE),
('https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=2400&q=80', 'Woods of light', 'ಬೆಳಕಿನ ಕಾನನ', 'Woods of light', 3, TRUE),
('https://images.unsplash.com/photo-1530836369250-ef72a3f5cda8?auto=format&fit=crop&w=2400&q=80', 'Grown with care', 'ಕಾಳಜಿಯಿಂದ ಬೆಳೆದದ್ದು', 'Grown with care', 4, TRUE),
('https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=2400&q=80', 'The path to your garden', 'ನಿಮ್ಮ ತೋಟದ ಹಾದಿ', 'The path to your garden', 5, TRUE),
('https://images.unsplash.com/photo-1463936575829-25148e1db1b8?auto=format&fit=crop&w=2400&q=80', 'Botanical beauty', 'ಸಸ್ಯ ಸೊಬಗು', 'Botanical beauty', 6, TRUE);
