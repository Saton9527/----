export interface ProblemItem {
  id: number;
  problemCode: string;
  title: string;
  rating: number;
  tag: string;
  platform: string;
  bucketLabel: string;
  solved: boolean;
  recommended: boolean;
}

export interface ProblemQuery {
  keyword?: string;
  minRating?: number | null;
  maxRating?: number | null;
  solved?: boolean | null;
  recommended?: boolean | null;
  page?: number;
  size?: number;
}
