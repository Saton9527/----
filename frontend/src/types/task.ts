export interface TaskItem {
  id: number;
  title: string;
  description: string;
  deadline: string;
  status: 'PUBLISHED' | 'DONE' | 'OVERDUE';
  totalProblems: number;
  completedProblems: number;
}
