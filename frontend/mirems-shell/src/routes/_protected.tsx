import { createFileRoute } from '@tanstack/react-router';
import { ProtectedShellLayout } from '../ShellLayout';

export const Route = createFileRoute('/_protected')({
  component: ProtectedShellLayout,
});
