<template>
  <section class="not-found">
    <h1>404</h1>
    <p>抱歉，您访问的页面不存在。</p>
    <RouterLink to="/">返回首页</RouterLink>
  </section>
</template>

<script setup lang="ts"></script>

<style scoped>
.not-found {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
}

h1 {
  font-size: 72px;
  margin: 0;
}

p {
  color: #606266;
}
</style>
