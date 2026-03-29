<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
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
const inviteDialogVisible = ref(false);
const inviteDialogUsername = ref('');

const isCoach = computed(() => authStore.role === 'coach');
const myUserId = computed(() => authStore.user?.id ?? -1);
const isCaptain = computed(() => {
  if (!team.value) return false;
  return team.value.members.some((m) => m.userId === myUserId.value && m.role === 'CAPTAIN');
});

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
  team.value = await fetchMyTeam();
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

function onAddMember() {
  if (!team.value || !isCaptain.value || team.value.members.length >= 3) {
    return;
  }

  inviteDialogUsername.value = '';
  inviteDialogVisible.value = true;
}

async function onSubmitInviteDialog() {
  if (!team.value || !inviteDialogUsername.value.trim()) return;
  await inviteTeamMember(team.value.id, { username: inviteDialogUsername.value.trim() });
  team.value = await fetchMyTeam();
  inviteDialogUsername.value = '';
  inviteDialogVisible.value = false;
  ElMessage.success('邀请已发送');
  invites.value = await fetchMyTeamInvites();
}

async function refreshOnFocus() {
  await reloadAll();
}

onMounted(reloadAll);

onMounted(() => {
  window.addEventListener('focus', refreshOnFocus);
  document.addEventListener('visibilitychange', refreshOnFocus);
});

onBeforeUnmount(() => {
  window.removeEventListener('focus', refreshOnFocus);
  document.removeEventListener('visibilitychange', refreshOnFocus);
});
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
          <div class="member-section">
            <h4>成员</h4>
            <div class="member-grid">
              <article
                v-for="slot in buildMemberSlots(item.members)"
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
    </section>

    <template v-else>
      <section class="section-card glass-panel" v-loading="loading">
        <template v-if="team">
          <div class="team-card">
            <header>
              <strong>{{ team.name }}</strong>
              <span>队伍 ID: {{ team.id }}</span>
            </header>
            <div class="coach-section">
              <h4>教练</h4>
              <div class="coach-card">
                <div class="coach-avatar">{{ (team.coachName ?? '无').slice(0, 1) }}</div>
                <div class="coach-meta">
                  <div class="coach-name-row">
                    <strong>{{ team.coachName ?? '无' }}</strong>
                    <el-tag size="small" type="success">教练</el-tag>
                  </div>
                  <p>{{ team.coachName ? '当前已绑定教练' : '未指定教练' }}</p>
                </div>
              </div>
            </div>
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
                    <button
                      class="member-placeholder-button"
                      :class="{ active: isCaptain }"
                      :disabled="!isCaptain"
                      type="button"
                      @click="onAddMember"
                    >
                      <div class="member-placeholder">+</div>
                    </button>
                  </template>
                </article>
              </div>
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

    <el-dialog v-model="inviteDialogVisible" title="邀请队员" width="420px">
      <el-input v-model="inviteDialogUsername" placeholder="输入要邀请的用户名" />
      <template #footer>
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmitInviteDialog">发送邀请</el-button>
      </template>
    </el-dialog>
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

.coach-section,
.member-section {
  margin-top: 14px;
}

.coach-section h4,
.member-section h4 {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
}

.coach-card,
.member-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 10px;
  border-radius: 14px;
  background: rgba(29, 91, 143, 0.04);
  min-height: 80px;
}

.coach-avatar,
.member-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  flex: 0 0 auto;
}

.coach-avatar {
  background: linear-gradient(135deg, #f0b55a, #d97b5a);
}

.member-avatar {
  background: linear-gradient(135deg, #79d2bf, #5f8dcb);
}

.coach-meta,
.member-meta {
  min-width: 0;
}

.coach-name-row,
.member-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.coach-meta strong,
.member-meta strong {
  color: #16b7a7;
  font-size: 20px;
  font-weight: 500;
}

.coach-meta p,
.member-meta p {
  margin: 0;
  color: #9d9d9d;
  font-size: 15px;
}

.member-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.member-card.placeholder {
  justify-content: center;
  background: rgba(29, 91, 143, 0.02);
  border: 1px dashed rgba(29, 91, 143, 0.18);
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

.member-placeholder {
  font-size: 36px;
  line-height: 1;
  color: #7aaecb;
  font-weight: 300;
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

@media (max-width: 960px) {
  .member-grid {
    grid-template-columns: 1fr;
  }
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
