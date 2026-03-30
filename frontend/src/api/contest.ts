import http from './http';
import type { ContestItem } from '@/types/contest';
import { isMockEnabled, mockResolve } from './mock';

export interface ContestCreatePayload {
  url: string;
  title?: string;
  startTime: string;
  reminderMinutes: number;
}

const mockContests: ContestItem[] = [
  {
    id: 1,
    platform: 'QOJ',
    sourceType: 'MANUAL',
    title: 'QOJ 模拟训练赛 #1',
    url: 'https://qoj.ac/contest/1',
    startTime: '2026-03-30 19:00',
    reminderTime: '2026-03-30 17:00',
    reminderMinutes: 120,
    status: 'TODAY'
  },
  {
    id: 2,
    platform: 'Codeforces',
    sourceType: 'OFFICIAL',
    title: 'Codeforces Round #999',
    url: 'https://codeforces.com/contest/1999',
    startTime: '2026-04-01 22:35',
    reminderTime: '2026-04-01 20:35',
    reminderMinutes: 120,
    status: 'UPCOMING'
  },
  {
    id: 3,
    platform: 'AtCoder',
    sourceType: 'OFFICIAL',
    title: 'AtCoder Beginner Contest 452',
    url: 'https://atcoder.jp/contests/abc452',
    startTime: '2026-04-04 20:00',
    reminderTime: '2026-04-04 18:00',
    reminderMinutes: 120,
    status: 'UPCOMING'
  }
];

export async function fetchContests(): Promise<ContestItem[]> {
  if (isMockEnabled) {
    return mockResolve(mockContests);
  }
  return (await http.get('/api/contests')) as unknown as ContestItem[];
}

export async function createContest(payload: ContestCreatePayload): Promise<ContestItem> {
  if (isMockEnabled) {
    const item: ContestItem = {
      id: mockContests.length ? Math.max(...mockContests.map((contest) => contest.id)) + 1 : 1,
      platform: 'QOJ',
      sourceType: 'MANUAL',
      title: payload.title?.trim() || 'QOJ 训练赛',
      url: payload.url,
      startTime: payload.startTime,
      reminderTime: shiftMinutes(payload.startTime, -payload.reminderMinutes),
      reminderMinutes: payload.reminderMinutes,
      status: resolveStatus(payload.startTime)
    };
    mockContests.unshift(item);
    return mockResolve(item);
  }

  return (await http.post('/api/contests', payload)) as unknown as ContestItem;
}

export async function syncOfficialContests(): Promise<ContestItem[]> {
  if (isMockEnabled) {
    return mockResolve(mockContests, 500);
  }

  return (await http.post('/api/contests/sync-official')) as unknown as ContestItem[];
}

function shiftMinutes(dateTime: string, offsetMinutes: number): string {
  const normalized = dateTime.replace(' ', 'T');
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) {
    return dateTime;
  }
  date.setMinutes(date.getMinutes() + offsetMinutes);
  return formatDate(date);
}

function resolveStatus(startTime: string): ContestItem['status'] {
  const now = new Date();
  const start = new Date(startTime.replace(' ', 'T'));
  if (Number.isNaN(start.getTime())) {
    return 'UPCOMING';
  }
  if (start.getTime() < now.getTime()) {
    return 'FINISHED';
  }
  if (start.toDateString() === now.toDateString()) {
    return 'TODAY';
  }
  return 'UPCOMING';
}

function formatDate(date: Date): string {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  const hour = `${date.getHours()}`.padStart(2, '0');
  const minute = `${date.getMinutes()}`.padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
}
