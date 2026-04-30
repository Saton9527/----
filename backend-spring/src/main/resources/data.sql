INSERT INTO user_account (id, username, password, real_name, email, role) VALUES
(1, 'coach01', '123456', '演示教练A', 'coach01@example.com', 'coach'),
(2, 'student01', '123456', '演示学生A', NULL, 'student'),
(3, 'student02', '123456', '演示学生B', NULL, 'student'),
(4, 'student03', '123456', '演示学生C', NULL, 'student'),
(5, 'coach02', '123456', '演示教练B', 'coach02@example.com', 'coach'),
(6, '5baf2d92', '123456', '演示用户', NULL, 'student');

INSERT INTO training_task (id, title, description, deadline, status, total_problems, completed_problems) VALUES
(1, '基础动态规划热身', '完成 5 道 1200-1400 rating 的 DP 题目。', '2026-03-12 23:59:00', 'PUBLISHED', 5, 2),
(2, '图论专题训练', '完成最短路与并查集专题，共 6 题。', '2026-03-15 20:00:00', 'OVERDUE', 6, 4),
(3, '字符串专题', 'KMP 与哈希基础练习。', '2026-03-18 20:00:00', 'DONE', 4, 4);

INSERT INTO ranking_overall (id, rank_no, user_name, cf_rating, atc_rating, total_points, solved_count, streak_days) VALUES
(1, 4, '演示学生A', 0, 0, 24.0, 0, 1),
(2, 1, '演示学生B', 3792, 3658, 221.0, 145, 7),
(3, 2, '演示学生C', 3696, 3619, 198.0, 123, 5),
(4, 3, '演示用户', 3074, 0, 186.0, 98, 6);

INSERT INTO point_log (id, user_id, user_name, source_type, source_key, reason, points, created_at) VALUES
(1, 2, '演示学生A', 'TASK', 'SEED:TASK:2:graph', '完成图论专题任务', 24.0, '2026-03-06 21:10:00'),
(4, 3, '演示学生B', 'OJ_PROBLEM', 'SEED:PROBLEM:3:CF1749C', '完成 Codeforces 题目 CF 1749C（1700）', 1.7, '2026-03-06 20:10:00'),
(6, 3, '演示学生B', 'SYNC_BOOTSTRAP', 'SEED:BOOTSTRAP:3', '导入历史训练积分', 212.0, '2026-03-01 09:00:00'),
(7, 4, '演示学生C', 'SYNC_BOOTSTRAP', 'SEED:BOOTSTRAP:4', '导入历史训练积分', 198.0, '2026-03-01 09:00:00'),
(8, 6, '演示用户', 'SYNC_BOOTSTRAP', 'SEED:BOOTSTRAP:6', '导入历史训练积分', 186.0, '2026-03-01 09:00:00');

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

INSERT INTO alert_log (id, user_id, user_name, rule_code, risk_level, hit_time, status, description, suspicious_problems, suggestion, student_feedback, feedback_at, notified_at) VALUES
  (1, 3, '演示学生B', 'RULE_1', 'HIGH', '2026-03-08 21:15:00', 'OPEN', '24 小时内通过题数异常偏高，超过当前训练画像的正常波动范围。', 'CF 1749C, CF 1901B, CF 1843C', '建议教练结合最近比赛记录和提交节奏进行人工复核。', NULL, NULL, NULL),
  (2, 4, '演示学生C', 'RULE_4', 'MEDIUM', '2026-03-08 20:00:00', 'OPEN', '最近通过题目难度跳跃明显，和近期稳定区间存在偏差。', 'CF 1851C, CF 1899D', '建议教练核对近期训练安排和比赛背景。', '这些题是周末补题时完成的，来源正常。', '2026-03-08 20:30:00', NULL);

INSERT INTO student_info (
  id, user_id, real_name, grade, major, cf_handle, atc_handle,
  cf_synced_handle, atc_synced_handle, cf_last_submission_epoch_second, atc_last_submission_epoch_second,
  cf_rating, atc_rating, solved_count, total_points
) VALUES
(1, 2, '演示学生A', '2023', '计算机科学与技术', NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 0, 24.0),
(2, 3, '演示学生B', '2023', '软件工程', 'Benq', 'Benq', 'Benq', 'Benq', NULL, NULL, 3792, 3658, 145, 221.0),
(3, 4, '演示学生C', '2024', '数据科学与大数据技术', 'ecnerwala', 'ecnerwala', 'ecnerwala', 'ecnerwala', NULL, NULL, 3696, 3619, 123, 198.0),
(4, 6, '演示用户', '2024', '人工智能', 'rng_58', 'rng_58', 'rng_58', 'rng_58', NULL, NULL, 3074, 0, 98, 186.0);

INSERT INTO oj_solved_problem (id, user_id, platform, problem_code, title, problem_url, rating, tag, accepted_at, source_key) VALUES
(4, 3, 'Codeforces', 'CF 580C', 'Kefa and Park', 'https://codeforces.com/problemset/problem/580/C', 1700, 'Graph', '2026-03-28 18:10:00', 'CF:3:580:C');

INSERT INTO oj_contest_history (id, user_id, platform, contest_name, contest_url, contest_time, rank_no, performance, new_rating, rating_change) VALUES
(3, 3, 'Codeforces', 'Codeforces Round #897', 'https://codeforces.com/contest/1897', '2026-03-16 22:35:00', 214, NULL, 1540, -8);

INSERT INTO problemset_link (id, platform, title, url, created_by, created_at) VALUES
(1, 'LUOGU', '基础入门题单', 'https://www.luogu.com.cn/training/100', 1, '2026-03-10 19:00:00'),
(2, 'LUOGU', '图论训练题单', 'https://www.luogu.com.cn/training/101', 1, '2026-03-11 19:00:00');

INSERT INTO problemset_progress (id, user_id, problemset_id, solved, updated_at) VALUES
(1, 2, 1, TRUE, '2026-03-12 21:00:00'),
(2, 2, 2, FALSE, '2026-03-13 21:00:00'),
(3, 3, 1, TRUE, '2026-03-12 22:00:00');

INSERT INTO contest_link (id, platform, source_type, source_key, title, url, start_time, reminder_minutes, created_by, created_at, reminded_at) VALUES
(1, 'QOJ', 'MANUAL', 'MANUAL:https://qoj.ac/contest/101:2026-03-29T19:30', '周末训练赛 A', 'https://qoj.ac/contest/101', '2026-03-29 19:30:00', 120, 1, '2026-03-20 10:00:00', NULL),
(2, 'QOJ', 'MANUAL', 'MANUAL:https://qoj.ac/contest/102:2026-04-02T19:00', '周末训练赛 B', 'https://qoj.ac/contest/102', '2026-04-02 19:00:00', 60, 1, '2026-03-21 10:00:00', NULL);
