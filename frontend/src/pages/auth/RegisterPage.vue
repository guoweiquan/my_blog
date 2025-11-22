<template>
  <section class="auth-page">
    <el-card class="auth-card" shadow="hover">
      <h2>注册</h2>
      <p>注册功能将在下个阶段实现，当前仅开放管理员创建账号。</p>
      <RouterLink to="/login">返回登录</RouterLink>
    </el-card>
  </section>
</template>

<script setup lang="ts"></script>

<style scoped>
.auth-page {
  min-height: 70vh;
  display: flex;
  justify-content: center;
  align-items: center;
}

.auth-card {
  width: 360px;
  text-align: center;
}
</style>
