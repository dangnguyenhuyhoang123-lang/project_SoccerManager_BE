SET NAMES utf8mb4;

-- =========================================================
-- VIETNAM NATIONAL LEAGUES SAMPLE SEED
-- ---------------------------------------------------------
-- Real-world source basis:
-- 1) League system names: VFF national competitions page
-- 2) V.League 1 2025/26 participating clubs: VPF announcement 08/08/2025
-- 3) V.League 2 2025/26 participating clubs: VPF draw notice 11/09/2025
-- 4) Second Division 2025 active clubs: VFF official return-leg schedule page
-- 5) Third Division 2025 participating clubs: VFF draw notice 09/10/2025
--
-- Note:
-- - Club names and league names are seeded from official competition sources.
-- - Stadiums, coaches, players, rounds and matches below are sample/demo data
--   designed to fit the current backend schema and support API testing.
-- =========================================================

INSERT INTO system_rule (
    id, max_teams, min_age, max_age, min_players, max_players,
    win_points, draw_points, lose_points, allowed_goal_types,
    status, max_substitution, min_registration_players, max_foreign_players
) VALUES
    (1, 14, 16, 40, 18, 30, 3, 1, 0, 'NORMAL,OWN_GOAL,PENALTY', 'ACTIVE', 5, 18, 3),
    (2, 12, 16, 40, 18, 30, 3, 1, 0, 'NORMAL,OWN_GOAL,PENALTY', 'ACTIVE', 5, 18, 2),
    (3, 14, 16, 35, 18, 28, 3, 1, 0, 'NORMAL,OWN_GOAL,PENALTY', 'ACTIVE', 5, 18, 1),
    (4, 17, 16, 35, 18, 28, 3, 1, 0, 'NORMAL,OWN_GOAL,PENALTY', 'ACTIVE', 5, 18, 0);

INSERT INTO league (id, name, country, scale, status, logo) VALUES
    (1, 'Giải Vô địch Quốc gia (V.League 1)', 'Việt Nam', 'Quốc gia', 'ACTIVE', NULL),
    (2, 'Giải Hạng Nhất Quốc gia (V.League 2)', 'Việt Nam', 'Quốc gia', 'ACTIVE', NULL),
    (3, 'Giải Hạng Nhì Quốc gia', 'Việt Nam', 'Quốc gia', 'ACTIVE', NULL),
    (4, 'Giải Hạng Ba Quốc gia', 'Việt Nam', 'Quốc gia', 'ACTIVE', NULL);

INSERT INTO season (id, year, name, start_date, end_date, system_rule_id, league_id) VALUES
    (1, '2025/26', 'LPBank V.League 1 2025/26', '2025-08-15', '2026-06-20', 1, 1),
    (2, '2025/26', 'Giải Hạng Nhất Quốc gia 2025/26', '2025-09-19', '2026-06-15', 2, 2),
    (3, '2025', 'Giải Hạng Nhì Quốc gia 2025', '2025-04-11', '2025-06-22', 3, 3),
    (4, '2025', 'Giải Hạng Ba Quốc gia 2025', '2025-10-26', '2025-11-23', 4, 4);

INSERT INTO rounds (
    id, round_number, name, start_date, end_date, max_matches, status, notify_teams, season_id
) VALUES
    (1, 1, 'Vòng 1', '2025-08-15 18:00:00', '2025-08-18 22:00:00', 7, 'SCHEDULED', 1, 1),
    (2, 2, 'Vòng 2', '2025-08-22 18:00:00', '2025-08-25 22:00:00', 7, 'SCHEDULED', 1, 1),
    (3, 3, 'Vòng 3', '2025-08-29 18:00:00', '2025-09-01 22:00:00', 7, 'SCHEDULED', 1, 1),
    (4, 1, 'Vòng 1', '2025-09-19 15:00:00', '2025-09-21 22:00:00', 6, 'SCHEDULED', 1, 2),
    (5, 2, 'Vòng 2', '2025-09-26 15:00:00', '2025-09-28 22:00:00', 6, 'SCHEDULED', 1, 2),
    (6, 3, 'Vòng 3', '2025-10-03 15:00:00', '2025-10-05 22:00:00', 6, 'SCHEDULED', 1, 2),
    (7, 1, 'Lượt 1', '2025-04-11 15:00:00', '2025-04-13 22:00:00', 7, 'COMPLETED', 1, 3),
    (8, 2, 'Lượt 2', '2025-05-02 15:00:00', '2025-05-04 22:00:00', 7, 'COMPLETED', 1, 3),
    (9, 3, 'Lượt 3', '2025-06-01 15:00:00', '2025-06-03 22:00:00', 7, 'COMPLETED', 1, 3),
    (10, 1, 'Lượt 1', '2025-10-26 14:00:00', '2025-10-27 18:00:00', 8, 'COMPLETED', 1, 4),
    (11, 2, 'Lượt 2', '2025-11-02 14:00:00', '2025-11-03 18:00:00', 8, 'COMPLETED', 1, 4),
    (12, 3, 'Lượt 3', '2025-11-09 14:00:00', '2025-11-10 18:00:00', 8, 'COMPLETED', 1, 4);

