export interface RecommendItem {
  id: number;
  level: 'WARMUP' | 'CORE' | 'CHALLENGE';
  problemCode: string;
  title: string;
  suggestedRating: number | null;
  hiddenRating: number | null;
  reason: string;
}