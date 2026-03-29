import http from './http';
import { isMockEnabled, mockResolve } from './mock';

export interface TrendPoint {
  date: string;
  solved: number;
}

export interface ProblemBucket {
  rangeLabel: string;
  solvedCount: number;
  percentage: number;
}

export interface ProblemTag {
  tag: string;
  count: number;
}

export interface ProblemDetail {
  problemCode: string;
  title: string;
  rating: number;
  tag: string;
  bucketLabel: string;
}

export interface DashboardAnalytics {
  totalSolved: number;
  hiddenRating: number;
  buckets: ProblemBucket[];
  tags: ProblemTag[];
  recentSolved: ProblemDetail[];
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

const mockAnalytics: DashboardAnalytics = {
  totalSolved: 161,
  hiddenRating: 1680,
  buckets: [
    { rangeLabel: '800-1199', solvedCount: 24, percentage: 15 },
    { rangeLabel: '1200-1399', solvedCount: 37, percentage: 23 },
    { rangeLabel: '1400-1599', solvedCount: 45, percentage: 28 },
    { rangeLabel: '1600-1899', solvedCount: 36, percentage: 22 },
    { rangeLabel: '1900+', solvedCount: 19, percentage: 12 }
  ],
  tags: [
    { tag: 'Greedy', count: 31 },
    { tag: 'Implementation', count: 28 },
    { tag: 'DP', count: 33 },
    { tag: 'Graph', count: 39 },
    { tag: 'String', count: 30 }
  ],
  recentSolved: [
    { problemCode: 'CF 1749C', title: 'Number Game', rating: 1700, tag: 'Graph', bucketLabel: '1600-1899' },
    { problemCode: 'CF 1714A', title: 'Everyone Loves to Sleep', rating: 1600, tag: 'DP', bucketLabel: '1600-1899' },
    { problemCode: 'CF 1851C', title: 'Tiles Comeback', rating: 1500, tag: 'Greedy', bucketLabel: '1400-1599' },
    { problemCode: 'CF 1399A', title: 'Remove Smallest', rating: 1500, tag: 'Implementation', bucketLabel: '1400-1599' },
    { problemCode: 'CF 510A', title: 'Fox And Snake', rating: 1200, tag: 'String', bucketLabel: '1200-1399' }
  ]
};

export function fetchTrend(): Promise<TrendPoint[]> {
  if (isMockEnabled) {
    return mockResolve(mockTrend);
  }

  return http.get('/api/dashboard/me/trend');
}

export function fetchDashboardAnalytics(): Promise<DashboardAnalytics> {
  if (isMockEnabled) {
    return mockResolve(mockAnalytics);
  }

  return http.get('/api/dashboard/me/analytics');
}
