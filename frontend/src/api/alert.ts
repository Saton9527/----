import http from './http';
import { isMockEnabled, mockResolve } from './mock';
import { unwrapList } from './utils';
import type { AlertItem, UpdateAlertFeedbackPayload } from '@/types/alert';

const mockAlerts: AlertItem[] = [
  {
    id: 1,
    userId: 3,
    userName: '李四',
    ruleCode: 'RULE_1',
    riskLevel: 'HIGH',
    hitTime: '2026-03-08 21:15',
    status: 'OPEN',
    description: '短时间内高频通过，超过当前训练画像的正常波动范围。',
    suspiciousProblems: 'CF 1749C, CF 1901B, CF 1843C',
    suggestion: '建议教练结合最近比赛记录和提交节奏进行人工复核。',
    mailSentAt: '2026-03-08 21:20',
    studentFeedback: null,
    feedbackAt: null
  },
  {
    id: 2,
    userId: 4,
    userName: '王五',
    ruleCode: 'RULE_4',
    riskLevel: 'MEDIUM',
    hitTime: '2026-03-08 20:00',
    status: 'OPEN',
    description: '通过题目难度跳跃明显，和近期稳定区间存在偏差。',
    suspiciousProblems: 'CF 1851C, CF 1899D',
    suggestion: '建议先核对做题来源，再决定是否需要单独跟进。',
    mailSentAt: null,
    studentFeedback: '这些题来自周末专题补题。',
    feedbackAt: '2026-03-08 20:30'
  }
];

export async function fetchAlerts(): Promise<AlertItem[]> {
  if (isMockEnabled) {
    return mockResolve(mockAlerts);
  }

  const payload = await http.get('/api/alerts');
  return unwrapList<AlertItem>(payload);
}

export async function fetchMyAlerts(): Promise<AlertItem[]> {
  if (isMockEnabled) {
    return mockResolve(mockAlerts.filter((item) => item.userId === 3));
  }

  const payload = await http.get('/api/alerts/me');
  return unwrapList<AlertItem>(payload);
}

export async function updateMyAlertFeedback(id: number, payload: UpdateAlertFeedbackPayload): Promise<AlertItem> {
  if (isMockEnabled) {
    const target = mockAlerts.find((item) => item.id === id);
    if (!target) {
      throw new Error('异常记录不存在');
    }
    target.studentFeedback = payload.feedback;
    target.feedbackAt = payload.feedback ? new Date().toISOString().slice(0, 16).replace('T', ' ') : null;
    return mockResolve(target);
  }

  return (await http.patch(`/api/alerts/${id}/feedback`, payload)) as unknown as AlertItem;
}
