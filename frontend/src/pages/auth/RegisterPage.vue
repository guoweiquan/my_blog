<template>
  <section class="auth-page">
    <el-card class="auth-card" shadow="hover">
      <h2>注册</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="可选" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="submitting" @click="handleSubmit">注册</el-button>
        </el-form-item>
        <p class="tips">
          已有账号？<RouterLink to="/login">立即登录</RouterLink>
        </p>
      </el-form>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  username: '',
  email: '',
  nickname: '',
  password: ''
});

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      await authStore.register({ ...form });
      ElMessage.success('注册成功，请登录');
      router.push({ name: 'Login' });
    } catch (error: any) {
      ElMessage.error(error?.message || '注册失败，请稍后再试');
    } finally {
      submitting.value = false;
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
