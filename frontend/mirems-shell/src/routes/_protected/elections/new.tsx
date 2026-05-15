import { createFileRoute } from '@tanstack/react-router';
import { ElectionCreationWizard } from '../../../features/elections/election-create';

export const Route = createFileRoute('/_protected/elections/new')({
  component: ElectionCreationWizard,
});
