export interface FileNode {
  name: string;
  path: string;
  type: 'file' | 'folder';
  handle: FileSystemHandle;
  children?: FileNode[];
  extension?: string;
}

export interface OpenFile {
  handle: FileSystemFileHandle;
  content: string;
  modified: boolean;
  savedContent: string;
  previewContext?: ModelPreviewContext;
}

export interface ModelPreviewContext {
  autoTexture?: string;
  autoTextureName?: string;
  autoStructureModel?: string;
  autoStructureModelName?: string;
}
