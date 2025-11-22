<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover">
          <p class="label">{{ card.label }}</p>
          <p class="value">{{ card.value }}</p>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="placeholder" shadow="never">
      <h3>趋势图</h3>
      <p>后续接入 PV/UV、发文量、评论量等可视化数据。</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const cards = computed(() => [
  { label: '今日 PV', value: 0 },
  { label: '今日 UV', value: 0 },
  { label: '文章总数', value: 0 },
  { label: '评论待审核', value: 0 }
]);
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.label {
  color: #909399;
  margin: 0 0 8px;
}

.value {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.placeholder {
  min-height: 240px;
}
</style>
