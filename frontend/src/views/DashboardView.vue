<script setup lang="ts">
import axios from 'axios';
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus';
import { fetchDashboardAnalytics, fetchTrend, type DashboardAnalytics, type TrendPoint } from '@/api/dashboard';
import { fetchContests } from '@/api/contest';
import {
  fetchMyOjSyncJob,
  fetchMyContactEmail,
  fetchMyContestHistory,
  fetchMyProfile,
  importMyAtcSubmissions,
  startMyOjSyncJob,
  updateMyContactEmail,
  updatePlatformBinding
} from '@/api/profile';
import { fetchCoachTeams, fetchMyTeam, inviteTeamMember } from '@/api/team';
import { useAuthStore } from '@/store/auth';
import { resolveProblemUrl } from '@/utils/problem-link';
import type { ContestItem } from '@/types/contest';
import type { OjContestHistoryItem } from '@/types/profile';
import type { MyProfile } from '@/types/profile';
import type { TeamInfo } from '@/types/team';

const authStore = useAuthStore();
const isStudent = computed(() => authStore.role === 'student');

const trendChartRef = ref<HTMLDivElement>();
const pieChartRef = ref<HTMLDivElement>();
const profile = ref<MyProfile | null>(null);
const contests = ref<ContestItem[]>([]);
const contestHistory = ref<OjContestHistoryItem[]>([]);
const trendData = ref<TrendPoint[]>([]);
const analytics = ref<DashboardAnalytics | null>(null);
const profileLoading = ref(false);
const syncingOj = ref(false);
const importingAtc = ref(false);
const myTeam = ref<TeamInfo | null>(null);
const coachTeams = ref<TeamInfo[]>([]);
const inviteDialogVisible = ref(false);
const inviteDialogUsername = ref('');
const contactEmail = ref<string | null>(null);
const contactEmailDialogVisible = ref(false);
const contactEmailSubmitting = ref(false);
const contactEmailFormRef = ref<FormInstance>();
const contactEmailForm = reactive({
  email: ''
});

const bindingDialogVisible = ref(false);
const bindingSubmitting = ref(false);
const bindingFormRef = ref<FormInstance>();
const bindingForm = reactive({
  cfHandle: '',
  atcHandle: ''
});

const bindingRules: FormRules<typeof bindingForm> = {};

const contactEmailRules: FormRules<typeof contactEmailForm> = {
  email: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        const normalized = value.trim();
        if (!normalized) {
          callback();
          return;
        }

        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailPattern.test(normalized)) {
          callback(new Error('请输入正确的邮箱地址'));
          return;
        }

        callback();
      },
      trigger: 'blur'
    }
  ]
};

type EChartsModule = typeof import('echarts');
type EChartsInstance = ReturnType<EChartsModule['init']>;

let trendChart: EChartsInstance | null = null;
let pieChart: EChartsInstance | null = null;
let echartsModulePromise: Promise<EChartsModule> | null = null;

function loadEcharts() {
  if (!echartsModulePromise) {
    echartsModulePromise = import('echarts');
  }

  return echartsModulePromise;
}

async function refreshTeamInfo() {
  if (isStudent.value) {
    myTeam.value = await fetchMyTeam().catch(() => null);
    return;
  }

  coachTeams.value = await fetchCoachTeams().catch(() => []);
}

function buildMemberSlots(members: TeamInfo['members']) {
  const slots: Array<{
    key: string;
    placeholder: boolean;
    member: TeamInfo['members'][number] | null;
  }> = members.map((member) => ({
    key: `member-${member.userId}`,
    placeholder: false,
    member
  }));

  while (slots.length < 3) {
    slots.push({
      key: `placeholder-${slots.length}`,
      placeholder: true,
      member: null
    });
  }

  return slots.slice(0, 3);
}

const isTeamCaptain = computed(() => {
  if (!isStudent.value || !myTeam.value || !authStore.user) {
    return false;
  }

  return myTeam.value.members.some((member) => member.userId === authStore.user!.id && member.role === 'CAPTAIN');
});

function isDuplicateInviteError(error: unknown) {
  if (!axios.isAxiosError(error)) {
    return false;
  }

  const message = error.response?.data?.message;
  return error.response?.status === 409 && typeof message === 'string' && message.includes('已向该用户发送过邀请');
}

function displayHandle(handle: string | null | undefined) {
  return handle && handle.trim() ? handle : '无';
}

function displayRating(handle: string | null | undefined, rating: number | null | undefined) {
  return handle && handle.trim() ? (rating ?? 0) : '无';
}

