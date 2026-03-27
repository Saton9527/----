<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
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

const coachTeams = ref<TeamInfo[]>([]);
const createdTasks = ref<CoachTaskItem[]>([]);
const assignments = ref<MyCoachTaskItem[]>([]);

const teamId = ref<number | null>(null);
const title = ref('');
const description = ref('');
const deadline = ref('');

const isCoach = computed(() => authStore.role === 'coach');

async function reloadCoach() {
  const [teams, tasks] = await Promise.all([fetchCoachTeams(), fetchMyCreatedCoachTasks()]);
  coachTeams.value = teams;
  createdTasks.value = tasks;
  if (!teamId.value && teams.length > 0) {
    teamId.value = teams[0].id;
  }
}

async function reloadStudent() {
  assignments.value = await fetchMyCoachAssignments();
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

async function onCreateTask() {
  if (!teamId.value || !title.value.trim() || !description.value.trim() || !deadline.value.trim()) return;
  await createCoachTask({
    teamId: teamId.value,
    title: title.value.trim(),
    description: description.value.trim(),
    deadline: deadline.value.trim()
  });

  title.value = '';
  description.value = '';
  deadline.value = '';
  ElMessage.success('任务下发成功');
  await reloadCoach();
}

async function onUpdateStatus(item: MyCoachTaskItem) {
  await updateCoachAssignmentStatus(item.assignmentId, { status: item.status });
  ElMessage.success('状态已更新');
  await reloadStudent();
}

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
      <section class="section-card glass-panel form-grid" v-loading="loading">
        <el-select v-model="teamId" placeholder="选择队伍">
          <el-option v-for="team in coachTeams" :key="team.id" :label="team.name" :value="team.id" />
        </el-select>
        <el-input v-model="title" placeholder="任务标题" />
        <el-input v-model="description" placeholder="任务描述" />
        <el-input v-model="deadline" placeholder="截止时间（yyyy-MM-dd HH:mm）" />
        <el-button type="primary" @click="onCreateTask">下发任务</el-button>
      </section>

      <section class="section-card glass-panel" v-loading="loading">
        <el-table :data="createdTasks" empty-text="暂无已下发任务">
          <el-table-column prop="teamId" label="队伍" width="90" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
          <el-table-column prop="deadline" label="截止时间" width="170" />
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
              <el-option label="ASSIGNED" value="ASSIGNED" />
              <el-option label="IN_PROGRESS" value="IN_PROGRESS" />
              <el-option label="DONE" value="DONE" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="onUpdateStatus(row)">保存</el-button>
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
  grid-template-columns: 180px 160px 1fr 220px auto;
  gap: 10px;
}

@media (max-width: 1080px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>