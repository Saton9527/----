export interface MyProfile {
  userId: number;
  username: string;
  realName: string;
  grade: string;
  major: string;
  cfHandle: string;
  atcHandle: string | null;
  cfRating: number;
  atcRating: number;
  solvedCount: number;
  totalPoints: number;
}

export interface UpdatePlatformBindingPayload {
  cfHandle: string;
  atcHandle?: string | null;
}
