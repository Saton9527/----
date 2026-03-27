INSERT INTO user_account (id, username, password, real_name, role) VALUES
(1, 'coach01', '123456', '演示教练A', 'coach'),
(2, 'student01', '123456', '演示学生A', 'student'),
(3, 'student02', '123456', '演示学生B', 'student'),
(4, 'student03', '123456', '演示学生C', 'student'),
(5, 'coach02', '123456', '演示教练B', 'coach'),
(6, '5baf2d92', '123456', '演示用户', 'student');

INSERT INTO training_task (id, title, description, deadline, status, total_problems, completed_problems) VALUES
(1, '基础动态规划热身', '完成 5 道 1200-1400 rating 的 DP 题目。', '2026-03-12 23:59:00', 'PUBLISHED', 5, 2),
(2, '图论专题训练', '完成最短路与并查集专题，共 6 题。', '2026-03-15 20:00:00', 'OVERDUE', 6, 4),
(3, '字符串专题', 'KMP 与哈希基础练习。', '2026-03-18 20:00:00', 'DONE', 4, 4);

INSERT INTO ranking_overall (id, rank_no, user_name, cf_rating, atc_rating, total_points, solved_count, streak_days) VALUES
(1, 1, '演示学生A', 1620, 1450, 248, 161, 9),
(2, 2, '演示学生B', 1540, 1410, 221, 145, 7),
(3, 3, '演示学生C', 1490, 1330, 198, 123, 5),
(4, 4, '演示用户', 1360, 1290, 186, 98, 6);

INSERT INTO point_log (id, source_type, reason, points, created_at) VALUES
(1, 'TASK', '完成图论专题任务', 24, '2026-03-06 21:10:00'),
(2, 'DAILY_AC', '完成 3 道 1400 rating 题目', 12, '2026-03-05 22:34:00'),
(3, 'CONTEST', '周赛排名奖励', 18, '2026-03-04 22:00:00');

INSERT INTO trend_point (id, stat_date, solved) VALUES
(1, '2026-03-01', 3),
(2, '2026-03-02', 5),
(3, '2026-03-03', 2),
(4, '2026-03-04', 6),
(5, '2026-03-05', 4),
(6, '2026-03-06', 3),
(7, '2026-03-07', 4);

INSERT INTO recommendation (id, level, problem_code, title) VALUES
(1, 'WARMUP', 'CF 1607A', 'Linear Keyboard'),
(2, 'CORE', 'CF 1851C', 'Tiles Comeback'),
(3, 'CHALLENGE', 'CF 1899D', 'Yarik and Musical Notes');

INSERT INTO alert_log (id, user_name, rule_code, risk_level, hit_time, status) VALUES
(1, '演示学生B', 'RULE_1', 'HIGH', '2026-03-08 21:15:00', 'OPEN'),
(2, '演示学生C', 'RULE_4', 'MEDIUM', '2026-03-08 20:00:00', 'OPEN');

INSERT INTO student_info (id, user_id, real_name, grade, major, cf_handle, atc_handle, cf_rating, atc_rating, solved_count, total_points) VALUES
(1, 2, '演示学生A', '2023', '计算机科学与技术', 'student01_cf', 'student01_atc', 1620, 1450, 161, 248),
(2, 3, '演示学生B', '2023', '软件工程', 'student02_cf', 'student02_atc', 1540, 1410, 145, 221),
(3, 4, '演示学生C', '2024', '数据科学与大数据技术', 'student03_cf', 'student03_atc', 1490, 1330, 123, 198),
(4, 6, '演示用户', '2024', '人工智能', 'demo_cf', 'demo_atc', 1360, 1290, 98, 186);

INSERT INTO team (id, name, coach_id, created_at) VALUES
(1, 'AlphaCoders', 1, '2026-03-09 18:00:00');

INSERT INTO team_member (id, team_id, user_id, member_role, created_at) VALUES
(1, 1, 2, 'CAPTAIN', '2026-03-09 18:00:00'),
(2, 1, 3, 'MEMBER', '2026-03-09 18:05:00');

INSERT INTO team_invite (id, team_id, inviter_id, invitee_id, status, created_at) VALUES
(1, 1, 2, 4, 'PENDING', '2026-03-09 18:10:00');

INSERT INTO problemset_link (id, platform, title, url, created_by, created_at) VALUES
(1, 'LUOGU', '洛谷入门经典题单', 'https://www.luogu.com.cn/training/list', 1, '2026-03-09 19:00:00'),
(2, 'LUOGU', '洛谷图论专项', 'https://www.luogu.com.cn/training/38450', 1, '2026-03-09 19:05:00');

INSERT INTO problemset_progress (id, user_id, problemset_id, solved, updated_at) VALUES
(1, 2, 1, 1, '2026-03-10 20:30:00'),
(2, 2, 2, 0, '2026-03-10 20:35:00'),
(3, 3, 1, 1, '2026-03-11 21:10:00');

INSERT INTO contest_link (id, platform, title, url, created_by, created_at) VALUES
(1, 'QOJ', 'QOJ 模拟训练赛 #1', 'https://qoj.ac/contest/1', 1, '2026-03-09 19:30:00'),
(2, 'QOJ', 'QOJ 模拟训练赛 #2', 'https://qoj.ac/contest/2', 1, '2026-03-09 19:35:00');

INSERT INTO coach_task (id, coach_id, team_id, title, description, deadline, created_at) VALUES
(1, 1, 1, '本周图论刷题', '完成 6 道最短路与并查集相关题目。', '2026-03-20 23:00:00', '2026-03-10 09:00:00');

INSERT INTO coach_task_assignment (id, task_id, user_id, status, created_at, completed_at) VALUES
(1, 1, 2, 'IN_PROGRESS', '2026-03-10 09:00:00', NULL),
(2, 1, 3, 'DONE', '2026-03-10 09:00:00', '2026-03-12 20:15:00');