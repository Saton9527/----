export interface MenuItem {
  label: string;
  path: string;
}

export const studentMenu: MenuItem[] = [
  { label: '个人中心', path: '/dashboard' },
  { label: '任务中心', path: '/tasks' },
  { label: '题目列表', path: '/problems' },
  { label: '组队管理', path: '/teams' },
  { label: '推荐题单', path: '/problemsets' },
  { label: '模拟训练赛', path: '/contests' },
  { label: '教练任务', path: '/coach-tasks' },
  { label: '排行榜', path: '/ranking' },
  { label: '积分流水', path: '/points' },
  { label: '异常反馈', path: '/alerts' }
];

export const coachMenu: MenuItem[] = [
  { label: '训练总览', path: '/dashboard' },
  { label: '组队管理', path: '/teams' },
  { label: '教练任务', path: '/coach-tasks' },
  { label: '模拟训练赛', path: '/contests' },
  { label: '学生管理', path: '/students' },
  { label: '排行榜', path: '/ranking' },
  { label: '异常提醒', path: '/alerts' }
];
