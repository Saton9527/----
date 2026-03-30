export interface MyProfile {
  userId: number;
  username: string;
  realName: string;
  grade: string;
  major: string;
  cfHandle: string | null;
  atcHandle: string | null;
  cfRating: number;
  atcRating: number;
  solvedCount: number;
  totalPoints: number;
}

export interface OjContestHistoryItem {
  id: number;
  platform: string;
  contestName: string;
  contestUrl: string;
  contestTime: string;
  rankNo: number | null;
  performance: number | null;
  newRating: number | null;
  ratingChange: number | null;
}

export interface UpdatePlatformBindingPayload {
  cfHandle: string | null;
  atcHandle?: string | null;
}

export interface ContactEmailResponse {
  email: string | null;
}

export interface UpdateContactEmailPayload {
  email: string | null;
}
