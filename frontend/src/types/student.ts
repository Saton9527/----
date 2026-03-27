export interface StudentItem {
  id: number;
  userId: number;
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
