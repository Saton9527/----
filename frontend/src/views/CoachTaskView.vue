<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import {
  createCoachTask,
  fetchMyCoachAssignments,
  fetchMyCreatedCoachTasks,
  updateCoachAssignmentStatus
} from '@/api/coach-task';
import { fetchCoachTeams } from '@/api/team';
import type { CoachTaskItem, MyCoachTaskItem } from '@/types/coach-task';
import type { TeamInfo } from '@/types/team';

const authStore = useAuthStore();
const loading = ref(false);
const creatingTask = ref(false);
const updatingAssignmentId = ref<number | null>(null);

const coachTeams = ref<TeamInfo[]>([]);
const createdTasks = ref<CoachTaskItem[]>([]);
const assignments = ref<MyCoachTaskItem[]>([]);

const teamId = ref<number | null>(null);
const title = ref('');
const description = ref('');
const deadline = ref('');
const assigneeUserIds = ref<number[]>([]);

const isCoach = computed(() => authStore.role === 'coach');
const selectedTeam = computed(() => coachTeams.value.find((team) => team.id === teamId.value) ?? null);
const selectedMemberOptions = computed(() => selectedTeam.value?.members ?? []);
const selectedTaskCount = computed(() => createdTasks.value.length);
const totalAssignedCount = computed(() => createdTasks.value.reduce((sum, task) => sum + task.assignedCount, 0));
const totalDoneCount = computed(() => createdTasks.value.reduce((sum, task) => sum + task.doneCount, 0));

async function reloadCoach() {
  try {
    const [teams, tasks] = await Promise.all([fetchCoachTeams(), fetchMyCreatedCoachTasks()]);
    coachTeams.value = teams;
    createdTasks.value = tasks;
    if (teams.length === 0) {
      teamId.value = null;
      assigneeUserIds.value = [];
      return;
    }
    if (!teamId.value || !teams.some((team) => team.id === teamId.value)) {
      teamId.value = teams[0].id;
    }
  } catch {
    ElMessage.error('教练任务数据加载失败，请稍后重试');
  }
}

async function reloadStudent() {
  try {
    assignments.value = await fetchMyCoachAssignments();
  } catch {
    ElMessage.error('教练任务列表加载失败，请稍后重试');
  }
}

async function reload() {
  loading.value = true;
  try {
    if (isCoach.value) {
      await reloadCoach();
    } else {
      await reloadStudent();
    }
  } finally {
    loading.value = false;
  }
}

function isValidDateTime(value: string) {
  return !Number.isNaN(new Date(value.replace(' ', 'T')).getTime());
}

function selectAllMembers() {
  assigneeUserIds.value = selectedMemberOptions.value.map((member) => member.userId);
}

async function onCreateTask() {
  if (creatingTask.value) {
    return;
  }

  const normalizedTitle = title.value.trim();
  const normalizedDescription = description.value.trim();
  const normalizedDeadline = deadline.value.trim();

  if (!teamId.value) {
    ElMessage.warning('请先选择要下发任务的队伍');
    return;
  }

  if (!normalizedTitle || !normalizedDescription || !normalizedDeadline) {
    ElMessage.warning('请完整填写任务标题、描述和截止时间');
    return;
  }

  if (!isValidDateTime(normalizedDeadline)) {
    ElMessage.warning('截止时间格式无效，请使用 yyyy-MM-dd HH:mm');
    return;
  }

  if (assigneeUserIds.value.length === 0) {
    ElMessage.warning('请至少选择一名下发对象');
    return;
  }

  creatingTask.value = true;
  try {
    await createCoachTask({
      teamId: teamId.value,
      title: normalizedTitle,
      description: normalizedDescription,
      deadline: normalizedDeadline,
      assigneeUserIds: assigneeUserIds.value
    });

    title.value = '';
    description.value = '';
    deadline.value = '';
    selectAllMembers();
    ElMessage.success('任务下发成功');
    await reloadCoach();
  } catch {
    ElMessage.error('任务下发失败，请检查内容后重试');
  } finally {
    creatingTask.value = false;
  }
}

async function onUpdateStatus(item: MyCoachTaskItem) {
  if (updatingAssignmentId.value === item.assignmentId) {
    return;
  }

  updatingAssignmentId.value = item.assignmentId;
  try {
    await updateCoachAssignmentStatus(item.assignmentId, { status: item.status });
    ElMessage.success('状态已更新');
    await reloadStudent();
  } catch {
    ElMessage.error('任务状态更新失败，请稍后重试');
  } finally {
    updatingAssignmentId.value = null;
  }
}

function statusTagType(status: MyCoachTaskItem['status'] | CoachTaskItem['assignees'][number]['status']) {
  if (status === 'DONE') {
    return 'success';
  }
  if (status === 'IN_PROGRESS') {
    return 'warning';
  }
  return 'info';
}

function statusLabel(status: MyCoachTaskItem['status'] | CoachTaskItem['assignees'][number]['status']) {
  if (status === 'DONE') {
    return '已完成';
  }
  if (status === 'IN_PROGRESS') {
    return '进行中';
  }
  return '已下发';
}