INSERT INTO stadiums (id, name, address, capacity, grass) VALUES
    (1, 'Gò Đậu', 'Bình Dương', 18000, 'Standard'),
    (2, 'Hàng Đẫy', 'Hà Nội', 22000, 'Standard'),
    (3, 'Thống Nhất', 'TP. Hồ Chí Minh', 25000, 'Standard'),
    (4, 'Thanh Hóa', 'Thanh Hóa', 14000, 'Standard'),
    (5, 'Lạch Tray', 'Hải Phòng', 28000, 'Standard'),
    (6, 'Pleiku', 'Gia Lai', 12000, 'Standard'),
    (7, 'Hà Tĩnh', 'Hà Tĩnh', 20000, 'Standard'),
    (8, 'Ninh Bình', 'Ninh Bình', 22000, 'Standard'),
    (9, 'PVF', 'Hưng Yên', 5000, 'Standard'),
    (10, 'Hòa Xuân', 'Đà Nẵng', 20000, 'Standard'),
    (11, 'Vinh', 'Nghệ An', 18000, 'Standard'),
    (12, 'Thiên Trường', 'Nam Định', 30000, 'Standard'),
    (13, 'Mỹ Đình', 'Hà Nội', 40000, 'Standard'),
    (14, 'Từ Sơn', 'Bắc Ninh', 12000, 'Standard'),
    (15, 'Cao Lãnh', 'Đồng Tháp', 10000, 'Standard'),
    (16, '19 Tháng 8 Nha Trang', 'Khánh Hòa', 18000, 'Standard'),
    (17, 'Long An', 'Long An', 15000, 'Standard'),
    (18, 'Đồng Nai', 'Đồng Nai', 18000, 'Standard'),
    (19, 'Cẩm Phả', 'Quảng Ninh', 15000, 'Standard'),
    (20, 'Quy Nhơn', 'Bình Định', 20000, 'Standard'),
    (21, 'Việt Trì', 'Phú Thọ', 20000, 'Standard'),
    (22, 'Thanh Trì', 'Hà Nội', 5000, 'Synthetic'),
    (23, 'Kon Tum', 'Kon Tum', 8000, 'Standard'),
    (24, 'Buôn Ma Thuột', 'Đắk Lắk', 12000, 'Standard'),
    (25, 'Đà Lạt', 'Lâm Đồng', 7000, 'Standard'),
    (26, 'Vĩnh Long', 'Vĩnh Long', 8000, 'Standard'),
    (27, 'Tây Ninh', 'Tây Ninh', 12000, 'Standard'),
    (28, 'Tân Hiệp', 'TP. Hồ Chí Minh', 5000, 'Synthetic'),
    (29, 'Minh Khoa', 'Hà Nội', 5000, 'Synthetic'),
    (30, 'Hóc Môn', 'TP. Hồ Chí Minh', 4000, 'Synthetic'),
    (31, 'Quân khu 5', 'Đà Nẵng', 5000, 'Synthetic'),
    (32, 'Long Xuyên', 'An Giang', 12000, 'Standard');

