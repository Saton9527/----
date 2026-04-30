<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchPointLogs } from '@/api/point';
import type { PointLogItem } from '@/types/point';

const loading = ref(false);
const logs = ref<PointLogItem[]>([]);

async function loadLogs() {
  loading.value = true;
  try {
    logs.value = await fetchPointLogs();
  } catch {
    ElMessage.error('积分流水加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

onMounted(loadLogs);
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>积分流水</h1>
      </div>
    </div>

    <section class="section-card glass-panel">
      <el-table :data="logs" v-loading="loading" empty-text="暂无积分记录">
        <el-table-column prop="sourceType" label="来源类型" min-width="140" />
        <el-table-column prop="reason" label="说明" min-width="280" />
        <el-table-column prop="points" label="积分" min-width="100" />
        <el-table-column prop="createdAt" label="时间" min-width="180" />
      </el-table>
    </section>
  </div>
</template>
