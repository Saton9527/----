import http from './http';
import { isMockEnabled, mockResolve } from './mock';
import type {
  ContactEmailResponse,
  MyProfile,
  OjContestHistoryItem,
  UpdateContactEmailPayload,
  UpdatePlatformBindingPayload
} from '@/types/profile';

const mockProfile: MyProfile = {
  userId: 2,
  username: 'student01',
  realName: '演示学生A',
  grade: '2023',
  major: '计算机科学与技术',
  cfHandle: null,
  atcHandle: null,
  cfRating: 0,
  atcRating: 0,
  solvedCount: 0,
  totalPoints: 24.0
};

const mockContactEmail: ContactEmailResponse = {
  email: 'coach01@example.com'
};

let mockContestHistory: OjContestHistoryItem[] = [];

export async function fetchMyProfile(): Promise<MyProfile> {
  if (isMockEnabled) {
    return mockResolve(mockProfile);
  }

  return (await http.get('/api/profile/me')) as unknown as MyProfile;
}

export async function updatePlatformBinding(payload: UpdatePlatformBindingPayload): Promise<MyProfile> {
  if (isMockEnabled) {
    mockProfile.cfHandle = payload.cfHandle;
    mockProfile.atcHandle = payload.atcHandle ?? null;
    if (!mockProfile.cfHandle) {
      mockProfile.cfRating = 0;
    }
    if (!mockProfile.atcHandle) {
      mockProfile.atcRating = 0;
    }
    if (!mockProfile.cfHandle && !mockProfile.atcHandle) {
      mockProfile.solvedCount = 0;
      mockContestHistory = [];
    }
    return mockResolve(mockProfile);
  }

  return (await http.put('/api/profile/me/platform-binding', payload, {
    timeout: 180000
  })) as unknown as MyProfile;
}

export async function syncMyOjProfile(): Promise<MyProfile> {
  if (isMockEnabled) {
    if (!mockProfile.cfHandle && !mockProfile.atcHandle) {
      return mockResolve(mockProfile, 600);
    }
    mockProfile.cfRating += 15;
    mockProfile.atcRating += 12;
    mockProfile.solvedCount += 3;
    mockProfile.totalPoints += 2.4;
    return mockResolve(mockProfile, 600);
  }

  return (await http.post('/api/profile/me/sync-oj', null, {
    timeout: 180000
  })) as unknown as MyProfile;
}

export async function importMyAtcSubmissions(file: File): Promise<MyProfile> {
  if (isMockEnabled) {
    mockProfile.solvedCount += 4;
    mockProfile.totalPoints += 1.8;
    return mockResolve(mockProfile, 600);
  }

  const formData = new FormData();
  formData.append('file', file);
  return (await http.post('/api/profile/me/import-atc-submissions', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })) as unknown as MyProfile;
}

export async function fetchMyContestHistory(): Promise<OjContestHistoryItem[]> {
  if (isMockEnabled) {
    return mockResolve(mockContestHistory);
  }

  return (await http.get('/api/profile/me/contest-history')) as unknown as OjContestHistoryItem[];
}

export async function fetchMyContactEmail(): Promise<ContactEmailResponse> {
  if (isMockEnabled) {
    return mockResolve(mockContactEmail);
  }

  return (await http.get('/api/profile/me/contact-email')) as unknown as ContactEmailResponse;
}

export async function updateMyContactEmail(payload: UpdateContactEmailPayload): Promise<ContactEmailResponse> {
  if (isMockEnabled) {
    mockContactEmail.email = payload.email;
    return mockResolve(mockContactEmail);
  }

  return (await http.put('/api/profile/me/contact-email', payload)) as unknown as ContactEmailResponse;
}
