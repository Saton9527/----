import http from './http';
import type { TeamInfo, TeamInvite } from '@/types/team';

export interface TeamCreatePayload {
  name: string;
}

export interface TeamInvitePayload {
  username: string;
}

export interface TeamCoachPayload {
  coachUsername: string;
}

export async function fetchMyTeam(): Promise<TeamInfo | null> {
  return (await http.get('/api/teams/me')) as unknown as TeamInfo | null;
}

export async function fetchCoachTeams(): Promise<TeamInfo[]> {
  return (await http.get('/api/teams/coach/me')) as unknown as TeamInfo[];
}

export async function createTeam(payload: TeamCreatePayload): Promise<TeamInfo> {
  return (await http.post('/api/teams', payload)) as unknown as TeamInfo;
}

export async function inviteTeamMember(teamId: number, payload: TeamInvitePayload): Promise<TeamInvite> {
  return (await http.post(`/api/teams/${teamId}/invites`, payload)) as unknown as TeamInvite;
}

export async function fetchMyTeamInvites(): Promise<TeamInvite[]> {
  return (await http.get('/api/teams/invites/me')) as unknown as TeamInvite[];
}

export async function acceptTeamInvite(inviteId: number): Promise<TeamInfo> {
  return (await http.post(`/api/teams/invites/${inviteId}/accept`)) as unknown as TeamInfo;
}

export async function rejectTeamInvite(inviteId: number): Promise<TeamInvite> {
  return (await http.post(`/api/teams/invites/${inviteId}/reject`)) as unknown as TeamInvite;
}

export async function assignTeamCoach(teamId: number, payload: TeamCoachPayload): Promise<TeamInfo> {
  return (await http.post(`/api/teams/${teamId}/coach`, payload)) as unknown as TeamInfo;
}