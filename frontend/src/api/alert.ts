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
    status: 'OPEN',
    description: '短时间内高频通过，超过当前训练画像的正常波动范围。',
    suspiciousProblems: 'CF 1749C, CF 1901B, CF 1843C',
    suggestion: '建议教练结合最近比赛记录和提交节奏进行人工复核。'
  },
  {
    id: 2,
    userName: '王五',
    ruleCode: 'RULE_4',
    riskLevel: 'MEDIUM',
    hitTime: '2026-03-08 20:00',
    status: 'OPEN',
    description: '通过题目难度跳跃明显，和近期稳定区间存在偏差。',
    suspiciousProblems: 'CF 1851C, CF 1899D',
    suggestion: '建议先核对做题来源，再决定是否需要单独跟进。'
  }
];

export async function fetchAlerts(): Promise<AlertItem[]> {
  if (isMockEnabled) {
    return mockResolve(mockAlerts);
  }

  const payload = await http.get('/api/alerts');
  return unwrapList<AlertItem>(payload);
}
