<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { fetchRecommendations } from '@/api/recommend';
import { createProblemset, fetchProblemsets, updateProblemsetSolved } from '@/api/problemset';
import type { RecommendItem } from '@/types/recommend';
import type { ProblemsetItem } from '@/types/problemset';
import { resolveProblemUrl } from '@/utils/problem-link';

type RecommendLevel = RecommendItem['level'];

interface RecommendGroup {
  level: RecommendLevel;
  title: string;
  subtitle: string;
  tone: 'success' | 'warning' | 'danger';
  items: RecommendItem[];
}

const authStore = useAuthStore();
const router = useRouter();

const recommendationLoading = ref(false);
const sharedLoading = ref(false);
const creating = ref(false);
const togglingId = ref<number | null>(null);

const recommendations = ref<RecommendItem[]>([]);
const problemsets = ref<ProblemsetItem[]>([]);

const title = ref('');
const url = ref('');

const canEdit = computed(() => authStore.role === 'coach');
const canMarkSolved = computed(() => authStore.role === 'student');

const hiddenRating = computed(() => {
  const withScore = recommendations.value.find((item) => item.hiddenRating !== null);
  return withScore?.hiddenRating ?? null;
});

const recommendationGroups = computed<RecommendGroup[]>(() => {
  const meta: Record<RecommendLevel, Omit<RecommendGroup, 'items'>> = {
    WARMUP: {
      level: 'WARMUP',
      title: '热身区',
      subtitle: '先把手感和稳定性找回来，适合快速起量。',
      tone: 'success'
    },
    CORE: {
      level: 'CORE',
      title: '主训练区',
      subtitle: '贴近当前训练主线，应该是这轮刷题的核心投入。',
      tone: 'warning'
    },
    CHALLENGE: {
      level: 'CHALLENGE',
      title: '提升区',
      subtitle: '留给冲刺和破圈题，适合在状态好的时候攻坚。',
      tone: 'danger'
    }
  };

  return (['WARMUP', 'CORE', 'CHALLENGE'] as RecommendLevel[]).map((level) => ({
    ...meta[level],
    items: recommendations.value.filter((item) => item.level === level)
  }));
});

async function loadRecommendations() {
  recommendationLoading.value = true;
  try {
    recommendations.value = await fetchRecommendations();
  } catch {
    ElMessage.error('推荐题单加载失败，请稍后重试');
  } finally {
    recommendationLoading.value = false;
  }
}

async function loadProblemsets() {
  sharedLoading.value = true;
  try {
    problemsets.value = await fetchProblemsets();
  } catch {
    ElMessage.error('共享题单加载失败，请稍后重试');
  } finally {
    sharedLoading.value = false;
  }
}

async function reload() {
  await Promise.all([loadRecommendations(), loadProblemsets()]);
}

async function onCreate() {
  if (creating.value) {
    return;
  }

  const normalizedUrl = url.value.trim();
  if (!normalizedUrl) {
    ElMessage.warning('请输入题单链接');
    return;
  }

  if (!/^https?:\/\//i.test(normalizedUrl)) {
    ElMessage.warning('题单链接需以 http:// 或 https:// 开头');
    return;
  }

  creating.value = true;
  try {
    await createProblemset({
      title: title.value.trim(),
      url: normalizedUrl
    });
    title.value = '';
    url.value = '';
    ElMessage.success('共享题单已添加');
    await loadProblemsets();
  } catch {
    ElMessage.error('共享题单添加失败，请检查链接后重试');
  } finally {
    creating.value = false;
  }
}

async function onToggleSolved(item: ProblemsetItem) {
  if (togglingId.value === item.id) {
    return;
  }

  togglingId.value = item.id;
  try {
    await updateProblemsetSolved(item.id, { solved: !item.solved });
    ElMessage.success(item.solved ? '已取消完成标记' : '已标记为完成');
    await loadProblemsets();
  } catch {
    ElMessage.error('题单状态更新失败，请稍后重试');
  } finally {
    togglingId.value = null;
  }
}

function goToRecommendedProblems() {
  void router.push('/problems?recommended=true');
}

function rowClassName({ row }: { row: ProblemsetItem }) {
  return row.solved ? 'solved-row' : '';
}

