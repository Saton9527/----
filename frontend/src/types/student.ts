export interface StudentItem {
  id: number;
  userId: number;
  username: string;
  realName: string;
  grade: string;
  major: string;
  cfHandle: string | null;
  atcHandle: string | null;
  cfRating: number;
  atcRating: number;
  solvedCount: number;
  totalPoints: number;
}

export interface StudentImportResult {
  importedCount: number;
  createdCount: number;
  updatedCount: number;
  skippedCount: number;
  errors: string[];
}

export interface StudentImportErrorItem {
  rowNumber: number | null;
  reason: string;
  rawMessage: string;
}

export interface StudentUpsertPayload {
  username: string;
  password?: string;
  realName: string;
  grade: string;
  major: string;
  cfHandle: string | null;
  atcHandle: string | null;
  cfRating: number;
  atcRating: number;
  solvedCount: number;
  totalPoints: number;
}
