<template>
  <footer class="app-footer">
    <p>© {{ new Date().getFullYear() }} 我的博客 · 架构：Vue 3 + Spring Boot + MySQL + Redis</p>
  </footer>
</template>

<script setup lang="ts"></script>

<style scoped>
.app-footer {
  padding: 24px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
</style>