INSERT INTO team (
    id, name, logo, established_year, city, region, owner, description, status, stadium_id
) VALUES
    (1, 'Becamex TP.Hồ Chí Minh', NULL, 1976, 'TP. Hồ Chí Minh', 'Nam', 'Becamex', 'CLB dự V.League 1 2025/26', 'ACTIVE', 1),
    (2, 'Công an Hà Nội', NULL, 1956, 'Hà Nội', 'Bắc', 'Công an Hà Nội', 'CLB dự V.League 1 2025/26', 'ACTIVE', 2),
    (3, 'Công an TP.HCM', NULL, 1975, 'TP. Hồ Chí Minh', 'Nam', 'Công an TP.HCM', 'CLB dự V.League 1 2025/26', 'ACTIVE', 3),
    (4, 'Đông Á Thanh Hóa', NULL, 1962, 'Thanh Hóa', 'Bắc Trung Bộ', 'Đông Á', 'CLB dự V.League 1 2025/26', 'ACTIVE', 4),
    (5, 'Hà Nội', NULL, 2006, 'Hà Nội', 'Bắc', 'T&T Group', 'CLB dự V.League 1 2025/26', 'ACTIVE', 2),
    (6, 'Hải Phòng', NULL, 1952, 'Hải Phòng', 'Bắc', 'Hải Phòng FC', 'CLB dự V.League 1 2025/26', 'ACTIVE', 5),
    (7, 'Hoàng Anh Gia Lai', NULL, 2001, 'Gia Lai', 'Tây Nguyên', 'Hoàng Anh Gia Lai', 'CLB dự V.League 1 2025/26', 'ACTIVE', 6),
    (8, 'Hồng Lĩnh Hà Tĩnh', NULL, 2019, 'Hà Tĩnh', 'Bắc Trung Bộ', 'Hồng Lĩnh', 'CLB dự V.League 1 2025/26', 'ACTIVE', 7),
    (9, 'Ninh Bình', NULL, 2024, 'Ninh Bình', 'Bắc', 'Ninh Bình FC', 'CLB dự V.League 1 2025/26', 'ACTIVE', 8),
    (10, 'PVF-CAND', NULL, 2008, 'Hưng Yên', 'Bắc', 'PVF-CAND', 'CLB dự V.League 1 2025/26', 'ACTIVE', 9),
    (11, 'SHB Đà Nẵng', NULL, 1976, 'Đà Nẵng', 'Miền Trung', 'SHB', 'CLB dự V.League 1 2025/26', 'ACTIVE', 10),
    (12, 'Sông Lam Nghệ An', NULL, 1979, 'Nghệ An', 'Bắc Trung Bộ', 'SLNA', 'CLB dự V.League 1 2025/26', 'ACTIVE', 11),
    (13, 'Thép Xanh Nam Định', NULL, 1965, 'Nam Định', 'Bắc', 'Thép Xanh', 'CLB dự V.League 1 2025/26', 'ACTIVE', 12),
    (14, 'Thể Công Viettel', NULL, 1954, 'Hà Nội', 'Bắc', 'Viettel', 'CLB dự V.League 1 2025/26', 'ACTIVE', 13),

    (15, 'Bắc Ninh', NULL, 2023, 'Bắc Ninh', 'Bắc', 'Bắc Ninh FC', 'CLB dự Hạng Nhất 2025/26', 'ACTIVE', 14),
    (16, 'Đại học Văn Hiến', NULL, 2023, 'TP. Hồ Chí Minh', 'Nam', 'Đại học Văn Hiến', 'CLB dự Hạng Nhất 2025/26', 'ACTIVE', 3),
    (17, 'Đồng Tháp', NULL, 1976, 'Đồng Tháp', 'Nam', 'Đồng Tháp FC', 'CLB dự Hạng Nhất 2025/26', 'ACTIVE', 15),
    (18, 'Khatoco Khánh Hòa', NULL, 1976, 'Khánh Hòa', 'Miền Trung', 'Khatoco', 'CLB dự Hạng Nhất 2025/26', 'ACTIVE', 16),
    (19, 'Long An', NULL, 2000, 'Long An', 'Nam', 'Long An FC', 'CLB dự Hạng Nhất 2025/26', 'ACTIVE', 17),
    (20, 'TP.Hồ Chí Minh', NULL, 2025, 'TP. Hồ Chí Minh', 'Nam', 'TP.HCM FC', 'CLB dự Hạng Nhất 2025/26', 'ACTIVE', 3),
    (21, 'Thanh Niên TP.Hồ Chí Minh', NULL, 2025, 'TP. Hồ Chí Minh', 'Nam', 'Thanh Niên TP.HCM', 'CLB dự Hạng Nhất 2025/26', 'ACTIVE', 3),
    (22, 'Trẻ PVF-CAND', NULL, 2021, 'Hưng Yên', 'Bắc', 'PVF-CAND', 'CLB dự Hạng Nhất 2025/26', 'ACTIVE', 9),
    (23, 'Trường Tươi Đồng Nai', NULL, 2025, 'Đồng Nai', 'Nam', 'Trường Tươi', 'CLB dự Hạng Nhất 2025/26', 'ACTIVE', 18),
    (24, 'Quảng Ninh', NULL, 2024, 'Quảng Ninh', 'Bắc', 'Quảng Ninh FC', 'CLB dự Hạng Nhất 2025/26', 'ACTIVE', 19),
    (25, 'Quy Nhơn United', NULL, 2025, 'Bình Định', 'Miền Trung', 'Quy Nhơn United', 'CLB dự Hạng Nhất 2025/26', 'ACTIVE', 20),
    (26, 'Xuân Thiện Phú Thọ', NULL, 2025, 'Phú Thọ', 'Bắc', 'Xuân Thiện', 'CLB dự Hạng Nhất 2025/26', 'ACTIVE', 21),

    (27, 'Hoài Đức', NULL, 2020, 'Hà Nội', 'Bắc', 'Hoài Đức FC', 'CLB dự Hạng Nhì 2025', 'ACTIVE', 22),
    (28, 'Trẻ Hà Nội', NULL, 2024, 'Hà Nội', 'Bắc', 'Hà Nội FC', 'CLB dự Hạng Nhì 2025', 'ACTIVE', 22),
    (29, 'PVF', NULL, 2009, 'Hưng Yên', 'Bắc', 'PVF', 'CLB dự Hạng Nhì 2025', 'ACTIVE', 9),
    (30, 'Trẻ SHB Đà Nẵng', NULL, 2024, 'Đà Nẵng', 'Miền Trung', 'SHB Đà Nẵng', 'CLB dự Hạng Nhì 2025', 'ACTIVE', 31),
    (31, 'Kon Tum', NULL, 2020, 'Kon Tum', 'Tây Nguyên', 'Kon Tum FC', 'CLB dự Hạng Nhì 2025', 'ACTIVE', 23),
    (32, 'Đắk Lắk', NULL, 2006, 'Đắk Lắk', 'Tây Nguyên', 'Đắk Lắk FC', 'CLB dự Hạng Nhì 2025', 'ACTIVE', 24),
    (33, 'Lâm Đồng', NULL, 2010, 'Lâm Đồng', 'Tây Nguyên', 'Lâm Đồng FC', 'CLB dự Hạng Nhì 2025', 'ACTIVE', 25),
    (34, 'Vĩnh Long', NULL, 2010, 'Vĩnh Long', 'Nam', 'Vĩnh Long FC', 'CLB dự Hạng Nhì 2025', 'ACTIVE', 26),
    (35, 'Tây Ninh', NULL, 2010, 'Tây Ninh', 'Nam', 'Tây Ninh FC', 'CLB dự Hạng Nhì 2025', 'ACTIVE', 27),
    (36, 'Gia Định', NULL, 2016, 'TP. Hồ Chí Minh', 'Nam', 'Gia Định FC', 'CLB dự Hạng Nhì 2025', 'ACTIVE', 28),

    (37, 'An Giang', NULL, 1976, 'An Giang', 'Nam', 'An Giang FC', 'CLB dự Hạng Ba 2025', 'ACTIVE', 32),
    (38, 'Đồng Nai', NULL, 1980, 'Đồng Nai', 'Nam', 'Đồng Nai FC', 'CLB dự Hạng Ba 2025', 'ACTIVE', 18),
    (39, 'Hà Nội Bulls', NULL, 2025, 'Hà Nội', 'Bắc', 'Hà Nội Bulls', 'CLB dự Hạng Ba 2025', 'ACTIVE', 29),
    (40, 'Trẻ Hoài Đức', NULL, 2025, 'Hà Nội', 'Bắc', 'Hoài Đức FC', 'CLB dự Hạng Ba 2025', 'ACTIVE', 22),
    (41, 'Luxury Hạ Long', NULL, 2025, 'Quảng Ninh', 'Bắc', 'Luxury Hạ Long', 'CLB dự Hạng Ba 2025', 'ACTIVE', 19),
    (42, 'Phù Đổng FC', NULL, 2015, 'Hà Nội', 'Bắc', 'Phù Đổng FC', 'CLB dự Hạng Ba 2025', 'ACTIVE', 29),
    (43, 'STP FOOD TP.HCM', NULL, 2025, 'TP. Hồ Chí Minh', 'Nam', 'STP FOOD', 'CLB dự Hạng Ba 2025', 'ACTIVE', 30),
    (44, 'Trẻ Becamex TP.HCM', NULL, 2025, 'TP. Hồ Chí Minh', 'Nam', 'Becamex', 'CLB dự Hạng Ba 2025', 'ACTIVE', 1),
    (45, 'Trẻ Công an Hà Nội', NULL, 2025, 'Hà Nội', 'Bắc', 'Công an Hà Nội', 'CLB dự Hạng Ba 2025', 'ACTIVE', 29),
    (46, 'Trẻ Đắk Lắk', NULL, 2025, 'Đắk Lắk', 'Tây Nguyên', 'Đắk Lắk FC', 'CLB dự Hạng Ba 2025', 'ACTIVE', 24),
    (47, 'Trẻ Thống Nhất TPG', NULL, 2025, 'TP. Hồ Chí Minh', 'Nam', 'Thống Nhất TPG', 'CLB dự Hạng Ba 2025', 'ACTIVE', 30),
    (48, 'Trung tâm Bóng đá Đào Hà', NULL, 2025, 'Hà Nội', 'Bắc', 'Đào Hà', 'CLB dự Hạng Ba 2025', 'ACTIVE', 29),
    (49, 'TTHL&TĐ TDTT Hà Tĩnh', NULL, 2025, 'Hà Tĩnh', 'Bắc Trung Bộ', 'TDTT Hà Tĩnh', 'CLB dự Hạng Ba 2025', 'ACTIVE', 7),
    (50, 'TTHLKTTT Khánh Hòa', NULL, 2025, 'Khánh Hòa', 'Miền Trung', 'Khánh Hòa', 'CLB dự Hạng Ba 2025', 'ACTIVE', 16),
    (51, 'Trường ĐH Công nghệ Đồng Nai', NULL, 2025, 'Đồng Nai', 'Nam', 'ĐH Công nghệ Đồng Nai', 'CLB dự Hạng Ba 2025', 'ACTIVE', 18),
    (52, 'Trường Giang Gia Định', NULL, 2025, 'TP. Hồ Chí Minh', 'Nam', 'Trường Giang', 'CLB dự Hạng Ba 2025', 'ACTIVE', 30),
    (53, 'Trẻ Đồng Tháp', NULL, 2025, 'Đồng Tháp', 'Nam', 'Đồng Tháp FC', 'CLB dự Hạng Ba 2025', 'ACTIVE', 15);

