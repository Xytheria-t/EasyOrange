import { useCurrentUser } from '@/hooks';

export function ProfilePage() {
  const { data: user } = useCurrentUser();

  return (
    <div className="rounded-xl bg-white p-6 shadow-sm">
      <h1 className="text-2xl font-bold">个人中心</h1>
      <div className="mt-6 space-y-4">
        <div>
          <label className="text-sm text-gray-500">用户名</label>
          <p className="text-lg">{user?.username}</p>
        </div>
        <div>
          <label className="text-sm text-gray-500">邮箱</label>
          <p className="text-lg">{user?.email}</p>
        </div>
      </div>
    </div>
  );
}
