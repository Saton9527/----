<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import {
  createStudent,
  downloadStudentImportErrorReport,
  downloadStudentImportTemplate,
  fetchStudents,
  importStudentAtcSubmissions,
  importStudents,
  parseStudentImportErrors,
  syncStudentOj,
  updateStudent
} from '@/api/student';
import type { StudentImportResult, StudentItem, StudentUpsertPayload } from '@/types/student';

const authStore = useAuthStore();
const loading = ref(false);
const importing = ref(false);
const downloadingTemplate = ref(false);
const studentDialogVisible = ref(false);
const submittingStudent = ref(false);
const editingStudentId = ref<number | null>(null);
const studentFormRef = ref<FormInstance>();
const students = ref<StudentItem[]>([]);
const importResult = ref<StudentImportResult | null>(null);
const syncingStudentId = ref<number | null>(null);
const importingAtcStudentId = ref<number | null>(null);
const studentForm = reactive({
  username: '',
  password: '',
  realName: '',
  grade: '',
  major: '',
  cfHandle: '',
  atcHandle: '',
  cfRating: 0,
  atcRating: 0,
  solvedCount: 0,
  totalPoints: 0
});

const canImport = computed(() => authStore.role === 'coach');
const isEditingStudent = computed(() => editingStudentId.value !== null);
const studentDialogTitle = computed(() => (isEditingStudent.value ? '编辑学生' : '新增学生'));
const importErrorItems = computed(() => parseStudentImportErrors(importResult.value?.errors ?? []));
const studentFormRules: FormRules<typeof studentForm> = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{
    validator: (_rule, value, callback) => {
      if (!isEditingStudent.value && !(value ?? '').trim()) {
        callback(new Error('请输入登录密码'));
        return;
      }
      callback();
    },
    trigger: 'blur'
  }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  grade: [{ required: true, message: '请输入年级', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }]
};

function displayHandle(handle: string | null | undefined) {
  return handle && handle.trim() ? handle : '无';
}

function displayRating(handle: string | null | undefined, rating: number) {
  return handle && handle.trim() ? rating : '无';
}

async function loadStudents() {
  loading.value = true;
  try {
    students.value = await fetchStudents();
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await loadStudents();
});

function resetStudentForm() {
  studentForm.username = '';
  studentForm.password = '';
  studentForm.realName = '';
  studentForm.grade = '';
  studentForm.major = '';
  studentForm.cfHandle = '';
  studentForm.atcHandle = '';
  studentForm.cfRating = 0;
  studentForm.atcRating = 0;
  studentForm.solvedCount = 0;
  studentForm.totalPoints = 0;
}

async function openCreateStudentDialog() {
  editingStudentId.value = null;
  resetStudentForm();
  studentDialogVisible.value = true;
  await nextTick();
  studentFormRef.value?.clearValidate();
}

async function openEditStudentDialog(student: StudentItem) {
  editingStudentId.value = student.id;
  studentForm.username = student.username;
  studentForm.password = '';
  studentForm.realName = student.realName;
  studentForm.grade = student.grade;
  studentForm.major = student.major;
  studentForm.cfHandle = student.cfHandle ?? '';
  studentForm.atcHandle = student.atcHandle ?? '';
  studentForm.cfRating = student.cfRating;
  studentForm.atcRating = student.atcRating;
  studentForm.solvedCount = student.solvedCount;
  studentForm.totalPoints = student.totalPoints;
  studentDialogVisible.value = true;
  await nextTick();
  studentFormRef.value?.clearValidate();
}

async function handleDownloadTemplate() {
  downloadingTemplate.value = true;
  try {
    await downloadStudentImportTemplate();
    ElMessage.success('模板下载已开始');
  } finally {
    downloadingTemplate.value = false;
  }
}

async function handleImport(options: UploadRequestOptions) {
  importing.value = true;
  importResult.value = null;
  try {
    const result = await importStudents(options.file as File);
    importResult.value = result;
    ElMessage.success(`导入完成：成功 ${result.importedCount} 行`);
    await loadStudents();
    options.onSuccess?.(result);
  } catch (error) {
    const uploadError = {
      name: 'UploadAjaxError',
      message: error instanceof Error ? error.message : '导入失败',
      status: 500,
      method: 'post',
      url: '/api/students/import'
    } as Parameters<NonNullable<UploadRequestOptions['onError']>>[0];
    options.onError?.(uploadError);
  } finally {
    importing.value = false;
  }
}

