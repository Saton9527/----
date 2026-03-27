<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchStudents } from '@/api/student';
import type { StudentItem } from '@/types/student';

const loading = ref(false);
const students = ref<StudentItem[]>([]);

onMounted(async () => {
  loading.value = true;
  try {
    students.value = await fetchStudents();
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div>
    <div class="page-heading">
      <div>
        <h1>学生管理</h1>
      </div>
    </div>

    <section class="section-card glass-panel">
      <el-table :data="students" v-loading="loading" empty-text="暂无数据">
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="grade" label="年级" min-width="90" />
        <el-table-column prop="major" label="专业" min-width="180" />
        <el-table-column prop="cfHandle" label="Codeforces ID" min-width="140" />
        <el-table-column prop="atcHandle" label="AtCoder ID" min-width="130" />
        <el-table-column prop="cfRating" label="CF 分数" min-width="100" />
        <el-table-column prop="atcRating" label="ATC 分数" min-width="100" />
        <el-table-column prop="solvedCount" label="做题数" min-width="90" />
        <el-table-column prop="totalPoints" label="积分" min-width="90" />
      </el-table>
    </section>
  </div>
</template>
