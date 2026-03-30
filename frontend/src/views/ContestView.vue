<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import { createContest, fetchContests, syncOfficialContests } from '@/api/contest';
import type { ContestItem } from '@/types/contest';

const authStore = useAuthStore();
const loading = ref(false);
const syncingOfficial = ref(false);
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

async function onSyncOfficial() {
  syncingOfficial.value = true;
  try {
    items.value = await syncOfficialContests();
    ElMessage.success('CF / ATC 官方比赛已同步');
  } finally {
    syncingOfficial.value = false;
  }
}

onMounted(reload);
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>比赛提醒</h1>
      </div>
      <el-button v-if="canEdit" type="primary" plain :loading="syncingOfficial" @click="onSyncOfficial">
        同步 CF / ATC
      </el-button>
    </div>

    <section class="section-card glass-panel notice-box">
      <p>系统会展示自动同步的 Codeforces / AtCoder 官方比赛；教练也可以额外录入 QOJ 训练赛提醒。</p>
    </section>

    <section v-if="canEdit" class="section-card glass-panel add-box">
      <el-input v-model="title" placeholder="训练赛标题（可选）" />
      <el-input v-model="url" placeholder="QOJ 训练赛链接（https://qoj.ac/contest/...）" />
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
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag :type="row.sourceType === 'OFFICIAL' ? 'success' : 'info'">
              {{ row.sourceType === 'OFFICIAL' ? '官方' : '手工' }}
            </el-tag>
          </template>
        </el-table-column>
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
.notice-box {
  margin-bottom: 16px;
}

.notice-box p {
  margin: 0;
  color: var(--muted);
}

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
