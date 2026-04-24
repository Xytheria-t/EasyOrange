import { useUIStore } from '@/store';

export function GlobalLoading() {
  const { isLoading, loadingMessage } = useUIStore();

  if (!isLoading) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="rounded-lg bg-white p-6 text-center">
        <div className="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-4 border-primary-500 border-t-transparent" />
        <p className="text-gray-600">{loadingMessage}</p>
      </div>
    </div>
  );
}
