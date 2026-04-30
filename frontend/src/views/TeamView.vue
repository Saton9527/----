<script setup lang="ts">
import axios from 'axios';
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
const creatingTeam = ref(false);
const invitingMember = ref(false);
const assigningCoach = ref(false);
const inviteDialogSubmitting = ref(false);
const inviteActionId = ref<number | null>(null);
const team = ref<TeamInfo | null>(null);
const coachTeams = ref<TeamInfo[]>([]);
const invites = ref<TeamInvite[]>([]);

const createTeamName = ref('');
const inviteUsername = ref('');
const coachUsername = ref('');
const inviteDialogVisible = ref(false);
const inviteDialogUsername = ref('');
const coachDialogVisible = ref(false);
const coachDialogUsername = ref('');

const isCoach = computed(() => authStore.role === 'coach');
const myUserId = computed(() => authStore.user?.id ?? -1);
const isCaptain = computed(() => {
  if (!team.value) return false;
  return team.value.members.some((m) => m.userId === myUserId.value && m.role === 'CAPTAIN');
});

function isDuplicateInviteError(error: unknown) {
  if (!axios.isAxiosError(error)) {
    return false;
  }

  const message = error.response?.data?.message;
  return error.response?.status === 409 && typeof message === 'string' && message.includes('已向该用户发送过邀请');
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

async function reloadStudentData() {
  try {
    const [myTeam, myInvites] = await Promise.all([fetchMyTeam(), fetchMyTeamInvites()]);
    team.value = myTeam;
    invites.value = myInvites;
  } catch {
    ElMessage.error('组队信息加载失败，请稍后重试');
  }
}

async function reloadCoachData() {
  try {
    coachTeams.value = await fetchCoachTeams();
  } catch {
    ElMessage.error('队伍列表加载失败，请稍后重试');
  }
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
  if (creatingTeam.value) {
    return;
  }

  const name = createTeamName.value.trim();
  if (!name) {
    ElMessage.warning('请输入队伍名称');
    return;
  }

  creatingTeam.value = true;
  try {
    const created = await createTeam({ name });
    team.value = created;
    createTeamName.value = '';
    ElMessage.success('队伍创建成功');
  } catch {
    ElMessage.error('队伍创建失败，请稍后重试');
  } finally {
    creatingTeam.value = false;
  }
}

async function onInviteMember() {
  if (invitingMember.value) {
    return;
  }

  const username = inviteUsername.value.trim();
  if (!team.value) {
    return;
  }

  if (team.value.members.length >= 3) {
    ElMessage.warning('队伍人数已满，无法继续邀请');
    return;
  }

  if (!username) {
    ElMessage.warning('请输入要邀请的用户名');
    return;
  }

  invitingMember.value = true;
  try {
    await inviteTeamMember(team.value.id, { username });
    team.value = await fetchMyTeam();
    inviteUsername.value = '';
    ElMessage.success('邀请已发送');
    invites.value = await fetchMyTeamInvites();
  } catch (error) {
    if (isDuplicateInviteError(error)) {
      ElMessage.info('已递交邀请，请等待对方处理');
    } else {
      ElMessage.error('邀请发送失败，请确认用户名是否正确');
    }
  } finally {
    invitingMember.value = false;
  }
}

async function onAssignCoach() {
  if (assigningCoach.value) {
    return;
  }

  const username = coachUsername.value.trim();
  if (!team.value) {
    return;
  }

  if (!username) {
    ElMessage.warning('请输入教练用户名');
    return;
  }

  assigningCoach.value = true;
  try {
    team.value = await assignTeamCoach(team.value.id, { coachUsername: username });
    coachUsername.value = '';
    ElMessage.success('教练已绑定');
  } catch {
    ElMessage.error('教练绑定失败，请确认教练账号是否正确');
  } finally {
    assigningCoach.value = false;
  }
}

function onAddCoach() {
  if (!team.value || !isCaptain.value || team.value.coachName) {
    return;
  }

  coachDialogUsername.value = '';
  coachDialogVisible.value = true;
}

async function onSubmitCoachDialog() {
  if (assigningCoach.value) {
    return;
  }

  const username = coachDialogUsername.value.trim();
  if (!team.value) {
    return;
  }

  if (!username) {
    ElMessage.warning('请输入教练用户名');
    return;
  }

  assigningCoach.value = true;
  try {
    team.value = await assignTeamCoach(team.value.id, { coachUsername: username });
    coachUsername.value = '';
    coachDialogUsername.value = '';
    coachDialogVisible.value = false;
    ElMessage.success('教练已绑定');
  } catch {
    ElMessage.error('教练绑定失败，请确认教练账号是否正确');
  } finally {
    assigningCoach.value = false;
  }
}

async function onAcceptInvite(inviteId: number) {
  if (inviteActionId.value === inviteId) {
    return;
  }

  inviteActionId.value = inviteId;
  try {
    team.value = await acceptTeamInvite(inviteId);
    invites.value = await fetchMyTeamInvites();
    ElMessage.success('已加入队伍');
  } catch {
    ElMessage.error('接受邀请失败，请稍后重试');
  } finally {
    inviteActionId.value = null;
  }
}

async function onRejectInvite(inviteId: number) {
  if (inviteActionId.value === inviteId) {
    return;
  }

  inviteActionId.value = inviteId;
  try {
    await rejectTeamInvite(inviteId);
    invites.value = await fetchMyTeamInvites();
    ElMessage.success('已拒绝邀请');
  } catch {
    ElMessage.error('拒绝邀请失败，请稍后重试');
  } finally {
    inviteActionId.value = null;
  }
}

function onAddMember() {
  if (!team.value || !isCaptain.value || team.value.members.length >= 3) {
    return;
  }

  inviteDialogUsername.value = '';
  inviteDialogVisible.value = true;
}

async function onSubmitInviteDialog() {
  if (inviteDialogSubmitting.value) {
    return;
  }

  const username = inviteDialogUsername.value.trim();
  if (!team.value) {
    return;
  }

  if (team.value.members.length >= 3) {
    ElMessage.warning('队伍人数已满，无法继续邀请');
    inviteDialogVisible.value = false;
    return;
  }

  if (!username) {
    ElMessage.warning('请输入要邀请的用户名');
    return;
  }

  inviteDialogSubmitting.value = true;
  try {
    await inviteTeamMember(team.value.id, { username });
    team.value = await fetchMyTeam();
    inviteDialogUsername.value = '';
    inviteDialogVisible.value = false;
    ElMessage.success('邀请已发送');
    invites.value = await fetchMyTeamInvites();
  } catch (error) {
    if (isDuplicateInviteError(error)) {
      ElMessage.info('已递交邀请，请等待对方处理');
    } else {
      ElMessage.error('邀请发送失败，请确认用户名是否正确');
    }
  } finally {
    inviteDialogSubmitting.value = false;
  }
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
          <div class="coach-section">
            <h4>教练</h4>
            <div class="coach-card">
              <div class="coach-avatar">{{ (item.coachName ?? '无').slice(0, 1) }}</div>
              <div class="coach-meta">
                <div class="coach-name-row">
                  <strong>{{ item.coachName ?? '无' }}</strong>
                  <el-tag size="small" type="success">教练</el-tag>
                </div>
                <p>{{ item.coachName ? '当前已绑定教练' : '未指定教练' }}</p>
              </div>
            </div>
          </div>
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
              <button
                v-if="!team.coachName && isCaptain"
                class="coach-card coach-card-button placeholder"
                type="button"
                @click="onAddCoach"
              >
                <div class="coach-placeholder">+</div>
                <div class="coach-meta">
                  <div class="coach-name-row">
                    <strong>添加教练</strong>
                    <el-tag size="small" type="warning">待绑定</el-tag>
                  </div>
                  <p>点击后输入教练账号</p>
                </div>
              </button>
              <div v-else class="coach-card">
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
            <el-button type="primary" :loading="invitingMember" @click="onInviteMember">邀请队员</el-button>
            <el-input v-if="team.coachName" v-model="coachUsername" placeholder="输入教练用户名" />
            <el-button v-if="team.coachName" :loading="assigningCoach" @click="onAssignCoach">指定教练</el-button>
          </div>
        </template>

        <div v-else class="create-row">
          <el-input v-model="createTeamName" placeholder="输入队伍名称" />
          <el-button type="primary" :loading="creatingTeam" @click="onCreateTeam">创建队伍（你将成为队长）</el-button>
        </div>
      </section>

      <section class="section-card glass-panel invite-section" v-loading="loading">
        <h3>我的邀请</h3>
        <el-empty v-if="!invites.length" description="暂无待处理邀请" />
        <div v-else class="invite-list">
          <div v-for="invite in invites" :key="invite.id" class="invite-item">
            <span>{{ invite.teamName }} / 邀请人 {{ invite.inviterName }}</span>
            <div class="invite-actions">
              <el-button
                type="primary"
                plain
                :loading="inviteActionId === invite.id"
                @click="onAcceptInvite(invite.id)"
              >
                接受
              </el-button>
              <el-button
                type="danger"
                plain
                :loading="inviteActionId === invite.id"
                @click="onRejectInvite(invite.id)"
              >
                拒绝
              </el-button>
            </div>
          </div>
        </div>
      </section>
    </template>

    <el-dialog v-model="inviteDialogVisible" title="邀请队员" width="420px">
      <el-input v-model="inviteDialogUsername" placeholder="输入要邀请的用户名" />
      <template #footer>
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="inviteDialogSubmitting" @click="onSubmitInviteDialog">发送邀请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="coachDialogVisible" title="添加教练" width="420px">
      <el-input v-model="coachDialogUsername" placeholder="输入教练用户名" />
      <template #footer>
        <el-button @click="coachDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigningCoach" @click="onSubmitCoachDialog">绑定教练</el-button>
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

.coach-card-button {
  width: 100%;
  border: 0;
  text-align: left;
  cursor: pointer;
}

.coach-card.placeholder {
  border: 1px dashed rgba(29, 91, 143, 0.18);
  background: rgba(29, 91, 143, 0.02);
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

.coach-placeholder {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(240, 181, 90, 0.12);
  color: #d97b5a;
  font-size: 34px;
  font-weight: 300;
  flex: 0 0 auto;
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
