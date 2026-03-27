export function unwrapList<T>(payload: unknown): T[] {
  if (Array.isArray(payload)) {
    return payload as T[];
  }

  if (payload && typeof payload === 'object' && Array.isArray((payload as { content?: unknown }).content)) {
    return (payload as { content: T[] }).content;
  }

  return [];
}