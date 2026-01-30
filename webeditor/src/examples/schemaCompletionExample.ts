/**
 * 基于 Schema 的补全系统使用示例
 */

import * as monaco from 'monaco-editor';
import {schemaRegistry} from '@/utils/schemaRegistry';
import {globalCompletionRuleManager} from '@/utils/completionRules';
import {
    globalNamespaceIdProvider,
    initCompletionFromSchemas,
    registerNamespaceIdCompletion
} from '@/utils/namespaceIdCompletion';

// ==================== 示例 1: 基本集成 ====================

/**
 * 在应用启动时初始化补全系统
 */
export async function initializeCompletionSystem() {
  console.log('初始化命名空间 ID 补全系统...');

  // 从 Schema 自动提取补全规则
  await initCompletionFromSchemas(schemaRegistry, globalCompletionRuleManager);

  console.log('✅ 补全系统初始化完成');
}

/**
 * 注册 Monaco Editor 补全
 */
export function registerMonacoCompletion(monacoInstance: typeof monaco) {
  const disposable = registerNamespaceIdCompletion(
    monacoInstance,
    globalNamespaceIdProvider,
    globalCompletionRuleManager
  );

  console.log('✅ Monaco Editor 补全已注册');

  return disposable;
}

// ==================== 示例 2: Vue 3 组件集成 ====================

/*
<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as monaco from 'monaco-editor';
import { schemaRegistry } from '@/utils/schemaRegistry';
import { globalCompletionRuleManager } from '@/utils/completionRules';
import {
  globalNamespaceIdProvider,
  registerNamespaceIdCompletion,
  initCompletionFromSchemas
} from '@/utils/namespaceIdCompletion';
import { useFileSystemStore } from '@/stores/fileSystem';

const editorContainer = ref<HTMLElement>();
const fileSystemStore = useFileSystemStore();

let editor: monaco.editor.IStandaloneCodeEditor | null = null;
let completionDisposable: monaco.IDisposable | null = null;

onMounted(async () => {
  if (!editorContainer.value) return;

  // 1. 从 Schema 初始化补全规则
  await initCompletionFromSchemas(schemaRegistry, globalCompletionRuleManager);

  // 2. 创建编辑器
  editor = monaco.editor.create(editorContainer.value, {
    value: '{\n  "model": "ywzj_vehicle:"\n}',
    language: 'json',
    theme: 'vs-dark'
  });

  // 3. 注册补全
  completionDisposable = registerNamespaceIdCompletion(
    monaco,
    globalNamespaceIdProvider,
    globalCompletionRuleManager
  );

  // 4. 更新文件树
  globalNamespaceIdProvider.updateFileTree(fileSystemStore.fileTree);
});

// 监听文件树变化
watch(() => fileSystemStore.fileTree, (newTree) => {
  globalNamespaceIdProvider.updateFileTree(newTree);
});

onUnmounted(() => {
  completionDisposable?.dispose();
  editor?.dispose();
});
</script>

<template>
  <div ref="editorContainer" class="editor-container"></div>
</template>

<style scoped>
.editor-container {
  width: 100%;
  height: 600px;
}
</style>
*/

// ==================== 示例 3: 查看提取的规则 ====================

/**
 * 调试：查看从 Schema 提取的补全规则
 */
export async function debugSchemaCompletionRules() {
  const { extractCompletionRulesFromSchemaRegistry, deduplicateCompletionRules } =
    await import('@/utils/schemaCompletionExtractor');

  // 提取规则
  const rules = extractCompletionRulesFromSchemaRegistry(schemaRegistry);
  console.log('从 Schema 提取的规则 (去重前):', rules);

  // 去重
  const uniqueRules = deduplicateCompletionRules(rules);
  console.log('去重后的规则:', uniqueRules);

  // 按 Schema 分组显示
  const rulesBySchema = new Map<string, any[]>();
  schemaRegistry.forEach(info => {
    const schemaRules = extractCompletionRulesFromSchemaRegistry([info]);
    if (schemaRules.length > 0) {
      rulesBySchema.set(info.title, schemaRules);
    }
  });

  console.log('按 Schema 分组的规则:');
  rulesBySchema.forEach((rules, schemaTitle) => {
    console.log(`\n📋 ${schemaTitle}:`);
    rules.forEach(rule => {
      console.log(`  - ${typeof rule.fieldPattern === 'string' ? rule.fieldPattern : rule.fieldPattern.source}: ${rule.packType}/${rule.category}`);
    });
  });
}

