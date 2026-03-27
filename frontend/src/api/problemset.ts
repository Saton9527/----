import http from './http';
import type { ProblemsetItem } from '@/types/problemset';

export interface ProblemsetCreatePayload {
  url: string;
  title?: string;
}

export interface ProblemsetSolvedPayload {
  solved: boolean;
}

export async function fetchProblemsets(): Promise<ProblemsetItem[]> {
  return (await http.get('/api/problemsets')) as unknown as ProblemsetItem[];
}

export async function createProblemset(payload: ProblemsetCreatePayload): Promise<ProblemsetItem> {
  return (await http.post('/api/problemsets', payload)) as unknown as ProblemsetItem;
}

export async function updateProblemsetSolved(id: number, payload: ProblemsetSolvedPayload): Promise<ProblemsetItem> {
  return (await http.patch(`/api/problemsets/${id}/solved`, payload)) as unknown as ProblemsetItem;
}