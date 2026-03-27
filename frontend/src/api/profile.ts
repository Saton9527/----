import http from './http';
import { isMockEnabled, mockResolve } from './mock';
import type { MyProfile, UpdatePlatformBindingPayload } from '@/types/profile';

const mockProfile: MyProfile = {
  userId: 2,
  username: 'student01',
  realName: '演示学生A',
  grade: '2023',
  major: '计算机科学与技术',
  cfHandle: 'student01_cf',
  atcHandle: 'student01_atc',
  cfRating: 1620,
  atcRating: 1450,
  solvedCount: 161,
  totalPoints: 248
};

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
    return mockResolve(mockProfile);
  }

  return (await http.put('/api/profile/me/platform-binding', payload)) as unknown as MyProfile;
}
