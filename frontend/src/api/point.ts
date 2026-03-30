import http from './http';
import { isMockEnabled, mockResolve } from './mock';
import { unwrapList } from './utils';
import type { PointLogItem } from '@/types/point';

const mockLogs: PointLogItem[] = [
  { id: 1, sourceType: 'TASK', reason: '完成图论专题任务', points: 24.0, createdAt: '2026-03-06 21:10' },
  { id: 2, sourceType: 'OJ_PROBLEM', reason: '完成 1600 rating 题目，按难度计 1.6 分', points: 1.6, createdAt: '2026-03-05 22:34' },
  { id: 3, sourceType: 'CONTEST', reason: '周赛排名奖励', points: 18.0, createdAt: '2026-03-04 22:00' }
];

export async function fetchPointLogs(): Promise<PointLogItem[]> {
  if (isMockEnabled) {
    return mockResolve(mockLogs);
  }

  const payload = await http.get('/api/points/me/logs');
  return unwrapList<PointLogItem>(payload);
}
