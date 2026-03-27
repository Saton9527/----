import http from './http';
import { isMockEnabled, mockResolve } from './mock';
import { unwrapList } from './utils';
import type { RankingItem, RankingMetric } from '@/types/ranking';

const mockRanking: RankingItem[] = [
  { rankNo: 1, userName: '演示学生A', cfRating: 1620, atcRating: 1450, totalPoints: 248, solvedCount: 161, streakDays: 9 },
  { rankNo: 2, userName: '演示学生B', cfRating: 1540, atcRating: 1410, totalPoints: 221, solvedCount: 145, streakDays: 7 },
  { rankNo: 3, userName: '演示学生C', cfRating: 1490, atcRating: 1330, totalPoints: 198, solvedCount: 123, streakDays: 5 },
  { rankNo: 4, userName: '演示用户', cfRating: 1360, atcRating: 1290, totalPoints: 186, solvedCount: 98, streakDays: 6 }
];

function sortByMetric(rows: RankingItem[], metric: RankingMetric): RankingItem[] {
  const sorted = [...rows].sort((a, b) => {
    if (metric === 'CF_RATING') {
      return b.cfRating - a.cfRating;
    }
    if (metric === 'ATC_RATING') {
      return b.atcRating - a.atcRating;
    }
    if (metric === 'SOLVED_COUNT') {
      return b.solvedCount - a.solvedCount;
    }
    return b.totalPoints - a.totalPoints;
  });

  return sorted.map((row, index) => ({ ...row, rankNo: index + 1 }));
}

export async function fetchOverallRanking(metric: RankingMetric = 'TOTAL_POINTS'): Promise<RankingItem[]> {
  if (isMockEnabled) {
    return mockResolve(sortByMetric(mockRanking, metric));
  }

  const payload = await http.get('/api/rankings/overall', { params: { metric } });
  return unwrapList<RankingItem>(payload);
}
