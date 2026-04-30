import http from './http';
import { isMockEnabled, mockResolve } from './mock';
import { unwrapList } from './utils';
import type { RecommendItem } from '@/types/recommend';

const mockRecommendations: RecommendItem[] = [
  {
    id: 1,
    level: 'WARMUP',
    problemCode: 'CF 1399A',
    title: 'Remove Smallest',
    suggestedRating: 1400,
    hiddenRating: 1580,
    reason: '隐藏分 1580，建议先做 1400 附近题目。近期训练状态稳定，推荐难度略高于当前分段。'
  },
  {
    id: 2,
    level: 'CORE',
    problemCode: 'CF 1851C',
    title: 'Tiles Comeback',
    suggestedRating: 1600,
    hiddenRating: 1580,
    reason: '隐藏分 1580，建议先做 1600 附近题目。近期训练状态稳定，推荐难度略高于当前分段。'
  },
  {
    id: 3,
    level: 'CHALLENGE',
    problemCode: 'CF 1899D',
    title: 'Yarik and Musical Notes',
    suggestedRating: 1800,
    hiddenRating: 1580,
    reason: '隐藏分 1580，建议先做 1800 附近题目。近期训练状态稳定，推荐难度略高于当前分段。'
  }
];

export async function fetchRecommendations(): Promise<RecommendItem[]> {
  if (isMockEnabled) {
    return mockResolve(mockRecommendations);
  }

  const payload = await http.get('/api/recommendations/me', {
    params: {
      page: 0,
      size: 12
    }
  });
  return unwrapList<RecommendItem>(payload);
}
