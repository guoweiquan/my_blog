<template>
  <section class="auth-page">
    <el-card class="auth-card" shadow="hover">
      <h2>登录</h2>
            <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleSubmit">
        <el-form-item label="用户名 / 邮箱" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名或邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="authStore.loading" style="width: 100%" @click="handleSubmit">
            登录
          </el-button>
        </el-form-item>
        <p class="tips">
          还没有账号？<RouterLink to="/register">立即注册</RouterLink>
        </p>
      </el-form>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const formRef = ref<FormInstance>();
const form = reactive({ username: '', password: '' });
const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return;
    try {
      await authStore.login({ ...form });
      ElMessage.success('登录成功');
      const redirect = (route.query.redirect as string) || '/';
      router.replace(redirect);
    } catch (error: any) {
      ElMessage.error(error?.message || '登录失败，请稍后再试');
    }
  });
};
</script>

<style scoped>
.auth-page {
  min-height: 70vh;
  display: flex;
  justify-content: center;
  align-items: center;
}

.auth-card {
  width: 360px;
}

.tips {
  text-align: center;
  color: #909399;
}
</style>
