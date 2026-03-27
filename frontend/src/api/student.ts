import http from './http';
import { isMockEnabled, mockResolve } from './mock';
import { unwrapList } from './utils';
import type { StudentItem } from '@/types/student';

const mockStudents: StudentItem[] = [
  {
    id: 1,
    userId: 2,
    realName: '演示学生A',
    grade: '2023',
    major: '计算机科学与技术',
    cfHandle: 'student01_cf',
    atcHandle: 'student01_atc',
    cfRating: 1620,
    atcRating: 1450,
    solvedCount: 161,
    totalPoints: 248
  },
  {
    id: 2,
    userId: 3,
    realName: '演示学生B',
    grade: '2023',
    major: '软件工程',
    cfHandle: 'student02_cf',
    atcHandle: 'student02_atc',
    cfRating: 1540,
    atcRating: 1410,
    solvedCount: 145,
    totalPoints: 221
  },
  {
    id: 3,
    userId: 4,
    realName: '演示学生C',
    grade: '2024',
    major: '数据科学与大数据技术',
    cfHandle: 'student03_cf',
    atcHandle: 'student03_atc',
    cfRating: 1490,
    atcRating: 1330,
    solvedCount: 123,
    totalPoints: 198
  }
];

export async function fetchStudents(): Promise<StudentItem[]> {
  if (isMockEnabled) {
    return mockResolve(mockStudents);
  }

  const payload = await http.get('/api/students');
  return unwrapList<StudentItem>(payload);
}