// ==================== 示例 4: 测试补全功能 ====================

/**
 * 测试：模拟编辑器中的补全
 */
export async function testCompletion() {
  await initCompletionFromSchemas(schemaRegistry, globalCompletionRuleManager);

  // 模拟不同字段名的补全
  const testCases = [
    'model',
    'texture',
    'engine_start',
    'engine_idle',
    'animations',
    'script'
  ];

  console.log('\n🧪 测试补全规则匹配:\n');

  testCases.forEach(fieldName => {
    const rule = globalCompletionRuleManager.findMatchingRule(fieldName);
    if (rule) {
      console.log(`✅ ${fieldName}: ${rule.packType}/${rule.category}`);
    } else {
      console.log(`❌ ${fieldName}: 未找到匹配规则`);
    }
  });
}

// ==================== 示例 5: 手动添加额外规则 ====================

/**
 * 在自动提取的基础上，手动添加额外的规则
 */
export async function addCustomRules() {
  const { registerCompletionRule } = await import('@/utils/completionRules');

  // 首先从 Schema 加载
  await initCompletionFromSchemas(schemaRegistry, globalCompletionRuleManager);

  // 然后添加自定义规则
  registerCompletionRule({
    fieldPattern: 'custom_model',
    packType: 'assets',
    category: 'models/custom',
    description: '自定义模型',
    priority: 10
  });

  registerCompletionRule({
    fieldPattern: /^particle_/,  // 正则匹配
    packType: 'assets',
    category: 'particles',
    description: '粒子效果（前缀匹配）',
    priority: 8
  });

  console.log('✅ 已添加自定义补全规则');
}

// ==================== 示例 6: 完整的应用启动流程 ====================

/**
 * 应用启动时的完整初始化流程
 */
export async function initializeApplication() {
  console.log('🚀 应用启动中...\n');

  // 步骤 1: 初始化补全系统
  console.log('📝 步骤 1: 从 Schema 提取补全规则');
  await initCompletionFromSchemas(schemaRegistry, globalCompletionRuleManager);

  // 步骤 2: 可选 - 添加额外的自定义规则
  console.log('📝 步骤 2: 添加自定义规则（可选）');
  // await addCustomRules();

  // 步骤 3: 在编辑器准备好后注册补全
  console.log('📝 步骤 3: 等待 Monaco Editor 初始化');
  // 这部分通常在组件的 onMounted 中完成

  console.log('\n✅ 应用初始化完成！');
  console.log('📚 补全功能已准备就绪\n');
}

// ==================== 使用说明 ====================

/*
使用方式：

1. 在 main.ts 中调用：
   ```typescript
   import { initializeApplication } from '@/examples/schemaCompletionExample';

   initializeApplication().then(() => {
     console.log('应用已启动');
   });
   ```

2. 在组件中注册 Monaco 补全：
   ```typescript
   import { registerMonacoCompletion } from '@/examples/schemaCompletionExample';

   onMounted(() => {
     const disposable = registerMonacoCompletion(monaco);
     // 组件卸载时清理
     onUnmounted(() => disposable?.dispose());
   });
   ```

3. 调试时查看规则：
   ```typescript
   import { debugSchemaCompletionRules } from '@/examples/schemaCompletionExample';

   debugSchemaCompletionRules();
   ```

4. 测试补全匹配：
   ```typescript
   import { testCompletion } from '@/examples/schemaCompletionExample';

   testCompletion();
   ```
*/
