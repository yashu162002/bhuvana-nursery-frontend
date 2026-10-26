INSERT INTO plant_categories (
    name_kn,
    name_en,
    slug
) VALUES
('ಹಣ್ಣಿನ ಸಸಿಗಳು', 'Fruit Plants', 'fruit-plants'),
('ಕೃಷಿ ಸಸಿಗಳು', 'Agricultural Plants', 'agricultural-plants'),
('ಮರಗಳು', 'Trees and Shade Plants', 'trees'),
('ನೈಸರ್ಗಿಕ ಗೊಬ್ಬರ', 'Natural Manure', 'natural-manure');

INSERT INTO plants (
    category_id,
    name_kn,
    name_en,
    slug,
    description_kn,
    description_en,
    price,
    enquiry_only,
    availability,
    published,
    featured
) VALUES
(
    (SELECT id FROM plant_categories WHERE slug = 'agricultural-plants'),
    'ಅಡಿಕೆ',
    'Arecanut',
    'arecanut',
    'ಅಡಿಕೆ ಸಸಿಗಳ ಬಗ್ಗೆ ವಿಚಾರಿಸಿ.',
    'Enquire about arecanut plants.',
    NULL,
    TRUE,
    'UNKNOWN',
    TRUE,
    TRUE
),
(
    (SELECT id FROM plant_categories WHERE slug = 'agricultural-plants'),
    'ತೆಂಗು',
    'Coconut',
    'coconut',
    'ತೆಂಗಿನ ಸಸಿಗಳ ಬಗ್ಗೆ ವಿಚಾರಿಸಿ.',
    'Enquire about coconut plants.',
    NULL,
    TRUE,
    'UNKNOWN',
    TRUE,
    TRUE
),
(
    (SELECT id FROM plant_categories WHERE slug = 'trees'),
    'ಸಾಗುವಾನಿ / ಸಾಗಸೆ',
    'Teak / local variety to verify',
    'teak-local-variety',
    'ಸ್ಥಳೀಯ ಸಸ್ಯದ ಹೆಸರು ಮತ್ತು ಪ್ರಭೇದವನ್ನು ಖಚಿತಪಡಿಸಿಕೊಳ್ಳಿ.',
    'Verify the local plant name and species.',
    NULL,
    TRUE,
    'UNKNOWN',
    FALSE,
    FALSE
),
(
    (SELECT id FROM plant_categories WHERE slug = 'fruit-plants'),
    'ಕಸಿ ಮಾವು',
    'Grafted Mango',
    'grafted-mango',
    'ಕಸಿ ಮಾವಿನ ಸಸಿಗಳ ಬಗ್ಗೆ ವಿಚಾರಿಸಿ.',
    'Enquire about grafted mango plants.',
    NULL,
    TRUE,
    'UNKNOWN',
    TRUE,
    TRUE
),
(
    (SELECT id FROM plant_categories WHERE slug = 'fruit-plants'),
    'ನೇರಳೆ',
    'Jamun',
    'jamun',
    'ನೇರಳೆ ಸಸಿಗಳ ಬಗ್ಗೆ ವಿಚಾರಿಸಿ.',
    'Enquire about jamun plants.',
    NULL,
    TRUE,
    'UNKNOWN',
    TRUE,
    FALSE
),
(
    (SELECT id FROM plant_categories WHERE slug = 'natural-manure'),
    'ಸಗಣಿ ಗೊಬ್ಬರ',
    'Farmyard Manure',
    'farmyard-manure',
    'ಗೊಬ್ಬರದ ಲಭ್ಯತೆಯನ್ನು ವಿಚಾರಿಸಿ.',
    'Enquire about manure availability.',
    NULL,
    TRUE,
    'UNKNOWN',
    TRUE,
    FALSE
),
(
    (SELECT id FROM plant_categories WHERE slug = 'natural-manure'),
    'ಜೀವಾಮೃತ',
    'Jeevamrutha',
    'jeevamrutha',
    'ಜೀವಾಮೃತದ ಲಭ್ಯತೆಯನ್ನು ವಿಚಾರಿಸಿ.',
    'Enquire about Jeevamrutha availability.',
    NULL,
    TRUE,
    'UNKNOWN',
    TRUE,
    FALSE
);
