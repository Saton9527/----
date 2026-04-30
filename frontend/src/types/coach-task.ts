export interface CoachTaskAssigneeItem {
  assignmentId: number;
  userId: number;
  username: string;
  realName: string;
  status: 'ASSIGNED' | 'IN_PROGRESS' | 'DONE';
  completedAt: string | null;
}

export interface CoachTaskItem {
  id: number;
  teamId: number;
  teamName: string | null;
  title: string;
  description: string;
  deadline: string;
  createdAt: string;
  assignedCount: number;
  inProgressCount: number;
  doneCount: number;
  assignees: CoachTaskAssigneeItem[];
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
