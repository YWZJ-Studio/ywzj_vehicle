<template>
  <div class="monaco-editor-container" ref="editorContainer"></div>
</template>

<script setup lang="ts">
import {onActivated, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import * as monaco from 'monaco-editor';
import {findSchemaByPath, schemaRegistry} from '@/utils/schemaRegistry';
import {globalCompletionRuleManager} from '@/utils/completionRules';
import {
  globalNamespaceIdProvider,
  initCompletionFromSchemas,
  registerNamespaceIdCompletion
} from '@/utils/namespaceIdCompletion';
import {useFileSystemStore} from '@/stores/fileSystem';

interface Props {
  content: string;
  path: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  update: [content: string];
}>();

const editorContainer = ref<HTMLElement>();
let editor: monaco.editor.IStandaloneCodeEditor | null = null;
let completionDisposable: monaco.IDisposable | null = null;

const fileSystemStore = useFileSystemStore();

const getLanguage = (path: string): string => {
  const ext = path.split('.').pop()?.toLowerCase();
  switch (ext) {
    case 'json':
      return 'json';
    case 'js':
      return 'javascript';
    case 'ts':
      return 'typescript';
    case 'md':
      return 'markdown';
    default:
      return 'plaintext';
  }
};

// 检查文件是否应该应用 Schema
const shouldApplySchema = (path: string): boolean => {
  return findSchemaByPath(path) !== null;
};

// 配置 JSON Schema
function configureJsonSchema(enableValidation = true) {
  // 每次都重新配置以确保 Schema 生效
  monaco.languages.json.jsonDefaults.setDiagnosticsOptions({
    validate: enableValidation, // 允许禁用验证
    allowComments: true,
    schemaValidation: 'error', // Enable schema validation as errors
    enableSchemaRequest: true,
    schemas: enableValidation ? schemaRegistry.map(info => ({
      uri: info.uri,
      fileMatch: info.fileMatch,
      schema: info.schema as any
    })) : [] // 大文件时不加载schema
  });
}

onMounted(async () => {
  if (!editorContainer.value) return;

  // Initialize completion system from schemas
  await initCompletionFromSchemas(schemaRegistry, globalCompletionRuleManager);

  // Update file tree for namespace ID completions
  globalNamespaceIdProvider.updateFileTree(fileSystemStore.fileTree);

  const fileSize = props.content.length;

  if (fileSize > 5 * 1024 * 1024) { // 5MB
    console.error('[MonacoEditor] ❌ File too large:', (fileSize / 1024 / 1024).toFixed(2) + 'MB');
    // 显示错误消息而不是创建编辑器
    if (editorContainer.value) {
      editorContainer.value.innerHTML = `
        <div style="padding: 20px; color: #f44336; background: #2d2d2d; height: 100%; display: flex; align-items: center; justify-content: center; flex-direction: column;">
          <h3>⚠️ 文件过大无法在浏览器中编辑</h3>
          <p>文件大小: ${(fileSize / 1024).toFixed(0)}KB (${(fileSize / 1024 / 1024).toFixed(2)}MB)</p>
          <p>建议使用 VS Code 等桌面编辑器打开此文件</p>
        </div>
      `;
    }
    return;
  }

  // 创建带有正确 URI 的 model
  let language = getLanguage(props.path);

  // 确保 meta.json 文件使用 JSON 语言
  if (shouldApplySchema(props.path)) {
    language = 'json';
  }

  const normalizedPath = props.path.replace(/\\/g, '/');
  const uri = monaco.Uri.file(normalizedPath);

  const isLargeFile = fileSize > 500 * 1024;
  const isHugeFile = fileSize > 2 * 1024 * 1024;

  console.log('[MonacoEditor] File info:', {
    path: props.path,
    sizeKB: (fileSize / 1024).toFixed(1) + 'KB',
    isLargeFile,
    isHugeFile
  });

  // 🚀 核心优化：大JSON文件使用plaintext模式
  const originalLanguage = language;
  if (language === 'json' && isLargeFile) {
    console.warn('[MonacoEditor] ⚡ Large JSON detected - Using plaintext mode to prevent freezing');
    language = 'plaintext';
  }

  // 只有小的meta.json文件才启用Schema验证
  const enableValidation = !isLargeFile && shouldApplySchema(props.path) && language === 'json';
  configureJsonSchema(enableValidation);

  if (language === 'plaintext' && originalLanguage === 'json') {
    console.warn('[MonacoEditor] ⚡ JSON syntax highlighting disabled for performance');
  }

  console.log('[MonacoEditor] Creating model:', {
    path: props.path,
    normalizedPath,
    uri: uri.toString(),
    language,
    shouldApplySchema: shouldApplySchema(props.path)
  });

  const model = monaco.editor.createModel(props.content, language, uri);

  editor = monaco.editor.create(editorContainer.value, {
    model: model,
    theme: 'vs-dark',
    automaticLayout: true,
    fontSize: 14,
    lineNumbers: 'on',
    minimap: {
      enabled: !isLargeFile, // 大文件禁用minimap
    },
    scrollBeyondLastLine: false,
    wordWrap: 'on',
    formatOnPaste: !isLargeFile,
    formatOnType: !isLargeFile,
    suggest: {
      showWords: !isLargeFile,
    },
    quickSuggestions: !isLargeFile,
    codeLens: false,
    folding: !isHugeFile,
    foldingStrategy: isLargeFile ? 'indentation' : 'auto',
    occurrencesHighlight: isLargeFile ? 'off' : 'singleFile',
    renderWhitespace: 'none',
    renderControlCharacters: false,
    renderLineHighlight: isLargeFile ? 'none' : 'all',
    matchBrackets: isLargeFile ? 'never' : 'always',
    links: !isLargeFile,
    colorDecorators: false,
    contextmenu: true,
    smoothScrolling: false,
    cursorBlinking: isHugeFile ? 'solid' : 'blink',
    cursorSmoothCaretAnimation: 'off',
    renderValidationDecorations: 'off',
    rulers: [],
    glyphMargin: !isHugeFile,
    acceptSuggestionOnCommitCharacter: !isLargeFile,
    acceptSuggestionOnEnter: isLargeFile ? 'off' : 'on',
    tabCompletion: isLargeFile ? 'off' : 'on',
    wordBasedSuggestions: isLargeFile ? 'off' : 'currentDocument',
    parameterHints: {
      enabled: !isLargeFile
    },
    hover: {
      enabled: !isLargeFile
    },
    fixedOverflowWidgets: true // 允许悬浮提示溢出编辑器容器
  });

  // Register completion provider for JSON files (if not a large file)
  if (!isLargeFile && language === 'json') {
    completionDisposable = registerNamespaceIdCompletion(
      monaco,
      globalNamespaceIdProvider,
      globalCompletionRuleManager
    );
    console.log('[MonacoEditor] ✅ Completion provider registered');
  }

  // 🚀 使用防抖来减少更新频率
  let updateTimeout: ReturnType<typeof setTimeout> | null = null;
  const debounceDelay = isLargeFile ? 500 : 100; // 大文件延迟500ms

  editor.onDidChangeModelContent(() => {
    if (updateTimeout) {
      clearTimeout(updateTimeout);
    }
    updateTimeout = setTimeout(() => {
      const value = editor?.getValue() || '';
      emit('update', value);
    }, debounceDelay);
  });

  // 延迟检查 markers 以验证 Schema 是否生效（仅小文件）
  if (!isLargeFile && enableValidation) {
    setTimeout(() => {
      const markers = monaco.editor.getModelMarkers({ resource: uri });
      console.log('[MonacoEditor] Validation markers:', markers.length);
    }, 500);
  }
});

watch(() => props.content, (newContent) => {
  if (editor && editor.getValue() !== newContent) {
    const model = editor.getModel();
    if (model) {
      model.setValue(newContent);
    }
  }
});

watch(() => props.path, (newPath, oldPath) => {
  if (!editor || newPath === oldPath) return;

  console.log('[MonacoEditor] Path changed:', {
    from: oldPath,
    to: newPath,
    shouldApplySchema: shouldApplySchema(newPath)
  });

  const model = editor.getModel();
  if (model) {
    // 更新 model URI 以匹配新路径
    let language = getLanguage(newPath);

    // 确保 meta.json 文件使用 JSON 语言
    if (shouldApplySchema(newPath)) {
      language = 'json';
    }

    const fileSize = model.getValue().length;
    const isLargeFile = fileSize > 500 * 1024;

    if (language === 'json' && isLargeFile) {
      console.warn('[MonacoEditor] ⚡ Large JSON - Using plaintext mode');
      language = 'plaintext';
    }

    const enableValidation = !isLargeFile && shouldApplySchema(newPath) && language === 'json';
    configureJsonSchema(enableValidation);

    // 规范化路径，使用正斜杠
    const normalizedPath = newPath.replace(/\\/g, '/');
    const uri = monaco.Uri.file(normalizedPath);
    const newModel = monaco.editor.createModel(
      model.getValue(),
      language,
      uri
    );

    editor.setModel(newModel);
    model.dispose();

    // 延迟检查 markers（仅小文件）
    if (!isLargeFile && enableValidation) {
      setTimeout(() => {
        const markers = monaco.editor.getModelMarkers({ resource: uri });
        console.log('[MonacoEditor] Validation markers after path change:', markers.length);
      }, 500);
    }
  }
});

// Watch for file tree changes to update completion suggestions
watch(() => fileSystemStore.fileTree, (newTree) => {
  globalNamespaceIdProvider.updateFileTree(newTree);
  console.log('[MonacoEditor] File tree updated for completions');
}, { deep: true });

onActivated(() => {
  // 重新布局编辑器以适应可能的容器大小变化
  if (editor) {
    editor.layout();
  }
});

onBeforeUnmount(() => {
  const model = editor?.getModel();
  model?.dispose();
  editor?.dispose();
  completionDisposable?.dispose();
});
</script>

<style scoped>
.monaco-editor-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
