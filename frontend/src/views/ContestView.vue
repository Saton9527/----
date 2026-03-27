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

const canEdit = computed(() => authStore.role === 'coach');

async function reload() {
  loading.value = true;
  try {
    items.value = await fetchContests();
  } finally {
    loading.value = false;
  }
}

async function onCreate() {
  if (!url.value.trim()) return;
  await createContest({
    title: title.value.trim(),
    url: url.value.trim()
  });
  title.value = '';
  url.value = '';
  ElMessage.success('训练赛链接已添加');
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
      <el-button type="primary" @click="onCreate">新增训练赛入口</el-button>
    </section>

    <section class="section-card glass-panel" v-loading="loading">
      <el-table :data="items" empty-text="暂无训练赛入口">
        <el-table-column prop="title" label="训练赛名称" min-width="240" />
        <el-table-column prop="platform" label="平台" width="100" />
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
  grid-template-columns: 220px 1fr auto;
  gap: 10px;
}

@media (max-width: 820px) {
  .add-box {
    grid-template-columns: 1fr;
  }
}
</style>