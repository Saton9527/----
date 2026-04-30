import http from './http';
import { isMockEnabled, mockResolve } from './mock';
import { unwrapList } from './utils';
import type { TaskItem } from '@/types/task';

const mockTasks: TaskItem[] = [
  {
    id: 1,
    title: '基础动态规划热身',
    description: '完成 5 道 1200-1400 rating 的 DP 题目。',
    deadline: '2026-03-12 23:59',
    status: 'PUBLISHED',
    totalProblems: 5,
    completedProblems: 2
  },
  {
    id: 2,
    title: '图论专题训练',
    description: '完成最短路与并查集专题，共 6 题。',
    deadline: '2026-03-15 20:00',
    status: 'OVERDUE',
    totalProblems: 6,
    completedProblems: 4
  },
  {
    id: 3,
    title: '字符串专题',
    description: 'KMP 与哈希基础练习。',
    deadline: '2026-03-18 20:00',
    status: 'DONE',
    totalProblems: 4,
    completedProblems: 4
  }
];

export async function fetchTasks(): Promise<TaskItem[]> {
  if (isMockEnabled) {
    return mockResolve(mockTasks);
  }

  const payload = await http.get('/api/tasks', {
    skipErrorMessage: true
  });
  return unwrapList<TaskItem>(payload);
}
