export interface CoachTaskItem {
  id: number;
  teamId: number;
  title: string;
  description: string;
  deadline: string;
  createdAt: string;
}

export interface MyCoachTaskItem {
  assignmentId: number;
  taskId: number;
  teamId: number;
  title: string;
  description: string;
  deadline: string;
  status: 'ASSIGNED' | 'IN_PROGRESS' | 'DONE';
  coachName: string;
}