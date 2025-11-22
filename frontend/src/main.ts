import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';

import App from './App.vue';
import router from './router';
import { useAuthStore } from '@/stores/auth';

import 'element-plus/dist/index.css';
import '@/styles/index.scss';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.use(ElementPlus, { locale: zhCn });

const authStore = useAuthStore();
if (authStore.accessToken) {
  authStore.fetchProfile().catch(() => authStore.logout());
}

app.mount('#app');