function sleep(ms: number) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms);
  });
}

function onAddMemberFromDashboard() {
  if (!isTeamCaptain.value) {
    return;
  }
  inviteDialogUsername.value = '';
  inviteDialogVisible.value = true;
}

async function onSubmitInviteDialog() {
  const username = inviteDialogUsername.value.trim();
  if (!myTeam.value) {
    return;
  }

  if (!username) {
    ElMessage.warning('请输入要邀请的队员账号');
    return;
  }

  try {
    await inviteTeamMember(myTeam.value.id, { username });
    myTeam.value = await fetchMyTeam().catch(() => myTeam.value);
    inviteDialogUsername.value = '';
    inviteDialogVisible.value = false;
    ElMessage.success('邀请已发送');
  } catch (error) {
    if (isDuplicateInviteError(error)) {
      ElMessage.info('已递交邀请，请等待对方处理');
    } else {
      ElMessage.error('组队邀请发送失败，请确认账号是否正确');
    }
  }
}

const metricCards = computed(() => {
  if (profile.value) {
    return [
      { label: '当前积分', value: profile.value.totalPoints, delta: '实时更新' },
      { label: '总做题数', value: profile.value.solvedCount, delta: '累计题量' },
      { label: 'CF 分数', value: displayRating(profile.value.cfHandle, profile.value.cfRating), delta: displayHandle(profile.value.cfHandle) },
      { label: 'ATC 分数', value: displayRating(profile.value.atcHandle, profile.value.atcRating), delta: displayHandle(profile.value.atcHandle) }
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

async function renderTrendChart(data: TrendPoint[]) {
  if (!trendChartRef.value) {
    return;
  }

  const echarts = await loadEcharts();
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

async function renderPieChart() {
  if (!pieChartRef.value || !analytics.value) {
    return;
  }

  const echarts = await loadEcharts();
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
        data: analytics.value.tags.map((item) => ({
          name: item.tag,
          value: item.count
        }))
      }
    ]
  });
}

async function refreshTrendData() {
  trendData.value = await fetchTrend().catch(() => trendData.value);
  await renderTrendChart(trendData.value);
}

async function waitForMySyncJob(jobId: string) {
  for (let attempt = 0; attempt < 180; attempt += 1) {
    const job = await fetchMyOjSyncJob(jobId);
    if (job.status === 'SUCCESS') {
      return job.profile;
    }
    if (job.status === 'FAILED') {
      throw new Error(job.message || '真实 OJ 数据同步失败');
    }
    await sleep(2000);
  }
  throw new Error('真实 OJ 数据同步超时，请稍后刷新页面查看结果');
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

function openContactEmailDialog() {
  contactEmailForm.email = contactEmail.value ?? '';
  contactEmailDialogVisible.value = true;
}

async function submitBinding() {
  const valid = await bindingFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  bindingSubmitting.value = true;
  try {
    const cfHandle = bindingForm.cfHandle.trim() || null;
    const atcHandle = bindingForm.atcHandle.trim() || null;
    profile.value = await updatePlatformBinding({
      cfHandle,
      atcHandle
    });
    const job = await startMyOjSyncJob();
    const syncedProfile = await waitForMySyncJob(job.jobId);
    if (syncedProfile) {
      profile.value = syncedProfile;
    }
    await refreshTrendData();
    analytics.value = await fetchDashboardAnalytics().catch(() => analytics.value);
    contestHistory.value = await fetchMyContestHistory().catch(() => contestHistory.value);
    await renderPieChart();
    bindingDialogVisible.value = false;
    ElMessage.success('平台账号绑定已更新，真实 OJ 数据同步完成');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '平台账号绑定更新失败，请检查账号后重试');
  } finally {
    bindingSubmitting.value = false;
  }
}

async function onSyncOjProfile() {
  if (!profile.value?.cfHandle && !profile.value?.atcHandle) {
    ElMessage.warning('请先绑定 Codeforces 或 AtCoder 账号');
    return;
  }

  syncingOj.value = true;
  try {
    const job = await startMyOjSyncJob();
    const syncedProfile = await waitForMySyncJob(job.jobId);
    if (syncedProfile) {
      profile.value = syncedProfile;
    }
    await refreshTrendData();
    analytics.value = await fetchDashboardAnalytics().catch(() => analytics.value);
    contestHistory.value = await fetchMyContestHistory().catch(() => contestHistory.value);
    await renderPieChart();
    ElMessage.success('真实 OJ 数据同步完成');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '真实 OJ 数据同步失败，请稍后重试');
  } finally {
    syncingOj.value = false;
  }
}

