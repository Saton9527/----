import http from './http';
import { isMockEnabled, mockResolve } from './mock';

export interface TrendPoint {
  date: string;
  solved: number;
}

const mockTrend: TrendPoint[] = [
  { date: '03-01', solved: 3 },
  { date: '03-02', solved: 5 },
  { date: '03-03', solved: 2 },
  { date: '03-04', solved: 6 },
  { date: '03-05', solved: 4 },
  { date: '03-06', solved: 3 },
  { date: '03-07', solved: 4 }
];

export function fetchTrend(): Promise<TrendPoint[]> {
  if (isMockEnabled) {
    return mockResolve(mockTrend);
  }

  return http.get('/api/dashboard/me/trend');
}