function handleDownloadErrorReport() {
  if (!importResult.value?.errors.length) {
    return;
  }
  downloadStudentImportErrorReport(importResult.value.errors);
  ElMessage.success('错误报告下载已开始');
}

async function submitStudentForm() {
  const valid = await studentFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  const payload: StudentUpsertPayload = {
    username: studentForm.username.trim(),
    realName: studentForm.realName.trim(),
    grade: studentForm.grade.trim(),
    major: studentForm.major.trim(),
    cfHandle: studentForm.cfHandle.trim() || null,
    atcHandle: studentForm.atcHandle.trim() || null,
    cfRating: studentForm.cfHandle.trim() ? studentForm.cfRating : 0,
    atcRating: studentForm.atcHandle.trim() ? studentForm.atcRating : 0,
    solvedCount: studentForm.cfHandle.trim() || studentForm.atcHandle.trim() ? studentForm.solvedCount : 0,
    totalPoints: studentForm.totalPoints
  };

  const password = studentForm.password.trim();
  if (password) {
    payload.password = password;
  }

  submittingStudent.value = true;
  try {
    if (isEditingStudent.value && editingStudentId.value !== null) {
      await updateStudent(editingStudentId.value, payload);
      ElMessage.success('学生账号已更新');
    } else {
      await createStudent(payload);
      ElMessage.success('学生账号已创建');
    }
    studentDialogVisible.value = false;
    await loadStudents();
  } finally {
    submittingStudent.value = false;
  }
}

async function handleSyncStudent(student: StudentItem) {
  syncingStudentId.value = student.id;
  try {
    await syncStudentOj(student.id);
    ElMessage.success(`${student.realName} 的 OJ 数据已同步`);
    await loadStudents();
  } finally {
    syncingStudentId.value = null;
  }
}

async function handleImportStudentAtc(student: StudentItem, options: UploadRequestOptions) {
  importingAtcStudentId.value = student.id;
  try {
    await importStudentAtcSubmissions(student.id, options.file as File);
    ElMessage.success(`${student.realName} 的 AtCoder 提交已导入`);
    await loadStudents();
    options.onSuccess?.(student);
  } catch (error) {
    const uploadError = {
      name: 'UploadAjaxError',
      message: error instanceof Error ? error.message : '导入失败',
      status: 500,
      method: 'post',
      url: `/api/students/${student.id}/import-atc-submissions`
    } as Parameters<NonNullable<UploadRequestOptions['onError']>>[0];
    options.onError?.(uploadError);
  } finally {
    importingAtcStudentId.value = null;
  }
}

