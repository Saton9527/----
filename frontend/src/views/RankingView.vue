<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { fetchOverallRanking } from '@/api/ranking';
import type { RankingItem, RankingMetric } from '@/types/ranking';

const loading = ref(false);
const ranking = ref<RankingItem[]>([]);
const metric = ref<RankingMetric>('TOTAL_POINTS');

const metricOptions: Array<{ label: string; value: RankingMetric }> = [
  { label: '总积分', value: 'TOTAL_POINTS' },
  { label: 'CF 分数', value: 'CF_RATING' },
  { label: 'ATC 分数', value: 'ATC_RATING' },
  { label: '做题数', value: 'SOLVED_COUNT' }
];

async function loadRanking() {
  loading.value = true;
  try {
    ranking.value = await fetchOverallRanking(metric.value);
  } finally {
    loading.value = false;
  }
}

onMounted(loadRanking);
watch(metric, loadRanking);
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>排行榜</h1>
      </div>
      <el-select v-model="metric" placeholder="选择排行维度" style="width: 180px">
        <el-option v-for="option in metricOptions" :key="option.value" :label="option.label" :value="option.value" />
      </el-select>
    </div>

    <section class="section-card glass-panel">
      <el-table :data="ranking" v-loading="loading" empty-text="暂无榜单数据">
        <el-table-column prop="rankNo" label="排名" width="90" />
        <el-table-column prop="userName" label="姓名" min-width="160" />
        <el-table-column prop="cfRating" label="CF 分数" min-width="110" />
        <el-table-column prop="atcRating" label="ATC 分数" min-width="110" />
        <el-table-column prop="totalPoints" label="总积分" min-width="110" />
        <el-table-column prop="solvedCount" label="通过题数" min-width="110" />
        <el-table-column prop="streakDays" label="连续训练天数" min-width="140" />
      </el-table>
    </section>
  </div>
</template>
