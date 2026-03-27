import http from './http';
import { isMockEnabled, mockResolve } from './mock';
import { unwrapList } from './utils';
import type { PointLogItem } from '@/types/point';

const mockLogs: PointLogItem[] = [
  { id: 1, sourceType: 'TASK', reason: '完成图论专题任务', points: 24, createdAt: '2026-03-06 21:10' },
  { id: 2, sourceType: 'DAILY_AC', reason: '完成 3 道 1400 rating 题目', points: 12, createdAt: '2026-03-05 22:34' },
  { id: 3, sourceType: 'CONTEST', reason: '周赛排名奖励', points: 18, createdAt: '2026-03-04 22:00' }
];

export async function fetchPointLogs(): Promise<PointLogItem[]> {
  if (isMockEnabled) {
    return mockResolve(mockLogs);
  }

  const payload = await http.get('/api/points/me/logs');
  return unwrapList<PointLogItem>(payload);
}