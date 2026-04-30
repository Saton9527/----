import http from './http';
import type { CoachTaskItem, MyCoachTaskItem } from '@/types/coach-task';

export interface CoachTaskCreatePayload {
  teamId: number;
  title: string;
  description: string;
  deadline: string;
  assigneeUserIds?: number[];
}

export interface UpdateCoachTaskStatusPayload {
  status: 'ASSIGNED' | 'IN_PROGRESS' | 'DONE';
}

export async function fetchMyCreatedCoachTasks(): Promise<CoachTaskItem[]> {
  return (await http.get('/api/coach/tasks/my-created', {
    skipErrorMessage: true
  })) as unknown as CoachTaskItem[];
}

export async function createCoachTask(payload: CoachTaskCreatePayload): Promise<CoachTaskItem> {
  return (await http.post('/api/coach/tasks', payload, {
    skipErrorMessage: true
  })) as unknown as CoachTaskItem;
}

export async function fetchMyCoachAssignments(): Promise<MyCoachTaskItem[]> {
  return (await http.get('/api/coach/tasks/my-assignments', {
    skipErrorMessage: true
  })) as unknown as MyCoachTaskItem[];
}

export async function updateCoachAssignmentStatus(
  assignmentId: number,
  payload: UpdateCoachTaskStatusPayload
): Promise<MyCoachTaskItem> {
  return (await http.patch(`/api/coach/tasks/assignments/${assignmentId}/status`, payload, {
    skipErrorMessage: true
  })) as unknown as MyCoachTaskItem;
}
