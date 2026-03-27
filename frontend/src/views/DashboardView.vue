<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import * as echarts from 'echarts';
import { fetchTrend, type TrendPoint } from '@/api/dashboard';
import { fetchContests } from '@/api/contest';
import { fetchMyProfile, updatePlatformBinding } from '@/api/profile';
import { useAuthStore } from '@/store/auth';
import type { ContestItem } from '@/types/contest';
import type { MyProfile } from '@/types/profile';

const authStore = useAuthStore();
const isStudent = computed(() => authStore.role === 'student');

const trendChartRef = ref<HTMLDivElement>();
const pieChartRef = ref<HTMLDivElement>();
const profile = ref<MyProfile | null>(null);
const contests = ref<ContestItem[]>([]);
const trendData = ref<TrendPoint[]>([]);
const profileLoading = ref(false);

const bindingDialogVisible = ref(false);
const bindingSubmitting = ref(false);
const bindingFormRef = ref<FormInstance>();
const bindingForm = reactive({
  cfHandle: '',
  atcHandle: ''
});

const bindingRules: FormRules<typeof bindingForm> = {
  cfHandle: [{ required: true, message: '请输入 Codeforces 账号', trigger: 'blur' }]
};

let trendChart: echarts.ECharts | null = null;
let pieChart: echarts.ECharts | null = null;

const metricCards = computed(() => {
  if (profile.value) {
    return [
      { label: '当前积分', value: profile.value.totalPoints, delta: '实时更新' },
      { label: '总做题数', value: profile.value.solvedCount, delta: '累计题量' },
      { label: 'CF 分数', value: profile.value.cfRating, delta: profile.value.cfHandle },
      { label: 'ATC 分数', value: profile.value.atcRating, delta: profile.value.atcHandle || '未绑定' }
    ];
  }

  const totalSolved = trendData.value.reduce((sum, item) => sum + item.solved, 0);
  const activeDays = trendData.value.filter((item) => item.solved > 0).length;
  return [
    { label: '本周完成题目', value: totalSolved, delta: '团队总览' },
    { label: '活跃训练天数', value: activeDays, delta: '近 7 天' },
    { label: '当前身份', value: 'Coach', delta: authStore.user?.realName || '' }
  ];
});

function toSolvedSegments(solvedCount: number) {
  const basic = Math.max(0, Math.round(solvedCount * 0.45));
  const intermediate = Math.max(0, Math.round(solvedCount * 0.35));
  const advanced = Math.max(0, solvedCount - basic - intermediate);
  return [
    { name: '1200 以下', value: basic },
    { name: '1200-1599', value: intermediate },
    { name: '1600 及以上', value: advanced }
  ];
}

function renderTrendChart(data: TrendPoint[]) {
  if (!trendChartRef.value) {
    return;
  }

  trendChart?.dispose();
  trendChart = echarts.init(trendChartRef.value);
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 24, right: 24, top: 24, bottom: 24, containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map((item) => item.date),
      axisLine: { lineStyle: { color: '#9aacb9' } }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(34, 49, 63, 0.08)' } }
    },
    series: [
      {
        data: data.map((item) => item.solved),
        type: 'line',
        smooth: true,
        symbolSize: 9,
        lineStyle: { width: 4, color: '#1d5b8f' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(29, 91, 143, 0.35)' },
              { offset: 1, color: 'rgba(29, 91, 143, 0.02)' }
            ]
          }
        }
      }
    ]
  });
}

function renderPieChart() {
  if (!pieChartRef.value || !profile.value) {
    return;
  }

  const data = toSolvedSegments(profile.value.solvedCount);

  pieChart?.dispose();
  pieChart = echarts.init(pieChartRef.value);
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '72%'],
        center: ['50%', '45%'],
        label: { formatter: '{b}: {c}' },
        data
      }
    ]
  });
}

function handleResize() {
  trendChart?.resize();
  pieChart?.resize();
}

function openBindingDialog() {
  if (!isStudent.value) {
    return;
  }
  bindingForm.cfHandle = profile.value?.cfHandle ?? '';
  bindingForm.atcHandle = profile.value?.atcHandle ?? '';
  bindingDialogVisible.value = true;
}

async function submitBinding() {
  const valid = await bindingFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  bindingSubmitting.value = true;
  try {
    const updated = await updatePlatformBinding({
      cfHandle: bindingForm.cfHandle,
      atcHandle: bindingForm.atcHandle || null
    });
    profile.value = updated;
    renderPieChart();
    bindingDialogVisible.value = false;
    ElMessage.success('平台账号绑定已更新');
  } finally {
    bindingSubmitting.value = false;
  }
}

