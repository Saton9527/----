export type RankingMetric = 'TOTAL_POINTS' | 'CF_RATING' | 'ATC_RATING' | 'SOLVED_COUNT';

export interface RankingItem {
  rankNo: number;
  userName: string;
  cfRating: number;
  atcRating: number;
  totalPoints: number;
  solvedCount: number;
  streakDays: number;
}
