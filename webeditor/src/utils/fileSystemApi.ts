import type {FileNode} from '@/types/fileSystem';

export class FileSystemManager {
  private rootHandle: FileSystemDirectoryHandle | null = null;

  async openFolder(): Promise<FileSystemDirectoryHandle> {
    if (!('showDirectoryPicker' in window)) {
      throw new Error('File System Access API not supported');
    }

    this.rootHandle = await window.showDirectoryPicker({
      mode: 'readwrite'
    });
    return this.rootHandle;
  }

  async buildFileTree(
    dirHandle: FileSystemDirectoryHandle,
    path = ''
  ): Promise<FileNode[]> {
    const tree: FileNode[] = [];

    for await (const [name, handle] of dirHandle.entries()) {
      const fullPath = path ? `${path}/${name}` : name;

      if (handle.kind === 'directory') {
        tree.push({
          name,
          path: fullPath,
          type: 'folder',
          handle,
          children: await this.buildFileTree(handle as FileSystemDirectoryHandle, fullPath)
        });
      } else {
        tree.push({
          name,
          path: fullPath,
          type: 'file',
          handle,
          extension: name.split('.').pop()
        });
      }
    }

    return tree.sort((a, b) => {
      if (a.type !== b.type) return a.type === 'folder' ? -1 : 1;
      return a.name.localeCompare(b.name);
    });
  }

  async readFile(fileHandle: FileSystemFileHandle): Promise<string> {
    const file = await fileHandle.getFile();
    return await file.text();
  }

  async readFileAsArrayBuffer(fileHandle: FileSystemFileHandle): Promise<ArrayBuffer> {
    const file = await fileHandle.getFile();
    return await file.arrayBuffer();
  }

  async readFileAsDataURL(fileHandle: FileSystemFileHandle): Promise<string> {
    const file = await fileHandle.getFile();
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  async getFileSize(fileHandle: FileSystemFileHandle): Promise<number> {
    const file = await fileHandle.getFile();
    return file.size;
  }

  async writeFile(
    fileHandle: FileSystemFileHandle,
    content: string
  ): Promise<void> {
    const writable = await fileHandle.createWritable();
    await writable.write(content);
    await writable.close();
  }

  async createFile(
    dirHandle: FileSystemDirectoryHandle,
    fileName: string,
    content = ''
  ): Promise<FileSystemFileHandle> {
    const fileHandle = await dirHandle.getFileHandle(fileName, {
      create: true
    });
    if (content) {
      await this.writeFile(fileHandle, content);
    }
    return fileHandle;
  }

  async createFolder(
    dirHandle: FileSystemDirectoryHandle,
    folderName: string
  ): Promise<FileSystemDirectoryHandle> {
    return await dirHandle.getDirectoryHandle(folderName, {
      create: true
    });
  }

  async deleteFile(
    dirHandle: FileSystemDirectoryHandle,
    fileName: string
  ): Promise<void> {
    await dirHandle.removeEntry(fileName);
  }

  async deleteFolder(
    dirHandle: FileSystemDirectoryHandle,
    folderName: string
  ): Promise<void> {
    await dirHandle.removeEntry(folderName, { recursive: true });
  }

  async uploadFile(
    dirHandle: FileSystemDirectoryHandle,
    file: File
  ): Promise<FileSystemFileHandle> {
    const fileHandle = await dirHandle.getFileHandle(file.name, {
      create: true
    });
    const writable = await fileHandle.createWritable();
    await writable.write(file);
    await writable.close();
    return fileHandle;
  }

  getRootHandle(): FileSystemDirectoryHandle | null {
    return this.rootHandle;
  }
}

export const fileSystemManager = new FileSystemManager();
