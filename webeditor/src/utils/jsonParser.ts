/**
 * JSON 解析工具 - 支持带注释的 JSON
 */

/**
 * 移除 JSON 字符串中的注释
 * 支持单行注释和多行注释
 */
export function stripJsonComments(jsonString: string): string {
  let result = '';
  let i = 0;
  let inString = false;
  let stringChar = '';

  while (i < jsonString.length) {
    const char = jsonString[i];
    const nextChar = jsonString[i + 1];

    // 处理字符串
    if ((char === '"' || char === "'") && (i === 0 || jsonString[i - 1] !== '\\')) {
      if (!inString) {
        inString = true;
        stringChar = char;
      } else if (char === stringChar) {
        inString = false;
        stringChar = '';
      }
      result += char;
      i++;
      continue;
    }

    // 在字符串内部，直接添加字符
    if (inString) {
      result += char;
      i++;
      continue;
    }

    // 处理单行注释 //
    if (char === '/' && nextChar === '/') {
      // 跳过直到行尾
      i += 2;
      while (i < jsonString.length && jsonString[i] !== '\n' && jsonString[i] !== '\r') {
        i++;
      }
      continue;
    }

    // 处理多行注释 /* ... */
    if (char === '/' && nextChar === '*') {
      // 跳过直到 */
      i += 2;
      while (i < jsonString.length - 1) {
        if (jsonString[i] === '*' && jsonString[i + 1] === '/') {
          i += 2;
          break;
        }
        i++;
      }
      continue;
    }

    // 普通字符
    result += char;
    i++;
  }

  return result;
}

/**
 * 解析带注释的 JSON 字符串
 * @param jsonString - 可能包含注释的 JSON 字符串
 * @returns 解析后的对象
 */
export function parseJsonWithComments(jsonString: string): any {
  const stripped = stripJsonComments(jsonString);
  return JSON.parse(stripped);
}

/**
 * 安全解析 JSON，返回结果和错误信息
 * @param jsonString - 可能包含注释的 JSON 字符串
 * @returns { success: boolean, data?: any, error?: string }
 */
export function safeParseJson(jsonString: string): { success: boolean; data?: any; error?: string } {
  try {
    const data = parseJsonWithComments(jsonString);
    return { success: true, data };
  } catch (err: any) {
    return { success: false, error: err.message };
  }
}
