<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import { fetchAlerts, fetchMyAlerts, updateMyAlertFeedback } from '@/api/alert';
import type { AlertItem } from '@/types/alert';

const authStore = useAuthStore();
const isCoach = computed(() => authStore.role === 'coach');

const loading = ref(false);
const submitting = ref(false);
const alerts = ref<AlertItem[]>([]);
const feedbackDialogVisible = ref(false);
const editingAlert = ref<AlertItem | null>(null);
const feedbackForm = reactive({
  feedback: ''
});

function riskTag(level: AlertItem['riskLevel']) {
  if (level === 'HIGH') return 'danger';
  if (level === 'MEDIUM') return 'warning';
  return 'info';
}

function statusTag(status: AlertItem['status']) {
  return status === 'OPEN' ? 'warning' : 'success';
}

async function loadAlerts() {
  loading.value = true;
  try {
    alerts.value = isCoach.value ? await fetchAlerts() : await fetchMyAlerts();
  } catch {
    ElMessage.error(isCoach.value ? '异常提醒加载失败，请稍后重试' : '异常反馈列表加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

function openFeedbackDialog(item: AlertItem) {
  editingAlert.value = item;
  feedbackForm.feedback = item.studentFeedback ?? '';
  feedbackDialogVisible.value = true;
}

async function submitFeedback() {
  if (!editingAlert.value || submitting.value) {
    return;
  }

  submitting.value = true;
  try {
    const updated = await updateMyAlertFeedback(editingAlert.value.id, {
      feedback: feedbackForm.feedback.trim() || null
    });
    alerts.value = alerts.value.map((item) => (item.id === updated.id ? updated : item));
    feedbackDialogVisible.value = false;
    ElMessage.success(updated.studentFeedback ? '异常反馈已保存' : '异常反馈已清空');
  } catch {
    ElMessage.error('异常反馈保存失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  void loadAlerts();
});
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>{{ isCoach ? '异常提醒' : '异常反馈' }}</h1>
        <p v-if="isCoach">这里集中查看系统检测到的异常训练记录，便于教练跟进和邮件通知。</p>
        <p v-else>这里显示系统标记到的异常训练记录，你可以补充说明背景，供教练后续核对。</p>
      </div>
    </div>

    <section class="section-card glass-panel" v-loading="loading">
      <el-table :data="alerts" empty-text="暂无数据">
        <template v-if="isCoach">
          <el-table-column prop="userName" label="姓名" min-width="120" />
        </template>
        <el-table-column prop="ruleCode" label="规则" min-width="120" />
        <el-table-column label="风险" min-width="100">
          <template #default="{ row }">
            <el-tag :type="riskTag(row.riskLevel)">{{ row.riskLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="hitTime" label="触发时间" min-width="180" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="邮件发送" min-width="160">
          <template #default="{ row }">
            {{ row.mailSentAt || '未发送' }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="触发说明" min-width="240" />
        <el-table-column prop="suspiciousProblems" label="可疑题目" min-width="220" />
        <el-table-column prop="suggestion" label="处理建议" min-width="260" />
        <el-table-column v-if="!isCoach" label="我的反馈" min-width="240">
          <template #default="{ row }">
            <div class="feedback-cell">
              <span>{{ row.studentFeedback || '未填写' }}</span>
              <small v-if="row.feedbackAt">更新于 {{ row.feedbackAt }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="!isCoach" label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openFeedbackDialog(row)">
              {{ row.studentFeedback ? '修改反馈' : '填写反馈' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column v-else label="学生反馈" min-width="240">
          <template #default="{ row }">
            <div class="feedback-cell">
              <span>{{ row.studentFeedback || '未反馈' }}</span>
              <small v-if="row.feedbackAt">更新于 {{ row.feedbackAt }}</small>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="feedbackDialogVisible" title="异常反馈" width="520px">
      <el-input
        v-model="feedbackForm.feedback"
        type="textarea"
        :rows="6"
        maxlength="500"
        show-word-limit
        resize="none"
        placeholder="补充说明这次异常记录的背景，例如专题补题、赛后补交、多人讨论后独立完成等。"
      />
      <template #footer>
        <el-button @click="feedbackDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitFeedback">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.feedback-cell {
  display: grid;
  gap: 6px;
}

.feedback-cell span {
  line-height: 1.6;
}

.feedback-cell small {
  color: var(--muted);
}
</style>
