-- Normalized, deduplicated, categorized campus locations
INSERT INTO campus_location (name, slug, latitude, longitude, category, priority, radius_meters, is_active, created_at)
VALUES
('C12 Hostel', 'c12-hostel', 28.451599776906153, 77.58661999330064, 'HOSTEL', 1, 220, true, CURRENT_TIMESTAMP()),
('Academic Block A', 'academic-block-a', 28.450241872381834, 77.58430264649076, 'ACADEMIC', 2, 130, true, CURRENT_TIMESTAMP()),
('Academic Block B', 'academic-block-b', 28.44972803386619, 77.58458991634929, 'ACADEMIC', 3, 130, true, CURRENT_TIMESTAMP()),
('Academic Block P', 'academic-block-p', 28.449702747396273, 77.58269745254604, 'ACADEMIC', 4, 130, true, CURRENT_TIMESTAMP()),
('Academic Block N', 'academic-block-n', 28.448941986256347, 77.58333425110972, 'ACADEMIC', 5, 130, true, CURRENT_TIMESTAMP()),
('LRC', 'lrc', 28.449330857170512, 77.58415092447675, 'ACADEMIC', 6, 120, true, CURRENT_TIMESTAMP()),
('Paid Mess', 'paid-mess', 28.44939990697039, 77.5838349483853, 'FOOD', 7, 100, true, CURRENT_TIMESTAMP()),
('Mess', 'mess', 28.450631902014926, 77.58616164476247, 'FOOD', 8, 100, true, CURRENT_TIMESTAMP()),
('H Block', 'h-block', 28.45071472138364, 77.58714904209646, 'HOSTEL', 9, 230, true, CURRENT_TIMESTAMP()),
('Sports Complex', 'sports-complex', 28.450305537505677, 77.58720193145975, 'SPORTS', 10, 270, true, CURRENT_TIMESTAMP()),
('Football Ground', 'football-ground', 28.449540960638178, 77.58646276637697, 'SPORTS', 11, 280, true, CURRENT_TIMESTAMP()),
('Dominos', 'dominos', 28.448941986256347, 77.58333425110972, 'FOOD', 12, 90, true, CURRENT_TIMESTAMP());