function createImportStudentAtcHandler(student: StudentItem) {
  return (options: UploadRequestOptions) => handleImportStudentAtc(student, options);
}
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>学生管理</h1>
      </div>
    </div>

    <section v-if="canImport" class="section-card glass-panel import-box">
      <div class="import-copy">
        <strong>Excel 导入学生账号</strong>
        <p>建议先下载模板再填充。必填表头：账号、密码、姓名、年级、专业；OJ 账号列可留空。</p>
      </div>
      <div class="import-actions">
        <el-button plain type="success" @click="openCreateStudentDialog">新增学生</el-button>
        <el-button plain :loading="downloadingTemplate" @click="handleDownloadTemplate">下载模板</el-button>
        <el-upload
          :show-file-list="false"
          accept=".xlsx,.xls"
          :http-request="handleImport"
        >
          <el-button type="primary" :loading="importing">上传 Excel</el-button>
        </el-upload>
      </div>
    </section>

    <section v-if="importResult" class="section-card glass-panel import-result">
      <div class="result-summary">
        <el-tag type="success">成功 {{ importResult.importedCount }}</el-tag>
        <el-tag type="primary">新增 {{ importResult.createdCount }}</el-tag>
        <el-tag type="warning">更新 {{ importResult.updatedCount }}</el-tag>
        <el-tag type="danger">跳过 {{ importResult.skippedCount }}</el-tag>
      </div>
      <div v-if="importResult.errors.length" class="import-errors">
        <div class="import-errors__header">
          <el-alert
            type="warning"
            :closable="false"
            show-icon
            title="部分行导入失败"
            :description="`共 ${importResult.errors.length} 行失败，可导出错误报告后修正再重新导入。`"
          />
          <el-button plain @click="handleDownloadErrorReport">导出错误报告</el-button>
        </div>
        <el-table :data="importErrorItems" size="small" empty-text="暂无错误明细">
          <el-table-column prop="rowNumber" label="行号" min-width="80">
            <template #default="{ row }">
              {{ row.rowNumber ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="错误原因" min-width="320" show-overflow-tooltip />
          <el-table-column prop="rawMessage" label="原始信息" min-width="420" show-overflow-tooltip />
        </el-table>
      </div>
    </section>

    <section class="section-card glass-panel">
      <el-table :data="students" v-loading="loading" empty-text="暂无数据">
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="grade" label="年级" min-width="90" />
        <el-table-column prop="major" label="专业" min-width="180" />
        <el-table-column label="Codeforces ID" min-width="140">
          <template #default="{ row }">
            {{ displayHandle(row.cfHandle) }}
          </template>
        </el-table-column>
        <el-table-column label="AtCoder ID" min-width="130">
          <template #default="{ row }">
            {{ displayHandle(row.atcHandle) }}
          </template>
        </el-table-column>
        <el-table-column label="CF 分数" min-width="100">
          <template #default="{ row }">
            {{ displayRating(row.cfHandle, row.cfRating) }}
          </template>
        </el-table-column>
        <el-table-column label="ATC 分数" min-width="100">
          <template #default="{ row }">
            {{ displayRating(row.atcHandle, row.atcRating) }}
          </template>
        </el-table-column>
        <el-table-column prop="solvedCount" label="做题数" min-width="90" />
        <el-table-column prop="totalPoints" label="积分" min-width="90" />
        <el-table-column v-if="canImport" label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditStudentDialog(row)">编辑</el-button>
            <el-button
              link
              type="success"
              :loading="syncingStudentId === row.id"
              @click="handleSyncStudent(row)"
            >
              同步 OJ
            </el-button>
            <el-upload
              :show-file-list="false"
              accept=".json,application/json"
              :http-request="createImportStudentAtcHandler(row)"
            >
              <el-button
                link
                type="warning"
                :loading="importingAtcStudentId === row.id"
              >
                导入 ATC
              </el-button>
            </el-upload>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="studentDialogVisible" :title="studentDialogTitle" width="720px">
      <el-form ref="studentFormRef" :model="studentForm" :rules="studentFormRules" label-position="top">
        <div class="student-form-grid">
          <el-form-item label="登录账号" prop="username">
            <el-input v-model="studentForm.username" />
          </el-form-item>
          <el-form-item :label="isEditingStudent ? '登录密码（留空表示不修改）' : '登录密码'" prop="password">
            <el-input v-model="studentForm.password" type="password" show-password />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="studentForm.realName" />
          </el-form-item>
          <el-form-item label="年级" prop="grade">
            <el-input v-model="studentForm.grade" placeholder="如 2023" />
          </el-form-item>
          <el-form-item label="专业" prop="major">
            <el-input v-model="studentForm.major" />
          </el-form-item>
          <el-form-item label="Codeforces ID" prop="cfHandle">
            <el-input v-model="studentForm.cfHandle" placeholder="留空表示未绑定" />
          </el-form-item>
          <el-form-item label="AtCoder ID" prop="atcHandle">
            <el-input v-model="studentForm.atcHandle" placeholder="留空表示未绑定" />
          </el-form-item>
          <el-form-item label="CF 分数" prop="cfRating">
            <el-input-number v-model="studentForm.cfRating" :min="0" :step="50" controls-position="right" />
          </el-form-item>
          <el-form-item label="ATC 分数" prop="atcRating">
            <el-input-number v-model="studentForm.atcRating" :min="0" :step="50" controls-position="right" />
          </el-form-item>
          <el-form-item label="做题数" prop="solvedCount">
            <el-input-number v-model="studentForm.solvedCount" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="积分" prop="totalPoints">
            <el-input-number v-model="studentForm.totalPoints" :min="0" :step="0.1" :precision="1" controls-position="right" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="studentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingStudent" @click="submitStudentForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.import-box {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.import-copy p {
  margin: 8px 0 0;
  color: var(--muted);
}

.import-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.import-result {
  margin-bottom: 16px;
}

.student-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.import-errors {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.import-errors__header {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: flex-start;
}

.result-summary {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

@media (max-width: 900px) {
  .import-box {
    flex-direction: column;
    align-items: flex-start;
  }

  .import-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .import-errors__header {
    flex-direction: column;
    align-items: stretch;
  }

  .student-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
