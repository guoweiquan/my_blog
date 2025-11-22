<template>
  <section>
    <el-form :model="form" label-width="120px" class="settings-form">
      <el-form-item label="站点标题">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item label="副标题">
        <el-input v-model="form.subtitle" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.description" type="textarea" rows="4" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSave">保存配置</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import { ElMessage } from 'element-plus';

const form = reactive({
  title: '我的博客',
  subtitle: '分享技术与生活',
  description: '这是一个基于 Vue3 + Spring Boot 构建的博客系统'
});

const handleSave = () => {
  ElMessage.success('配置已保存（示例）');
};
</script>

<style scoped>
.settings-form {
  max-width: 600px;
  background: #fff;
  padding: 24px;
  border-radius: 12px;
}
</style>