onMounted(() => {
  void reload();
});
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>推荐题单</h1>
        <p>把个性化推荐题和共享题单放到同一页，直接开始刷，不再只是看一眼推荐结果。</p>
      </div>
      <el-tag v-if="hiddenRating !== null" type="warning" size="large">隐藏分：{{ hiddenRating }}</el-tag>
    </div>

    <div class="metric-grid" style="margin-bottom: 18px;">
      <article class="metric-card glass-panel">
        <span>推荐题数</span>
        <strong>{{ recommendations.length }}</strong>
        <div>当前训练单</div>
      </article>
      <article class="metric-card glass-panel">
        <span>热身 / 主训 / 提升</span>
        <strong>{{ recommendationGroups.map((group) => group.items.length).join(' / ') }}</strong>
        <div>按训练阶段拆分</div>
      </article>
      <article class="metric-card glass-panel">
        <span>共享题单</span>
        <strong>{{ problemsets.length }}</strong>
        <div>外部题单资源</div>
      </article>
    </div>

    <section class="section-card glass-panel hero-panel">
      <div>
        <h2>个性化训练单</h2>
        <p>系统会结合你当前绑定账号、已做题和隐藏分区间，给出一组可直接开刷的题目序列。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="goToRecommendedProblems">去题目列表只看推荐题</el-button>
        <el-button :loading="recommendationLoading" @click="loadRecommendations">刷新推荐</el-button>
      </div>
    </section>

    <section v-loading="recommendationLoading" class="recommend-grid">
      <article
        v-for="group in recommendationGroups"
        :key="group.level"
        class="section-card glass-panel recommend-card"
      >
        <div class="recommend-card__header">
          <div>
            <span class="recommend-card__eyebrow">{{ group.title }}</span>
            <h3>{{ group.subtitle }}</h3>
          </div>
          <el-tag :type="group.tone">{{ group.items.length }} 题</el-tag>
        </div>

        <div v-if="group.items.length" class="recommend-list">
          <a
            v-for="item in group.items"
            :key="item.id"
            class="recommend-item"
            :href="resolveProblemUrl(item.problemCode) || undefined"
            target="_blank"
            rel="noreferrer"
          >
            <div class="recommend-item__top">
              <strong>{{ item.problemCode }}</strong>
              <el-tag size="small">建议 {{ item.suggestedRating ?? '-' }}</el-tag>
            </div>
            <p>{{ item.title }}</p>
            <span>{{ item.reason }}</span>
          </a>
        </div>
        <el-empty v-else description="当前分组暂无推荐题" :image-size="72" />
      </article>
    </section>

    <section class="section-card glass-panel shared-panel">
      <div class="shared-panel__header">
        <div>
          <h2>共享题单</h2>
          <p>这里保留教练维护的洛谷题单，适合做专题练习或配合作业安排。</p>
        </div>
      </div>

      <div v-if="canEdit" class="add-box">
        <el-input v-model="title" placeholder="题单标题（可选）" />
        <el-input v-model="url" placeholder="洛谷题单链接（https://www.luogu.com.cn/...）" />
        <el-button type="primary" :loading="creating" @click="onCreate">新增共享题单</el-button>
      </div>

      <div v-loading="sharedLoading">
        <el-table :data="problemsets" empty-text="暂无共享题单" :row-class-name="rowClassName">
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
              <el-button
                size="small"
                :type="row.solved ? 'warning' : 'primary'"
                plain
                :loading="togglingId === row.id"
                @click="onToggleSolved(row)"
              >
                {{ row.solved ? '取消高亮' : '标记完成' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero-panel,
.shared-panel {
  margin-bottom: 18px;
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.hero-panel h2,
.shared-panel h2 {
  margin: 0;
}

.hero-panel p,
.shared-panel p {
  margin: 8px 0 0;
  color: var(--muted);
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 320px), 1fr));
  gap: 18px;
  margin-bottom: 18px;
}

.recommend-card {
  min-height: 320px;
}

.recommend-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.recommend-card__eyebrow {
  display: inline-block;
  color: var(--muted);
  margin-bottom: 6px;
}

.recommend-card__header h3 {
  margin: 0;
  font-size: 16px;
  line-height: 1.5;
}

.recommend-list {
  display: grid;
  gap: 10px;
}

.recommend-item {
  display: block;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.55);
  text-decoration: none;
  color: inherit;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.recommend-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(31, 45, 61, 0.08);
}

.recommend-item__top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.recommend-item p {
  margin: 10px 0 8px;
  font-weight: 600;
  line-height: 1.5;
}

.recommend-item span {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}

.shared-panel__header {
  margin-bottom: 14px;
}

.add-box {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: 220px 1fr auto;
  gap: 10px;
}

:deep(.el-table .solved-row > td) {
  background: rgba(47, 143, 100, 0.12) !important;
}

@media (max-width: 960px) {
  .hero-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .add-box {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .hero-actions {
    width: 100%;
  }

  .hero-actions :deep(.el-button) {
    flex: 1 1 100%;
    margin-left: 0;
  }

  .recommend-card__header,
  .recommend-item__top {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
