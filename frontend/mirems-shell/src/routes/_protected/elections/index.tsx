import { createFileRoute } from '@tanstack/react-router';
import { ElectionListPage } from '../../../features/elections/election-pages';

export const Route = createFileRoute('/_protected/elections/')({
  component: ElectionListPage,
});
