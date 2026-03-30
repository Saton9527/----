DROP TABLE IF EXISTS coach_task_assignment;
DROP TABLE IF EXISTS coach_task;
DROP TABLE IF EXISTS oj_solved_problem;
DROP TABLE IF EXISTS oj_contest_history;
DROP TABLE IF EXISTS contest_link;
DROP TABLE IF EXISTS problemset_progress;
DROP TABLE IF EXISTS problemset_link;
DROP TABLE IF EXISTS team_invite;
DROP TABLE IF EXISTS team_member;
DROP TABLE IF EXISTS team;
DROP TABLE IF EXISTS student_info;
DROP TABLE IF EXISTS alert_log;
DROP TABLE IF EXISTS recommendation;
DROP TABLE IF EXISTS trend_point;
DROP TABLE IF EXISTS point_log;
DROP TABLE IF EXISTS ranking_overall;
DROP TABLE IF EXISTS training_task;
DROP TABLE IF EXISTS user_account;

CREATE TABLE user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  real_name VARCHAR(64) NOT NULL,
  email VARCHAR(128) NULL UNIQUE,
  role VARCHAR(16) NOT NULL
);

CREATE TABLE training_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  description VARCHAR(255) NOT NULL,
  deadline TIMESTAMP NOT NULL,
  status VARCHAR(32) NOT NULL,
  total_problems INT NOT NULL,
  completed_problems INT NOT NULL
);

CREATE TABLE ranking_overall (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rank_no INT NOT NULL,
  user_name VARCHAR(64) NOT NULL,
  cf_rating INT NOT NULL,
  atc_rating INT NOT NULL,
  total_points DECIMAL(10, 1) NOT NULL,
  solved_count INT NOT NULL,
  streak_days INT NOT NULL
);

CREATE TABLE point_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  user_name VARCHAR(64) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_key VARCHAR(255) NOT NULL UNIQUE,
  reason VARCHAR(255) NOT NULL,
  points DECIMAL(10, 1) NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE trend_point (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stat_date DATE NOT NULL,
  solved INT NOT NULL
);

CREATE TABLE recommendation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  level VARCHAR(32) NOT NULL,
  problem_code VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL
);

CREATE TABLE alert_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_name VARCHAR(64) NOT NULL,
  rule_code VARCHAR(32) NOT NULL,
  risk_level VARCHAR(16) NOT NULL,
  hit_time TIMESTAMP NOT NULL,
  status VARCHAR(16) NOT NULL,
  description VARCHAR(500) NOT NULL,
  suspicious_problems VARCHAR(500) NULL,
  suggestion VARCHAR(500) NULL,
  notified_at TIMESTAMP NULL
);

CREATE TABLE student_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  real_name VARCHAR(64) NOT NULL,
  grade VARCHAR(16) NOT NULL,
  major VARCHAR(128) NOT NULL,
  cf_handle VARCHAR(64) NULL,
  atc_handle VARCHAR(64) NULL,
  cf_rating INT NOT NULL,
  atc_rating INT NOT NULL,
  solved_count INT NOT NULL,
  total_points DECIMAL(10, 1) NOT NULL
);

CREATE TABLE oj_solved_problem (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  platform VARCHAR(32) NOT NULL,
  problem_code VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  problem_url VARCHAR(255) NOT NULL,
  rating INT NULL,
  tag VARCHAR(64) NULL,
  accepted_at TIMESTAMP NOT NULL,
  source_key VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE oj_contest_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  platform VARCHAR(32) NOT NULL,
  contest_name VARCHAR(255) NOT NULL,
  contest_url VARCHAR(255) NOT NULL,
  contest_time TIMESTAMP NOT NULL,
  rank_no INT NULL,
  performance INT NULL,
  new_rating INT NULL,
  rating_change INT NULL
);

CREATE TABLE team (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL UNIQUE,
  coach_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE team_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  team_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  member_role VARCHAR(16) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_team_member_user UNIQUE (user_id)
);

CREATE TABLE team_invite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  team_id BIGINT NOT NULL,
  inviter_id BIGINT NOT NULL,
  invitee_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_team_invite UNIQUE (team_id, invitee_id)
);

CREATE TABLE problemset_link (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  platform VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL,
  url VARCHAR(255) NOT NULL,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE problemset_progress (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  problemset_id BIGINT NOT NULL,
  solved BOOLEAN NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_problemset_progress UNIQUE (user_id, problemset_id)
);

CREATE TABLE contest_link (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  platform VARCHAR(32) NOT NULL,
  source_type VARCHAR(16) NOT NULL,
  source_key VARCHAR(255) NOT NULL UNIQUE,
  title VARCHAR(128) NOT NULL,
  url VARCHAR(255) NOT NULL,
  start_time TIMESTAMP NOT NULL,
  reminder_minutes INT NOT NULL,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  reminded_at TIMESTAMP NULL
);

CREATE TABLE coach_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  coach_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  description VARCHAR(255) NOT NULL,
  deadline TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE coach_task_assignment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  completed_at TIMESTAMP NULL,
  CONSTRAINT uk_coach_task_assignment UNIQUE (task_id, user_id)
);
