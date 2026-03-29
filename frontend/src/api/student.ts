import http from './http';
import { isMockEnabled, mockResolve } from './mock';
import { unwrapList } from './utils';
import type { StudentImportErrorItem, StudentImportResult, StudentItem, StudentUpsertPayload } from '@/types/student';

const mockStudents: StudentItem[] = [
  {
    id: 1,
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
  },
  {
    id: 2,
    userId: 3,
    username: 'student02',
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
    username: 'student03',
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

export async function importStudents(file: File): Promise<StudentImportResult> {
  if (isMockEnabled) {
    return mockResolve({
      importedCount: 3,
      createdCount: 2,
      updatedCount: 1,
      skippedCount: 0,
      errors: []
    });
  }

  const formData = new FormData();
  formData.append('file', file);
  return (await http.post('/api/students/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })) as unknown as StudentImportResult;
}

export async function createStudent(payload: StudentUpsertPayload): Promise<StudentItem> {
  if (isMockEnabled) {
    const nextId = mockStudents.length ? Math.max(...mockStudents.map((item) => item.id)) + 1 : 1;
    const nextUserId = mockStudents.length ? Math.max(...mockStudents.map((item) => item.userId)) + 1 : 1;
    const created: StudentItem = {
      id: nextId,
      userId: nextUserId,
      username: payload.username,
      realName: payload.realName,
      grade: payload.grade,
      major: payload.major,
      cfHandle: payload.cfHandle,
      atcHandle: payload.atcHandle,
      cfRating: payload.cfRating,
      atcRating: payload.atcRating,
      solvedCount: payload.solvedCount,
      totalPoints: payload.totalPoints
    };
    mockStudents.push(created);
    return mockResolve(created);
  }

  return (await http.post('/api/students', payload)) as unknown as StudentItem;
}

export async function updateStudent(id: number, payload: StudentUpsertPayload): Promise<StudentItem> {
  if (isMockEnabled) {
    const index = mockStudents.findIndex((item) => item.id === id);
    if (index < 0) {
      throw new Error('学生不存在');
    }
    mockStudents[index] = {
      ...mockStudents[index],
      username: payload.username,
      realName: payload.realName,
      grade: payload.grade,
      major: payload.major,
      cfHandle: payload.cfHandle,
      atcHandle: payload.atcHandle,
      cfRating: payload.cfRating,
      atcRating: payload.atcRating,
      solvedCount: payload.solvedCount,
      totalPoints: payload.totalPoints
    };
    return mockResolve(mockStudents[index]);
  }

  return (await http.put(`/api/students/${id}`, payload)) as unknown as StudentItem;
}

export async function downloadStudentImportTemplate(): Promise<void> {
  let blob: Blob;

  if (isMockEnabled) {
    const csvContent = '\ufeff账号,密码,姓名,年级,专业,CF账号,ATC账号,CF分数,ATC分数,做题数,积分\nstudent2026001,123456,张三,2023,计算机科学与技术,zhangsan_cf,zhangsan_atc,1650,1480,180,260\n';
    blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  } else {
    blob = (await http.get('/api/students/import/template', {
      responseType: 'blob'
    })) as unknown as Blob;
  }

  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = isMockEnabled ? 'student-import-template.csv' : 'student-import-template.xlsx';
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}

export function parseStudentImportErrors(errors: string[]): StudentImportErrorItem[] {
  return errors.map((message) => {
    const match = message.match(/^第\s*(\d+)\s*行:\s*(.+)$/);
    return {
      rowNumber: match ? Number(match[1]) : null,
      reason: match ? match[2] : message,
      rawMessage: message
    };
  });
}

export function downloadStudentImportErrorReport(errors: string[]): void {
  const rows = parseStudentImportErrors(errors);
  const csvLines = [
    ['行号', '错误原因', '原始信息'],
    ...rows.map((item) => [
      item.rowNumber == null ? '' : String(item.rowNumber),
      item.reason,
      item.rawMessage
    ])
  ];

  const csvContent = '\ufeff' + csvLines.map((line) => line.map(escapeCsvValue).join(',')).join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = 'student-import-errors.csv';
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}

function escapeCsvValue(value: string): string {
  const normalized = value.replace(/"/g, '""');
  return /[",\n]/.test(normalized) ? `"${normalized}"` : normalized;
}
