import http from './http';
import { isMockEnabled, mockResolve } from './mock';
import { unwrapList } from './utils';
import type { AlertItem } from '@/types/alert';

const mockAlerts: AlertItem[] = [
  {
    id: 1,
    userName: '李四',
    ruleCode: 'RULE_1',
    riskLevel: 'HIGH',
    hitTime: '2026-03-08 21:15',
    status: 'OPEN'
  },
  {
    id: 2,
    userName: '王五',
    ruleCode: 'RULE_4',
    riskLevel: 'MEDIUM',
    hitTime: '2026-03-08 20:00',
    status: 'OPEN'
  }
];

export async function fetchAlerts(): Promise<AlertItem[]> {
  if (isMockEnabled) {
    return mockResolve(mockAlerts);
  }

  const payload = await http.get('/api/alerts');
  return unwrapList<AlertItem>(payload);
}