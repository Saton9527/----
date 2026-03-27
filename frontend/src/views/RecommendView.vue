<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchRecommendations } from '@/api/recommend';
import type { RecommendItem } from '@/types/recommend';

const loading = ref(false);
const recommendations = ref<RecommendItem[]>([]);

function label(level: RecommendItem['level']) {
  if (level === 'WARMUP') return '热身题';
  if (level === 'CORE') return '主训练题';
  return '提升题';
}

const hiddenRating = computed(() => {
  const withScore = recommendations.value.find((item) => item.hiddenRating !== null);
  return withScore?.hiddenRating ?? null;
});

onMounted(async () => {
  loading.value = true;
  try {
    recommendations.value = await fetchRecommendations();
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>题目推荐</h1>
      </div>
      <el-tag v-if="hiddenRating !== null" type="warning" size="large">隐藏分：{{ hiddenRating }}</el-tag>
    </div>

    <div v-loading="loading" class="recommend-grid">
      <article v-for="item in recommendations" :key="item.id" class="section-card glass-panel recommend-card">
        <span>{{ label(item.level) }}</span>
        <h3>{{ item.problemCode }}</h3>
        <p>{{ item.title }}</p>
        <el-tag v-if="item.suggestedRating !== null" size="small">建议难度 {{ item.suggestedRating }}</el-tag>
        <p class="reason-text">{{ item.reason }}</p>
      </article>
    </div>
  </div>
</template>

<style scoped>
.recommend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.recommend-card span {
  color: var(--muted);
}

.recommend-card h3 {
  margin: 10px 0 0;
  font-size: 24px;
}

.recommend-card p {
  margin: 8px 0 0;
  color: var(--muted);
}

.reason-text {
  margin-top: 12px;
  line-height: 1.5;
}
</style>