<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchTasks } from '@/api/task';
import type { TaskItem } from '@/types/task';

const loading = ref(false);
const status = ref('ALL');
const tasks = ref<TaskItem[]>([]);

const filteredTasks = computed(() => {
  if (status.value === 'ALL') {
    return tasks.value;
  }
  return tasks.value.filter((task) => task.status === status.value);
});

function tagType(taskStatus: TaskItem['status']) {
  if (taskStatus === 'DONE') return 'success';
  if (taskStatus === 'OVERDUE') return 'danger';
  return 'warning';
}

onMounted(async () => {
  loading.value = true;
  tasks.value = await fetchTasks();
  loading.value = false;
});
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>任务中心</h1>
      </div>

      <el-segmented
        v-model="status"
        :options="[
          { label: '全部', value: 'ALL' },
          { label: '进行中', value: 'PUBLISHED' },
          { label: '已完成', value: 'DONE' },
          { label: '已逾期', value: 'OVERDUE' }
        ]"
      />
    </div>

    <section class="section-card glass-panel">
      <el-table :data="filteredTasks" v-loading="loading" empty-text="暂无任务数据">
        <el-table-column prop="title" label="任务标题" min-width="220" />
        <el-table-column prop="description" label="说明" min-width="280" show-overflow-tooltip />
        <el-table-column prop="deadline" label="截止时间" min-width="160" />
        <el-table-column label="进度" min-width="180">
          <template #default="{ row }">
            {{ row.completedProblems }} / {{ row.totalProblems }}
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>
