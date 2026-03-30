import http from './http';
import type { PageResponse } from '@/types/page';
import type { ProblemItem, ProblemQuery } from '@/types/problem';

export function fetchProblems(params: ProblemQuery): Promise<PageResponse<ProblemItem>> {
  return http.get('/api/problems', {
    params: {
      keyword: params.keyword || undefined,
      minRating: params.minRating ?? undefined,
      maxRating: params.maxRating ?? undefined,
      solved: params.solved ?? undefined,
      page: params.page ?? 0,
      size: params.size ?? 12
    }
  });
}
