export const isMockEnabled = import.meta.env.VITE_ENABLE_MOCK === 'true';

export function mockResolve<T>(data: T, delay = 220): Promise<T> {
  return new Promise((resolve) => {
    window.setTimeout(() => resolve(data), delay);
  });
}
