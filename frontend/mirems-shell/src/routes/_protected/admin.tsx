import { createFileRoute } from '@tanstack/react-router';
import { AdminDashboardPage } from '../../features/admin/admin-dashboard-page';

export const Route = createFileRoute('/_protected/admin')({
  component: AdminRoutePage,
});

export function AdminRoutePage() {
  return <AdminDashboardPage />;
}
