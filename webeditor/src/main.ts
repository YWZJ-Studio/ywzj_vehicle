import {createApp} from 'vue';
import {createPinia} from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import App from './App.vue';
import './assets/styles/main.css';
import {setupMonacoWorkers} from './utils/monacoWorker';

// 配置 Monaco Editor 的 Web Workers
setupMonacoWorkers();

const app = createApp(App);
const pinia = createPinia();

// Register all icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(pinia);
app.use(ElementPlus);

app.mount('#app');
