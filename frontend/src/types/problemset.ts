export interface ProblemsetItem {
  id: number;
  platform: string;
  title: string;
  url: string;
  solved: boolean;
  solvedAt: string | null;
}