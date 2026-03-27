<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchAlerts } from '@/api/alert';
import type { AlertItem } from '@/types/alert';

const loading = ref(false);
const alerts = ref<AlertItem[]>([]);

function riskTag(level: AlertItem['riskLevel']) {
  if (level === 'HIGH') return 'danger';
  if (level === 'MEDIUM') return 'warning';
  return 'info';
}

onMounted(async () => {
  loading.value = true;
  alerts.value = await fetchAlerts();
  loading.value = false;
});
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>异常提醒</h1>
      </div>
    </div>

    <section class="section-card glass-panel">
      <el-table :data="alerts" v-loading="loading" empty-text="暂无数据">
        <el-table-column prop="userName" label="姓名" min-width="120" />
        <el-table-column prop="ruleCode" label="规则" min-width="120" />
        <el-table-column label="风险" min-width="100">
          <template #default="{ row }">
            <el-tag :type="riskTag(row.riskLevel)">{{ row.riskLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="hitTime" label="触发时间" min-width="180" />
        <el-table-column prop="status" label="状态" min-width="100" />
      </el-table>
    </section>
  </div>
</template>
