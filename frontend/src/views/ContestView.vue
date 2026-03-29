<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import { createContest, fetchContests } from '@/api/contest';
import type { ContestItem } from '@/types/contest';

const authStore = useAuthStore();
const loading = ref(false);
const items = ref<ContestItem[]>([]);
const title = ref('');
const url = ref('');
const startTime = ref('');
const reminderMinutes = ref(120);

const canEdit = computed(() => authStore.role === 'coach');

function statusTag(status: ContestItem['status']) {
  if (status === 'TODAY') return 'danger';
  if (status === 'UPCOMING') return 'warning';
  return 'info';
}

async function reload() {
  loading.value = true;
  try {
    items.value = await fetchContests();
  } finally {
    loading.value = false;
  }
}

async function onCreate() {
  if (!url.value.trim() || !startTime.value.trim()) return;
  await createContest({
    title: title.value.trim(),
    url: url.value.trim(),
    startTime: startTime.value.trim(),
    reminderMinutes: reminderMinutes.value
  });
  title.value = '';
  url.value = '';
  startTime.value = '';
  reminderMinutes.value = 120;
  ElMessage.success('比赛提醒已添加');
  await reload();
}

onMounted(reload);
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>线上模拟训练赛</h1>
      </div>
    </div>

    <section v-if="canEdit" class="section-card glass-panel add-box">
      <el-input v-model="title" placeholder="训练赛标题（可选）" />
      <el-input v-model="url" placeholder="QOJ 比赛链接（https://qoj.ac/contest/...）" />
      <el-input v-model="startTime" placeholder="开赛时间（2026-03-30 19:00）" />
      <el-select v-model="reminderMinutes">
        <el-option :value="30" label="提前 30 分钟" />
        <el-option :value="60" label="提前 1 小时" />
        <el-option :value="120" label="提前 2 小时" />
        <el-option :value="1440" label="提前 1 天" />
      </el-select>
      <el-button type="primary" @click="onCreate">新增比赛提醒</el-button>
    </section>

    <section class="section-card glass-panel" v-loading="loading">
      <el-table :data="items" empty-text="暂无训练赛入口">
        <el-table-column prop="title" label="训练赛名称" min-width="240" />
        <el-table-column prop="platform" label="平台" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开赛时间" min-width="180" />
        <el-table-column prop="reminderTime" label="提醒时间" min-width="180" />
        <el-table-column label="提醒规则" min-width="120">
          <template #default="{ row }">
            提前 {{ row.reminderMinutes }} 分钟
          </template>
        </el-table-column>
        <el-table-column label="链接" min-width="320">
          <template #default="{ row }">
            <a :href="row.url" target="_blank" rel="noreferrer">{{ row.url }}</a>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.add-box {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: 220px 1fr 220px 150px auto;
  gap: 10px;
}

@media (max-width: 820px) {
  .add-box {
    grid-template-columns: 1fr;
  }
}
</style>
