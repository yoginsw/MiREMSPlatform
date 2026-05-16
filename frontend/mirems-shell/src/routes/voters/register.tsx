import { createFileRoute } from '@tanstack/react-router';
import { VoterRegistrationPage } from '../../features/voters/voter-pages';

export const Route = createFileRoute('/voters/register')({
  component: VoterRegistrationPage,
});
