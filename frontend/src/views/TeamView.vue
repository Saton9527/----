<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import {
  acceptTeamInvite,
  assignTeamCoach,
  createTeam,
  fetchCoachTeams,
  fetchMyTeam,
  fetchMyTeamInvites,
  inviteTeamMember,
  rejectTeamInvite
} from '@/api/team';
import type { TeamInfo, TeamInvite } from '@/types/team';

const authStore = useAuthStore();
const loading = ref(false);
const team = ref<TeamInfo | null>(null);
const coachTeams = ref<TeamInfo[]>([]);
const invites = ref<TeamInvite[]>([]);

const createTeamName = ref('');
const inviteUsername = ref('');
const coachUsername = ref('');

const isCoach = computed(() => authStore.role === 'coach');
const myUserId = computed(() => authStore.user?.id ?? -1);
const isCaptain = computed(() => {
  if (!team.value) return false;
  return team.value.members.some((m) => m.userId === myUserId.value && m.role === 'CAPTAIN');
});

async function reloadStudentData() {
  team.value = await fetchMyTeam();
  invites.value = await fetchMyTeamInvites();
}

async function reloadCoachData() {
  coachTeams.value = await fetchCoachTeams();
}

async function reloadAll() {
  loading.value = true;
  try {
    if (isCoach.value) {
      await reloadCoachData();
    } else {
      await reloadStudentData();
    }
  } finally {
    loading.value = false;
  }
}

async function onCreateTeam() {
  if (!createTeamName.value.trim()) return;
  const created = await createTeam({ name: createTeamName.value.trim() });
  team.value = created;
  createTeamName.value = '';
  ElMessage.success('队伍创建成功');
}

async function onInviteMember() {
  if (!team.value || !inviteUsername.value.trim()) return;
  await inviteTeamMember(team.value.id, { username: inviteUsername.value.trim() });
  inviteUsername.value = '';
  ElMessage.success('邀请已发送');
  invites.value = await fetchMyTeamInvites();
}

async function onAssignCoach() {
  if (!team.value || !coachUsername.value.trim()) return;
  team.value = await assignTeamCoach(team.value.id, { coachUsername: coachUsername.value.trim() });
  coachUsername.value = '';
  ElMessage.success('教练已绑定');
}

async function onAcceptInvite(inviteId: number) {
  team.value = await acceptTeamInvite(inviteId);
  invites.value = await fetchMyTeamInvites();
  ElMessage.success('已加入队伍');
}

async function onRejectInvite(inviteId: number) {
  await rejectTeamInvite(inviteId);
  invites.value = await fetchMyTeamInvites();
  ElMessage.success('已拒绝邀请');
}

onMounted(reloadAll);
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>组队管理</h1>
      </div>
    </div>

    <section v-if="isCoach" class="section-card glass-panel" v-loading="loading">
      <el-empty v-if="!coachTeams.length" description="暂无名下队伍" />
      <div v-else class="team-list">
        <article v-for="item in coachTeams" :key="item.id" class="team-card">
          <header>
            <strong>{{ item.name }}</strong>
            <span>队伍 ID: {{ item.id }}</span>
          </header>
          <div class="member-list">
            <el-tag v-for="member in item.members" :key="member.userId" effect="plain">
              {{ member.realName }} / {{ member.role }}
            </el-tag>
          </div>
        </article>
      </div>
    </section>

    <template v-else>
      <section class="section-card glass-panel" v-loading="loading">
        <template v-if="team">
          <div class="team-card">
            <header>
              <strong>{{ team.name }}</strong>
              <span>队伍 ID: {{ team.id }}</span>
            </header>
            <p>教练：{{ team.coachName ?? '未指定' }}</p>
            <div class="member-list">
              <el-tag v-for="member in team.members" :key="member.userId" effect="plain">
                {{ member.realName }} / {{ member.role }}
              </el-tag>
            </div>
          </div>

          <div v-if="isCaptain" class="action-grid">
            <el-input v-model="inviteUsername" placeholder="输入要邀请的用户名" />
            <el-button type="primary" @click="onInviteMember">邀请队员</el-button>
            <el-input v-model="coachUsername" placeholder="输入教练用户名" />
            <el-button @click="onAssignCoach">指定教练</el-button>
          </div>
        </template>

        <div v-else class="create-row">
          <el-input v-model="createTeamName" placeholder="输入队伍名称" />
          <el-button type="primary" @click="onCreateTeam">创建队伍（你将成为队长）</el-button>
        </div>
      </section>

      <section class="section-card glass-panel invite-section" v-loading="loading">
        <h3>我的邀请</h3>
        <el-empty v-if="!invites.length" description="暂无待处理邀请" />
        <div v-else class="invite-list">
          <div v-for="invite in invites" :key="invite.id" class="invite-item">
            <span>{{ invite.teamName }} / 邀请人 {{ invite.inviterName }}</span>
            <div class="invite-actions">
              <el-button type="primary" plain @click="onAcceptInvite(invite.id)">接受</el-button>
              <el-button type="danger" plain @click="onRejectInvite(invite.id)">拒绝</el-button>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.team-list,
.invite-list {
  display: grid;
  gap: 14px;
}

.team-card {
  border: 1px solid var(--card-border);
  border-radius: 14px;
  padding: 14px;
}

.team-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.team-card p {
  margin: 6px 0 12px;
  color: var(--muted);
}

.member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.create-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.invite-section {
  margin-top: 16px;
}

.invite-section h3 {
  margin: 0 0 12px;
}

.invite-item {
  border: 1px solid var(--card-border);
  border-radius: 12px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.invite-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 760px) {
  .action-grid,
  .create-row,
  .invite-item {
    grid-template-columns: 1fr;
    display: grid;
  }

  .invite-actions {
    justify-content: flex-start;
  }
}
</style>