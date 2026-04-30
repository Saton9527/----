import http from './http';
import { isMockEnabled, mockResolve } from './mock';
import type {
  ContactEmailResponse,
  MyProfile,
  MyProfileSyncJob,
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
let mockSyncJob: MyProfileSyncJob | null = null;

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

export async function startMyOjSyncJob(): Promise<MyProfileSyncJob> {
  if (isMockEnabled) {
    mockSyncJob = {
      jobId: 'mock-my-sync-job',
      status: 'SUCCESS',
      message: '真实 OJ 数据同步完成',
      startedAt: new Date().toISOString(),
      finishedAt: new Date().toISOString(),
      profile: {
        ...mockProfile,
        cfRating: mockProfile.cfHandle ? mockProfile.cfRating + 15 : 0,
        atcRating: mockProfile.atcHandle ? mockProfile.atcRating + 12 : 0,
        solvedCount: mockProfile.cfHandle || mockProfile.atcHandle ? mockProfile.solvedCount + 3 : 0,
        totalPoints: mockProfile.cfHandle || mockProfile.atcHandle ? mockProfile.totalPoints + 2.4 : mockProfile.totalPoints
      }
    };
    mockProfile.cfRating = mockSyncJob.profile!.cfRating;
    mockProfile.atcRating = mockSyncJob.profile!.atcRating;
    mockProfile.solvedCount = mockSyncJob.profile!.solvedCount;
    mockProfile.totalPoints = mockSyncJob.profile!.totalPoints;
    return mockResolve(mockSyncJob, 400);
  }

  return (await http.post('/api/profile/me/sync-oj/jobs')) as unknown as MyProfileSyncJob;
}

export async function fetchMyOjSyncJob(jobId: string): Promise<MyProfileSyncJob> {
  if (isMockEnabled) {
    if (!mockSyncJob || mockSyncJob.jobId !== jobId) {
      throw new Error('同步任务不存在');
    }
    return mockResolve(mockSyncJob, 200);
  }

  return (await http.get(`/api/profile/me/sync-oj/jobs/${jobId}`)) as unknown as MyProfileSyncJob;
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