onMounted(async () => {
  profileLoading.value = true;
  try {
    const fallbackTrend: TrendPoint[] = [
      { date: '03-01', solved: 3 },
      { date: '03-02', solved: 5 },
      { date: '03-03', solved: 2 },
      { date: '03-04', solved: 6 },
      { date: '03-05', solved: 4 },
      { date: '03-06', solved: 3 },
      { date: '03-07', solved: 4 }
    ];

    const profilePromise = isStudent.value
      ? fetchMyProfile().catch(() => null)
      : Promise.resolve(null);

    const [profileResult, trendResult, contestResult] = await Promise.all([
      profilePromise,
      fetchTrend().catch(() => fallbackTrend),
      fetchContests().catch(() => [])
    ]);

    profile.value = profileResult;
    trendData.value = trendResult;
    contests.value = contestResult;

    await nextTick();
    renderTrendChart(trendResult);
    if (profile.value) {
      renderPieChart();
    }
    window.addEventListener('resize', handleResize);
  } finally {
    profileLoading.value = false;
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  trendChart?.dispose();
  pieChart?.dispose();
});
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>{{ isStudent ? '个人中心' : '训练总览' }}</h1>
      </div>
      <el-button v-if="isStudent" type="primary" @click="openBindingDialog">绑定 CF / ATC 账号</el-button>
    </div>

    <section v-if="isStudent" class="section-card glass-panel profile-summary" v-loading="profileLoading">
      <div class="summary-main">
        <h3>{{ profile?.realName || '未设置姓名' }}</h3>
        <p>{{ profile?.grade || '-' }}级 · {{ profile?.major || '-' }}</p>
      </div>
      <div class="summary-tags">
        <el-tag type="success">CF: {{ profile?.cfHandle || '未绑定' }}</el-tag>
        <el-tag type="warning">ATC: {{ profile?.atcHandle || '未绑定' }}</el-tag>
      </div>
    </section>

    <div class="metric-grid">
      <article v-for="metric in metricCards" :key="metric.label" class="metric-card glass-panel">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <div>{{ metric.delta }}</div>
      </article>
    </div>

    <div class="dashboard-grid">
      <section class="section-card glass-panel">
        <div class="card-header">
          <h3>近 7 天训练趋势</h3>
          <el-tag>AC</el-tag>
        </div>
        <div ref="trendChartRef" class="chart-box" />
      </section>

      <section v-if="isStudent" class="section-card glass-panel">
        <div class="card-header">
          <h3>题目难度分布</h3>
        </div>
        <div ref="pieChartRef" class="chart-box pie-box" />
      </section>

      <section v-else class="section-card glass-panel coach-panel">
        <div class="card-header">
          <h3>教练关注建议</h3>
        </div>
        <ul>
          <li>优先关注 3 天内无提交队员</li>
          <li>对高频异常提交设置邮件提醒</li>
          <li>每周按 CF / ATC 分数更新分层训练</li>
        </ul>
      </section>
    </div>

    <section class="section-card glass-panel" style="margin-top: 18px;">
      <div class="card-header">
        <h3>近期比赛经历</h3>
      </div>
      <el-timeline>
        <el-timeline-item
          v-for="contest in contests"
          :key="contest.id"
          :timestamp="contest.platform"
          placement="top"
        >
          <a :href="contest.url" target="_blank" rel="noreferrer">{{ contest.title }}</a>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="contests.length === 0" description="暂无比赛记录" :image-size="80" />
    </section>

    <el-dialog v-if="isStudent" v-model="bindingDialogVisible" title="绑定平台账号" width="460px">
      <el-form ref="bindingFormRef" :model="bindingForm" :rules="bindingRules" label-position="top">
        <el-form-item label="Codeforces ID" prop="cfHandle">
          <el-input v-model="bindingForm.cfHandle" maxlength="64" placeholder="例如 tourist" />
        </el-form-item>
        <el-form-item label="AtCoder ID" prop="atcHandle">
          <el-input v-model="bindingForm.atcHandle" maxlength="64" placeholder="例如 rng_58" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindingSubmitting" @click="submitBinding">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-summary {
  margin-bottom: 18px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.summary-main h3 {
  margin: 0;
  font-size: 24px;
}

.summary-main p {
  margin: 8px 0 0;
  color: var(--muted);
}

.summary-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 18px;
  margin-top: 18px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 18px;
}

.card-header h3 {
  margin: 0;
  font-size: 22px;
}

.chart-box {
  height: 320px;
}

.pie-box {
  max-width: 460px;
  margin: 0 auto;
}

.coach-panel ul {
  margin: 0;
  padding-left: 18px;
}

.coach-panel li + li {
  margin-top: 10px;
}

@media (max-width: 960px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .profile-summary {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
