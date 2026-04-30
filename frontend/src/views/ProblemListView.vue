<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { fetchProblems } from '@/api/problem';
import type { ProblemItem } from '@/types/problem';
import type { PageResponse } from '@/types/page';
import { resolveProblemUrl } from '@/utils/problem-link';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const items = ref<ProblemItem[]>([]);
const pageState = ref<PageResponse<ProblemItem> | null>(null);

const filters = reactive({
  keyword: '',
  minRating: null as number | null,
  maxRating: null as number | null,
  solvedOnly: false,
  recommendedOnly: false,
  page: 1,
  size: 12
});

const ratingOptions = [
  800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400
];

const solvedCount = computed(() => items.value.filter((item) => item.solved).length);
const recommendedCount = computed(() => items.value.filter((item) => item.recommended).length);

async function loadProblems() {
  if (filters.minRating != null && filters.maxRating != null && filters.minRating > filters.maxRating) {
    ElMessage.warning('最低难度不能高于最高难度');
    return;
  }

  loading.value = true;
  try {
    const response = await fetchProblems({
      keyword: filters.keyword.trim() || undefined,
      minRating: filters.minRating,
      maxRating: filters.maxRating,
      solved: filters.solvedOnly ? true : null,
      recommended: filters.recommendedOnly ? true : null,
      page: filters.page - 1,
      size: filters.size
    });
    pageState.value = response;
    items.value = response.content;
  } catch {
    ElMessage.error('题目列表加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

function syncRouteQuery() {
  const nextRecommended = filters.recommendedOnly ? 'true' : undefined;
  if (route.query.recommended === nextRecommended) {
    return;
  }

  void router.replace({
    query: {
      ...route.query,
      recommended: nextRecommended
    }
  });
}

function onSearch() {
  filters.page = 1;
  syncRouteQuery();
  void loadProblems();
}

function onReset() {
  filters.keyword = '';
  filters.minRating = null;
  filters.maxRating = null;
  filters.solvedOnly = false;
  filters.recommendedOnly = false;
  filters.page = 1;
  syncRouteQuery();
  void loadProblems();
}

function onPageChange(page: number) {
  filters.page = page;
  void loadProblems();
}

function rowClassName({ row }: { row: ProblemItem }) {
  if (row.solved) {
    return 'problem-row-solved';
  }
  if (row.recommended) {
    return 'problem-row-recommended';
  }
  return '';
}

function applyRouteQuery() {
  filters.recommendedOnly = route.query.recommended === 'true';
}

onMounted(() => {
  applyRouteQuery();
  void loadProblems();
});

watch(
  () => route.query.recommended,
  (recommended) => {
    const nextRecommendedOnly = recommended === 'true';
    if (filters.recommendedOnly === nextRecommendedOnly) {
      return;
    }

    filters.recommendedOnly = nextRecommendedOnly;
    filters.page = 1;
    void loadProblems();
  }
);
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>题目列表</h1>
        <p>按关键字和难度筛选，已做题高亮展示，方便继续刷题和查漏补缺。</p>
      </div>
    </div>

    <div class="metric-grid" style="margin-bottom: 18px;">
      <article class="metric-card glass-panel">
        <span>当前页题目</span>
        <strong>{{ items.length }}</strong>
        <div>分页结果</div>
      </article>
      <article class="metric-card glass-panel">
        <span>已做高亮</span>
        <strong>{{ solvedCount }}</strong>
        <div>当前筛选范围</div>
      </article>
      <article class="metric-card glass-panel">
        <span>推荐命中</span>
        <strong>{{ recommendedCount }}</strong>
        <div>贴近你的训练区间</div>
      </article>
    </div>

    <section class="section-card glass-panel filter-panel">
      <el-input
        v-model="filters.keyword"
        clearable
        placeholder="搜索题号 / 题名 / 标签"
        @keyup.enter="onSearch"
      />
      <el-select v-model="filters.minRating" clearable placeholder="最低难度">
        <el-option v-for="rating in ratingOptions" :key="`min-${rating}`" :label="rating" :value="rating" />
      </el-select>
        <el-select v-model="filters.maxRating" clearable placeholder="最高难度">
          <el-option v-for="rating in ratingOptions" :key="`max-${rating}`" :label="rating" :value="rating" />
        </el-select>
        <el-switch v-model="filters.solvedOnly" inline-prompt active-text="已做" inactive-text="全部" />
        <el-switch v-model="filters.recommendedOnly" inline-prompt active-text="推荐" inactive-text="全部" />
        <el-button type="primary" @click="onSearch">筛选</el-button>
        <el-button @click="onReset">重置</el-button>
      </section>

    <section class="section-card glass-panel" v-loading="loading">
      <el-table :data="items" empty-text="暂无符合条件的题目" :row-class-name="rowClassName">
        <el-table-column label="题号" min-width="130">
          <template #default="{ row }">
            <a v-if="resolveProblemUrl(row.problemCode)" :href="resolveProblemUrl(row.problemCode)!" target="_blank" rel="noreferrer">
              {{ row.problemCode }}
            </a>
            <template v-else>{{ row.problemCode }}</template>
          </template>
        </el-table-column>
        <el-table-column label="题目" min-width="240">
          <template #default="{ row }">
            <a v-if="resolveProblemUrl(row.problemCode)" :href="resolveProblemUrl(row.problemCode)!" target="_blank" rel="noreferrer">
              {{ row.title }}
            </a>
            <template v-else>{{ row.title }}</template>
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="Rating" width="100" />
        <el-table-column prop="tag" label="标签" min-width="130" />
        <el-table-column prop="bucketLabel" label="分段" min-width="130" />
        <el-table-column label="状态" min-width="170">
          <template #default="{ row }">
            <div class="status-tags">
              <el-tag :type="row.solved ? 'success' : 'info'">{{ row.solved ? '已做' : '未做' }}</el-tag>
              <el-tag v-if="row.recommended" type="warning">推荐</el-tag>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <span>共 {{ pageState?.totalElements ?? 0 }} 题</span>
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="filters.page"
          :page-size="filters.size"
          :total="pageState?.totalElements ?? 0"
          @current-change="onPageChange"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.filter-panel {
  margin-bottom: 18px;
  display: grid;
  grid-template-columns: minmax(220px, 1.2fr) repeat(2, minmax(140px, 180px)) 120px 120px auto auto;
  gap: 10px;
  align-items: center;
}

.status-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-row {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

:deep(.el-table .problem-row-solved > td) {
  background: rgba(47, 143, 100, 0.1) !important;
}

:deep(.el-table .problem-row-recommended > td) {
  background: rgba(220, 141, 36, 0.08) !important;
}

@media (max-width: 1080px) {
  .filter-panel {
    grid-template-columns: 1fr;
  }

  .pagination-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