INSERT INTO coach (id, name, nationality, birth_day, status)
SELECT
    t.id,
    CONCAT('HLV ', t.name),
    'Việt Nam',
    DATE_ADD('1970-01-01', INTERVAL (20 + t.id) YEAR),
    'ACTIVE'
FROM team t
ORDER BY t.id;

INSERT INTO season_team (id, team_id, season_id, notes, status) VALUES
    (1, 1, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (2, 2, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (3, 3, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (4, 4, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (5, 5, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (6, 6, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (7, 7, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (8, 8, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (9, 9, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (10, 10, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (11, 11, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (12, 12, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (13, 13, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (14, 14, 1, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),

    (15, 15, 2, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (16, 16, 2, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (17, 17, 2, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (18, 18, 2, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (19, 19, 2, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (20, 20, 2, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (21, 21, 2, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (22, 22, 2, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (23, 23, 2, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (24, 24, 2, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (25, 25, 2, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),
    (26, 26, 2, 'Đội tham dự chính thức mùa 2025/26', 'ACTIVE'),

    (27, 27, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (28, 28, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (29, 29, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (30, 22, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (31, 24, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (32, 15, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (33, 30, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (34, 31, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (35, 32, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (36, 33, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (37, 34, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (38, 35, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (39, 36, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),
    (40, 16, 3, 'Đội thi đấu Hạng Nhì 2025', 'ACTIVE'),

    (41, 37, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (42, 38, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (43, 53, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (44, 39, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (45, 40, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (46, 41, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (47, 42, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (48, 43, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (49, 44, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (50, 45, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (51, 46, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (52, 47, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (53, 48, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (54, 49, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (55, 50, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (56, 51, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE'),
    (57, 52, 4, 'Đội thi đấu Hạng Ba 2025', 'ACTIVE');

INSERT INTO season_team_coach (season_id, team_id, coach_id, role, assigned_date, end_date, status)
SELECT
    st.season_id,
    st.team_id,
    st.team_id,
    'HLV trưởng',
    s.start_date,
    NULL,
    'ACTIVE'
FROM season_team st
JOIN season s ON s.id = st.season_id;

INSERT INTO standing (
    season_id, team_id, played, win, draw, lose,
    goals_for, goals_against, goals_difference, points,
    rank, current_rank, recent_form
)
SELECT
    st.season_id,
    st.team_id,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, ''
FROM season_team st;

INSERT INTO player (
    id, name, date_of_birth, position, shirt_number,
    nationality, height, weight, status, team_id
)
SELECT t.id * 10 + 1, CONCAT(t.name, ' - Thủ môn số 1'), DATE_ADD('2001-01-15', INTERVAL t.id DAY), 'GK', 1, 'Việt Nam', 185, 78, 'ACTIVE', t.id
FROM team t
UNION ALL
SELECT t.id * 10 + 2, CONCAT(t.name, ' - Trung vệ số 4'), DATE_ADD('2002-02-20', INTERVAL t.id DAY), 'DF', 4, 'Việt Nam', 182, 76, 'ACTIVE', t.id
FROM team t
UNION ALL
SELECT t.id * 10 + 3, CONCAT(t.name, ' - Tiền vệ số 8'), DATE_ADD('2002-07-10', INTERVAL t.id DAY), 'MF', 8, 'Việt Nam', 176, 70, 'ACTIVE', t.id
FROM team t
UNION ALL
SELECT t.id * 10 + 4, CONCAT(t.name, ' - Tiền đạo số 9'), DATE_ADD('2003-03-05', INTERVAL t.id DAY), 'FW', 9, 'Việt Nam', 178, 72, 'ACTIVE', t.id
FROM team t;

INSERT INTO player_season (
    player_id, team_id, season_id, team_season_id,
    shirt_number, primary_position, contract_start, contract_end
)
SELECT
    p.id,
    p.team_id,
    st.season_id,
    st.id,
    p.shirt_number,
    p.position,
    s.start_date,
    s.end_date
FROM player p
JOIN season_team st ON st.team_id = p.team_id
JOIN season s ON s.id = st.season_id;

INSERT INTO `match` (
    id, status, home_score, away_score, match_date, stadium_id, season_id, home_team, away_team, round_id
) VALUES
    (1, 'SCHEDULED', NULL, NULL, '2025-08-15 19:15:00', 2, 1, 2, 14, 1),
    (2, 'SCHEDULED', NULL, NULL, '2025-08-16 18:00:00', 12, 1, 13, 6, 1),
    (3, 'SCHEDULED', NULL, NULL, '2025-08-23 19:15:00', 2, 1, 5, 3, 2),
    (4, 'SCHEDULED', NULL, NULL, '2025-08-30 18:00:00', 8, 1, 9, 10, 3),
    (5, 'SCHEDULED', NULL, NULL, '2025-09-19 16:00:00', 14, 2, 15, 24, 4),
    (6, 'SCHEDULED', NULL, NULL, '2025-09-20 16:00:00', 16, 2, 18, 17, 4),
    (7, 'SCHEDULED', NULL, NULL, '2025-10-03 17:00:00', 20, 2, 25, 23, 6),
    (8, 'SCHEDULED', NULL, NULL, '2025-10-04 17:00:00', 17, 2, 19, 26, 6),
    (9, 'SCHEDULED', NULL, NULL, '2025-04-11 15:30:00', 22, 3, 27, 28, 7),
    (10, 'SCHEDULED', NULL, NULL, '2025-05-02 16:00:00', 23, 3, 31, 35, 8),
    (11, 'SCHEDULED', NULL, NULL, '2025-06-01 16:00:00', 3, 3, 16, 36, 9),
    (12, 'SCHEDULED', NULL, NULL, '2025-10-26 14:00:00', 29, 4, 42, 45, 10),
    (13, 'SCHEDULED', NULL, NULL, '2025-10-26 14:00:00', 32, 4, 37, 38, 10),
    (14, 'SCHEDULED', NULL, NULL, '2025-11-02 15:00:00', 7, 4, 49, 50, 11),
    (15, 'SCHEDULED', NULL, NULL, '2025-11-09 15:00:00', 30, 4, 52, 43, 12);
