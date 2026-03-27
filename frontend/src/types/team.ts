export interface TeamMember {
  userId: number;
  username: string;
  realName: string;
  role: 'CAPTAIN' | 'MEMBER';
}

export interface TeamInfo {
  id: number;
  name: string;
  coachId: number | null;
  coachName: string | null;
  members: TeamMember[];
}

export interface TeamInvite {
  id: number;
  teamId: number;
  teamName: string;
  inviterName: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
}