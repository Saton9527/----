import http from './http';
import type { ContestItem } from '@/types/contest';

export interface ContestCreatePayload {
  url: string;
  title?: string;
}

export async function fetchContests(): Promise<ContestItem[]> {
  return (await http.get('/api/contests')) as unknown as ContestItem[];
}

export async function createContest(payload: ContestCreatePayload): Promise<ContestItem> {
  return (await http.post('/api/contests', payload)) as unknown as ContestItem;
}