async function handleImportAtcSubmissions(options: UploadRequestOptions) {
  if (!profile.value?.atcHandle) {
    ElMessage.warning('请先绑定 AtCoder 账号再导入提交记录');
    const uploadError = {
      name: 'UploadAjaxError',
      message: '未绑定 AtCoder 账号',
      status: 400,
      method: 'post',
      url: '/api/profile/me/import-atc-submissions'
    } as Parameters<NonNullable<UploadRequestOptions['onError']>>[0];
    options.onError?.(uploadError);
    return;
  }

  const file = options.file as File;
  const fileName = file.name.toLowerCase();
  if (!fileName.endsWith('.json')) {
    ElMessage.warning('请上传 AtCoder 导出的 JSON 文件');
    const uploadError = {
      name: 'UploadAjaxError',
      message: '文件格式不正确',
      status: 400,
      method: 'post',
      url: '/api/profile/me/import-atc-submissions'
    } as Parameters<NonNullable<UploadRequestOptions['onError']>>[0];
    options.onError?.(uploadError);
    return;
  }

  importingAtc.value = true;
  try {
    profile.value = await importMyAtcSubmissions(file);
    await refreshTrendData();
    analytics.value = await fetchDashboardAnalytics().catch(() => analytics.value);
    await renderPieChart();
    ElMessage.success('AtCoder 提交记录导入完成');
    options.onSuccess?.(profile.value);
  } catch (error) {
    const uploadError = {
      name: 'UploadAjaxError',
      message: error instanceof Error ? error.message : '导入失败',
      status: 500,
      method: 'post',
      url: '/api/profile/me/import-atc-submissions'
    } as Parameters<NonNullable<UploadRequestOptions['onError']>>[0];
    options.onError?.(uploadError);
  } finally {
    importingAtc.value = false;
  }
}

