export function getFileIcon(fileName: string, isFolder: boolean): string {
  if (isFolder) {
    return 'folder';
  }

  const ext = fileName.split('.').pop()?.toLowerCase();

  switch (ext) {
    case 'json':
      return 'document';
    case 'png':
    case 'jpg':
    case 'jpeg':
    case 'gif':
      return 'picture';
    case 'ogg':
    case 'mp3':
    case 'wav':
      return 'headset';
    case 'obj':
    case 'fbx':
    case 'gltf':
      return 'box';
    default:
      return 'document';
  }
}

export function getFileType(fileName: string): 'json' | 'image' | 'audio' | 'model' | 'text' | 'unknown' {
  const ext = fileName.split('.').pop()?.toLowerCase();

  switch (ext) {
    case 'json':
      return 'json';
    case 'png':
    case 'jpg':
    case 'jpeg':
    case 'gif':
    case 'bmp':
      return 'image';
    case 'ogg':
    case 'mp3':
    case 'wav':
      return 'audio';
    case 'obj':
    case 'fbx':
    case 'gltf':
    case 'glb':
      return 'model';
    case 'txt':
    case 'md':
      return 'text';
    default:
      return 'unknown';
  }
}

export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}

export function isValidFileName(name: string): boolean {
  const invalidChars = /[<>:"/\\|?*\x00-\x1f]/;
  return !invalidChars.test(name) && name.length > 0 && name.length <= 255;
}