watch(
  selectedMemberOptions,
  (members) => {
    if (members.length === 0) {
      assigneeUserIds.value = [];
      return;
    }
    const validUserIds = new Set(members.map((member) => member.userId));
    const filtered = assigneeUserIds.value.filter((userId) => validUserIds.has(userId));
    assigneeUserIds.value = filtered.length > 0 ? filtered : members.map((member) => member.userId);
  },
  { immediate: true }
);

onMounted(reload);
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>教练任务</h1>
      </div>
    </div>

    <template v-if="isCoach">
      <div class="metric-grid" style="margin-bottom: 18px;">
        <article class="metric-card glass-panel">
          <span>已下发任务</span>
          <strong>{{ selectedTaskCount }}</strong>
          <div>当前教练视角</div>
        </article>
        <article class="metric-card glass-panel">
          <span>累计下发对象</span>
          <strong>{{ totalAssignedCount }}</strong>
          <div>跨任务分配次数</div>
        </article>
        <article class="metric-card glass-panel">
          <span>已完成</span>
          <strong>{{ totalDoneCount }}</strong>
          <div>学生已回执完成</div>
        </article>
      </div>

      <section class="section-card glass-panel form-grid" v-loading="loading">
        <el-select v-model="teamId" placeholder="选择队伍">
          <el-option v-for="team in coachTeams" :key="team.id" :label="team.name" :value="team.id" />
        </el-select>
        <el-input v-model="title" placeholder="任务标题" />
        <el-input
          v-model="description"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="任务描述"
        />
        <el-date-picker
          v-model="deadline"
          type="datetime"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm"
          placeholder="选择截止时间"
          clearable
        />
        <el-select
          v-model="assigneeUserIds"
          multiple
          collapse-tags
          collapse-tags-tooltip
          placeholder="选择下发对象"
        >
          <el-option
            v-for="member in selectedMemberOptions"
            :key="member.userId"
            :label="`${member.realName}（${member.username}）`"
            :value="member.userId"
          />
        </el-select>
        <div class="form-actions">
          <span class="selection-hint">当前下发 {{ assigneeUserIds.length }} 人</span>
          <div class="action-buttons">
            <el-button @click="selectAllMembers">全选队员</el-button>
            <el-button type="primary" :loading="creatingTask" @click="onCreateTask">下发任务</el-button>
          </div>
        </div>
      </section>

      <section class="section-card glass-panel" v-loading="loading">
        <el-table :data="createdTasks" empty-text="暂无已下发任务">
          <el-table-column type="expand" width="48">
            <template #default="{ row }">
              <div class="assignee-panel">
                <div class="assignee-summary">
                  <el-tag type="info">下发 {{ row.assignedCount }} 人</el-tag>
                  <el-tag type="warning">进行中 {{ row.inProgressCount }}</el-tag>
                  <el-tag type="success">已完成 {{ row.doneCount }}</el-tag>
                </div>
                <div class="assignee-list">
                  <article v-for="assignee in row.assignees" :key="assignee.assignmentId" class="assignee-card">
                    <strong>{{ assignee.realName }}</strong>
                    <span>{{ assignee.username }}</span>
                    <el-tag size="small" :type="statusTagType(assignee.status)">
                      {{ statusLabel(assignee.status) }}
                    </el-tag>
                    <small v-if="assignee.completedAt">完成于 {{ assignee.completedAt }}</small>
                  </article>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="teamName" label="队伍" min-width="140" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
          <el-table-column prop="deadline" label="截止时间" width="170" />
          <el-table-column label="执行进度" min-width="190">
            <template #default="{ row }">
              <div class="status-stack">
                <span>已下发 {{ row.assignedCount }} 人</span>
                <span>进行中 {{ row.inProgressCount }} / 已完成 {{ row.doneCount }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="下发时间" width="170" />
        </el-table>
      </section>
    </template>

    <section v-else class="section-card glass-panel" v-loading="loading">
      <el-table :data="assignments" empty-text="暂无教练任务">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
        <el-table-column prop="coachName" label="教练" width="120" />
          <el-table-column prop="deadline" label="截止时间" width="170" />
        <el-table-column label="状态" width="180">
          <template #default="{ row }">
            <el-select v-model="row.status" size="small">
              <el-option label="已下发" value="ASSIGNED" />
              <el-option label="进行中" value="IN_PROGRESS" />
              <el-option label="已完成" value="DONE" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              plain
              :loading="updatingAssignmentId === row.assignmentId"
              @click="onUpdateStatus(row)"
            >
              保存
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.form-grid {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: 180px 180px minmax(260px, 1.2fr) 220px minmax(220px, 1fr) auto;
  gap: 10px;
  align-items: start;
}

.form-actions {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 10px;
  min-height: 100%;
}

.selection-hint {
  color: var(--muted);
  font-size: 13px;
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.status-stack {
  display: grid;
  gap: 4px;
}

.assignee-panel {
  padding: 6px 0;
}

.assignee-summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.assignee-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.assignee-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.58);
}

.assignee-card span,
.assignee-card small {
  color: var(--muted);
}

@media (max-width: 1080px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
