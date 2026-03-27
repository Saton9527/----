<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import { createProblemset, fetchProblemsets, updateProblemsetSolved } from '@/api/problemset';
import type { ProblemsetItem } from '@/types/problemset';

const authStore = useAuthStore();
const loading = ref(false);
const items = ref<ProblemsetItem[]>([]);
const title = ref('');
const url = ref('');

const canEdit = computed(() => authStore.role === 'coach');
const canMarkSolved = computed(() => authStore.role === 'student');

async function reload() {
  loading.value = true;
  try {
    items.value = await fetchProblemsets();
  } finally {
    loading.value = false;
  }
}

async function onCreate() {
  if (!url.value.trim()) return;
  await createProblemset({
    title: title.value.trim(),
    url: url.value.trim()
  });
  title.value = '';
  url.value = '';
  ElMessage.success('题单已添加');
  await reload();
}

async function onToggleSolved(item: ProblemsetItem) {
  await updateProblemsetSolved(item.id, { solved: !item.solved });
  ElMessage.success(item.solved ? '已取消完成标记' : '已标记为完成');
  await reload();
}

function rowClassName({ row }: { row: ProblemsetItem }) {
  return row.solved ? 'solved-row' : '';
}

onMounted(reload);
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>推荐题单</h1>
      </div>
    </div>

    <section v-if="canEdit" class="section-card glass-panel add-box">
      <el-input v-model="title" placeholder="题单标题（可选）" />
      <el-input v-model="url" placeholder="洛谷题单链接（https://www.luogu.com.cn/...）" />
      <el-button type="primary" @click="onCreate">新增题单</el-button>
    </section>

    <section class="section-card glass-panel" v-loading="loading">
      <el-table :data="items" empty-text="暂无题单" :row-class-name="rowClassName">
        <el-table-column prop="title" label="题单名称" min-width="220" />
        <el-table-column prop="platform" label="平台" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.solved ? 'success' : 'info'">{{ row.solved ? '已完成' : '待完成' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="链接" min-width="260">
          <template #default="{ row }">
            <a :href="row.url" target="_blank" rel="noreferrer">{{ row.url }}</a>
          </template>
        </el-table-column>
        <el-table-column v-if="canMarkSolved" label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" :type="row.solved ? 'warning' : 'primary'" plain @click="onToggleSolved(row)">
              {{ row.solved ? '取消高亮' : '标记完成' }}
            </el-button>
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

:deep(.el-table .solved-row > td) {
  background: rgba(47, 143, 100, 0.12) !important;
}

@media (max-width: 820px) {
  .add-box {
    grid-template-columns: 1fr;
  }
}
</style>