async function submitContactEmail() {
  const valid = await contactEmailFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  contactEmailSubmitting.value = true;
  try {
    const response = await updateMyContactEmail({
      email: contactEmailForm.email.trim() || null
    });
    contactEmail.value = response.email;
    contactEmailDialogVisible.value = false;
    ElMessage.success('绑定邮箱已更新');
  } catch {
    ElMessage.error('绑定邮箱更新失败，请稍后重试');
  } finally {
    contactEmailSubmitting.value = false;
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
    const analyticsPromise = isStudent.value
      ? fetchDashboardAnalytics().catch(() => null)
      : Promise.resolve(null);
    const contestHistoryPromise = isStudent.value
      ? fetchMyContestHistory().catch(() => [])
      : Promise.resolve([]);
    const teamPromise = isStudent.value
      ? fetchMyTeam().catch(() => null)
      : fetchCoachTeams().catch(() => []);
    const contactEmailPromise = fetchMyContactEmail().catch(() => ({ email: null }));

    const [profileResult, trendResult, contestResult, analyticsResult, teamResult, contestHistoryResult, contactEmailResult] = await Promise.all([
      profilePromise,
      fetchTrend().catch(() => fallbackTrend),
      fetchContests().catch(() => []),
      analyticsPromise,
      teamPromise,
      contestHistoryPromise,
      contactEmailPromise
    ]);

    profile.value = profileResult;
    trendData.value = trendResult;
    contests.value = contestResult;
    analytics.value = analyticsResult;
    contestHistory.value = contestHistoryResult as OjContestHistoryItem[];
    contactEmail.value = contactEmailResult.email;
    if (isStudent.value) {
      myTeam.value = teamResult as TeamInfo | null;
    } else {
      coachTeams.value = teamResult as TeamInfo[];
    }

    await nextTick();
    await renderTrendChart(trendResult);
    if (analytics.value) {
      await renderPieChart();
    }
    window.addEventListener('resize', handleResize);
    window.addEventListener('focus', refreshTeamInfo);
    document.addEventListener('visibilitychange', refreshTeamInfo);
  } finally {
    profileLoading.value = false;
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  window.removeEventListener('focus', refreshTeamInfo);
  document.removeEventListener('visibilitychange', refreshTeamInfo);
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
      <div class="header-actions">
        <template v-if="isStudent">
          <el-button :loading="syncingOj" type="success" @click="onSyncOjProfile">同步真实 OJ</el-button>
          <el-upload
            :show-file-list="false"
            accept=".json,application/json"
            :http-request="handleImportAtcSubmissions"
          >
            <el-button :loading="importingAtc" type="warning">导入 ATC 提交</el-button>
          </el-upload>
          <el-button type="primary" @click="openBindingDialog">绑定 CF / ATC 账号</el-button>
          <el-button type="primary" plain @click="openContactEmailDialog">绑定邮箱</el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="openContactEmailDialog">绑定邮箱</el-button>
        </template>
      </div>
    </div>

    <section v-if="isStudent" class="section-card glass-panel profile-summary" v-loading="profileLoading">
      <div class="summary-main">
        <h3>{{ profile?.realName || '未设置姓名' }}</h3>
        <p>{{ profile?.grade || '-' }}级 · {{ profile?.major || '-' }}</p>
        <p class="profile-hint">同步真实 OJ 会自动拉取 CF 与可用的 ATC 做题/比赛数据；若 ATC 公共接口不可用，可继续导入 JSON 作为兜底。</p>
      </div>
      <div class="summary-tags">
        <el-tag type="success">CF: {{ displayHandle(profile?.cfHandle) }}</el-tag>
        <el-tag type="warning">ATC: {{ displayHandle(profile?.atcHandle) }}</el-tag>
        <el-tag type="info">邮箱: {{ contactEmail || '未绑定' }}</el-tag>
      </div>
    </section>

    <div class="metric-grid">
      <article v-for="metric in metricCards" :key="metric.label" class="metric-card glass-panel">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <div>{{ metric.delta }}</div>
      </article>
    </div>

    <section class="section-card glass-panel team-summary">
      <div class="card-header">
        <h3>{{ isStudent ? '我的队伍' : '名下队伍' }}</h3>
      </div>

      <template v-if="isStudent">
        <div class="team-info-card">
          <div class="team-info-head">
            <strong>{{ myTeam?.name || '无' }}</strong>
            <el-tag type="primary">队伍 ID: {{ myTeam?.id || '无' }}</el-tag>
          </div>
          <div class="coach-section">
            <h4>教练</h4>
            <div class="coach-card">
              <div class="coach-avatar">{{ (myTeam?.coachName || '无').slice(0, 1) }}</div>
              <div class="coach-meta">
                <div class="coach-name-row">
                  <strong>{{ myTeam?.coachName || '无' }}</strong>
                  <el-tag size="small" type="success">教练</el-tag>
                </div>
                <p>{{ myTeam?.coachName ? '当前已绑定教练' : '未指定教练' }}</p>
              </div>
            </div>
          </div>
          <div v-if="myTeam?.members?.length" class="member-section">
            <h4>成员</h4>
            <div class="member-grid">
              <article
                v-for="slot in buildMemberSlots(myTeam.members)"
                :key="slot.key"
                class="member-card"
                :class="{ placeholder: slot.placeholder }"
              >
                <template v-if="!slot.placeholder">
                  <div class="member-avatar">{{ slot.member!.realName.slice(0, 1) }}</div>
                  <div class="member-meta">
                    <div class="member-name-row">
                      <strong>{{ slot.member!.realName }}</strong>
                      <el-tag v-if="slot.member!.role === 'CAPTAIN'" size="small" type="primary">队长</el-tag>
                      <el-tag v-else size="small" effect="plain">队员</el-tag>
                    </div>
                    <p>{{ slot.member!.username }}</p>
                  </div>
                </template>
                <template v-else>
                  <button
                    class="member-placeholder-button"
                    :class="{ active: isTeamCaptain }"
                    :disabled="!isTeamCaptain"
                    type="button"
                    @click="onAddMemberFromDashboard"
                  >
                    <div class="member-placeholder">+</div>
                  </button>
                </template>
              </article>
            </div>
          </div>
          <p v-else>队员：无</p>
        </div>
      </template>

      <template v-else>
        <div v-if="coachTeams.length" class="team-summary-list">
          <article v-for="team in coachTeams" :key="team.id" class="team-info-card">
            <div class="team-info-head">
              <strong>{{ team.name }}</strong>
              <el-tag type="primary">队伍 ID: {{ team.id }}</el-tag>
            </div>
            <p>成员人数：{{ team.members.length }}</p>
            <div class="member-section">
              <h4>成员</h4>
              <div class="member-grid">
                <article
                  v-for="slot in buildMemberSlots(team.members)"
                  :key="slot.key"
                  class="member-card"
                  :class="{ placeholder: slot.placeholder }"
                >
                  <template v-if="!slot.placeholder">
                    <div class="member-avatar">{{ slot.member!.realName.slice(0, 1) }}</div>
                    <div class="member-meta">
                      <div class="member-name-row">
                        <strong>{{ slot.member!.realName }}</strong>
                        <el-tag v-if="slot.member!.role === 'CAPTAIN'" size="small" type="primary">队长</el-tag>
                        <el-tag v-else size="small" effect="plain">队员</el-tag>
                      </div>
                      <p>{{ slot.member!.username }}</p>
                    </div>
                  </template>
                  <template v-else>
                    <button class="member-placeholder-button" disabled type="button">
                      <div class="member-placeholder">+</div>
                    </button>
                  </template>
                </article>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无名下队伍" :image-size="80" />
      </template>
    </section>

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
          <h3>题型分布</h3>
        </div>
        <div ref="pieChartRef" class="chart-box pie-box" />
      </section>

      <section v-else class="section-card glass-panel coach-panel">
        <div class="card-header">
          <h3>教练工作台</h3>
        </div>
        <div class="coach-mail-card">
          <div>
            <h4>绑定邮箱</h4>
            <p>{{ contactEmail || '未设置，比赛提醒、教练任务、邀请通知会回退到系统默认收件人' }}</p>
          </div>
          <el-button type="primary" plain @click="openContactEmailDialog">修改</el-button>
        </div>
        <ul>
          <li>优先关注 3 天内无提交队员</li>
          <li>对高频异常提交保持邮件提醒开启</li>
          <li>定期核对组队、比赛提醒和教练任务下发情况</li>
        </ul>
      </section>
    </div>

    <section v-if="isStudent && analytics" class="section-card glass-panel" style="margin-top: 18px;">
      <div class="card-header">
        <h3>做题分段统计</h3>
        <el-tag type="warning">隐藏分 {{ analytics.hiddenRating }}</el-tag>
      </div>
      <el-table :data="analytics.buckets" empty-text="暂无统计数据">
        <el-table-column prop="rangeLabel" label="分数段" min-width="160" />
        <el-table-column prop="solvedCount" label="完成题数" min-width="120" />
        <el-table-column prop="percentage" label="占比" min-width="120">
          <template #default="{ row }">{{ row.percentage }}%</template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="isStudent && analytics" class="section-card glass-panel" style="margin-top: 18px;">
      <div class="card-header">
        <h3>题目明细分析</h3>
        <el-tag>{{ analytics.recentSolved.length }} 题样本</el-tag>
      </div>
      <el-table :data="analytics.recentSolved" empty-text="暂无题目明细">
        <el-table-column label="题号" min-width="130">
          <template #default="{ row }">
            <a v-if="resolveProblemUrl(row.problemCode)" :href="resolveProblemUrl(row.problemCode)!" target="_blank" rel="noreferrer">
              {{ row.problemCode }}
            </a>
            <template v-else>{{ row.problemCode }}</template>
          </template>
        </el-table-column>
        <el-table-column label="题目" min-width="220">
          <template #default="{ row }">
            <a v-if="resolveProblemUrl(row.problemCode)" :href="resolveProblemUrl(row.problemCode)!" target="_blank" rel="noreferrer">
              {{ row.title }}
            </a>
            <template v-else>{{ row.title }}</template>
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="Rating" min-width="100" />
        <el-table-column prop="tag" label="标签" min-width="120" />
        <el-table-column prop="bucketLabel" label="分段" min-width="120" />
      </el-table>
    </section>

    <section class="section-card glass-panel" style="margin-top: 18px;">
      <div class="card-header">
        <h3>{{ isStudent ? '真实比赛经历' : '近期比赛经历' }}</h3>
      </div>
      <template v-if="isStudent">
        <el-timeline>
          <el-timeline-item
            v-for="item in contestHistory"
            :key="item.id"
            :timestamp="`${item.platform} · ${item.contestTime}`"
            placement="top"
          >
            <a :href="item.contestUrl" target="_blank" rel="noreferrer">{{ item.contestName }}</a>
            <div class="contest-history-meta">
              <span>排名 {{ item.rankNo ?? '-' }}</span>
              <span>新分 {{ item.newRating ?? '-' }}</span>
              <span>变化 {{ item.ratingChange ?? '-' }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-if="contestHistory.length === 0" description="暂无真实比赛记录" :image-size="80" />
      </template>
      <template v-else>
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
      </template>
    </section>

    <el-dialog v-if="isStudent" v-model="bindingDialogVisible" title="绑定平台账号" width="460px">
      <el-form ref="bindingFormRef" :model="bindingForm" :rules="bindingRules" label-position="top">
        <el-form-item label="Codeforces ID" prop="cfHandle">
          <el-input v-model="bindingForm.cfHandle" maxlength="64" placeholder="例如 tourist；留空表示解绑" />
        </el-form-item>
        <el-form-item label="AtCoder ID" prop="atcHandle">
          <el-input v-model="bindingForm.atcHandle" maxlength="64" placeholder="例如 rng_58；留空表示解绑" />
        </el-form-item>
        <p class="binding-tip">两个账号都留空时会清空已绑定 OJ 信息，页面显示为“无”。</p>
      </el-form>
      <template #footer>
        <el-button @click="bindingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindingSubmitting" @click="submitBinding">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="contactEmailDialogVisible" title="绑定邮箱" width="460px">
      <el-form ref="contactEmailFormRef" :model="contactEmailForm" :rules="contactEmailRules" label-position="top">
        <el-form-item label="邮箱地址" prop="email">
          <el-input
            v-model="contactEmailForm.email"
            maxlength="128"
            placeholder="例如 user@example.com"
          />
        </el-form-item>
        <p class="coach-mail-tip">绑定后，比赛提醒、教练任务、组队邀请和异常通知会优先发送到这里；留空时回退到系统默认收件人。</p>
      </el-form>
      <template #footer>
        <el-button @click="contactEmailDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="contactEmailSubmitting" @click="submitContactEmail">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-if="isStudent" v-model="inviteDialogVisible" title="邀请队员" width="420px">
      <el-input v-model="inviteDialogUsername" placeholder="输入要邀请的用户名" />
      <template #footer>
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmitInviteDialog">发送邀请</el-button>
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

.binding-tip {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.summary-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.header-actions {
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

.team-summary {
  margin-top: 18px;
}

.contest-history-meta {
  margin-top: 8px;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: var(--muted);
}

.team-summary-list {
  display: grid;
  gap: 14px;
}

.team-info-card {
  border: 1px solid var(--card-border);
  border-radius: 14px;
  padding: 14px;
}

.team-info-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.team-info-card p {
  margin: 0 0 12px;
  color: var(--muted);
}

.coach-section {
  margin-bottom: 14px;
}

.coach-section h4 {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
}

.coach-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 10px;
  border-radius: 14px;
  background: rgba(29, 91, 143, 0.04);
}

.coach-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #f0b55a, #d97b5a);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  flex: 0 0 auto;
}

.coach-meta {
  min-width: 0;
}

.coach-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.coach-meta strong {
  color: #16b7a7;
  font-size: 20px;
  font-weight: 500;
}

.coach-meta p {
  margin: 0;
  color: #9d9d9d;
  font-size: 15px;
}

.member-section h4 {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
}

.member-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.member-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 10px;
  border-radius: 14px;
  background: rgba(29, 91, 143, 0.04);
  min-height: 80px;
}

.member-card.placeholder {
  justify-content: center;
  background: rgba(29, 91, 143, 0.02);
  border: 1px dashed rgba(29, 91, 143, 0.18);
}

.member-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #79d2bf, #5f8dcb);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  flex: 0 0 auto;
}

.member-placeholder {
  font-size: 36px;
  line-height: 1;
  color: #7aaecb;
  font-weight: 300;
}

.member-placeholder-button {
  width: 100%;
  min-height: 56px;
  border: 0;
  background: transparent;
  display: grid;
  place-items: center;
  cursor: default;
}

.member-placeholder-button.active {
  cursor: pointer;
}

.member-meta {
  min-width: 0;
}

.member-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.member-meta strong {
  color: #16b7a7;
  font-size: 20px;
  font-weight: 500;
}

.member-meta p {
  margin: 0;
  color: #9d9d9d;
  font-size: 15px;
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

.coach-mail-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 14px 16px;
  margin-bottom: 16px;
  border-radius: 14px;
  background: rgba(29, 91, 143, 0.05);
  border: 1px solid rgba(29, 91, 143, 0.1);
}

.coach-mail-card h4 {
  margin: 0 0 6px;
  font-size: 16px;
}

.coach-mail-card p {
  margin: 0;
  color: var(--muted);
}

.coach-mail-tip {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
}

.coach-panel li + li {
  margin-top: 10px;
}

a {
  color: #1d5b8f;
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

@media (max-width: 960px) {
  .member-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .profile-summary {
    flex-direction: column;
    align-items: flex-start;
  }

  .coach-mail-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
