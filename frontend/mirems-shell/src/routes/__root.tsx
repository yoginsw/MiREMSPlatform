import { createRootRoute, Outlet, useRouterState } from '@tanstack/react-router';
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools';
import { AppErrorBoundary, NotFoundPage, RouteErrorPage } from '../errors/ErrorPages';
const routerDevtoolsEnabled = import.meta.env.DEV;

export const Route = createRootRoute({
  component: RootRouteComponent,
  errorComponent: RouteErrorPage,
  notFoundComponent: NotFoundPage,
});

function RootRouteComponent() {
  const currentPath = useRouterState({ select: (state) => state.location.href });
  return (
    <AppErrorBoundary path={currentPath}>
      <Outlet />
      {routerDevtoolsEnabled ? <TanStackRouterDevtools /> : null}
    </AppErrorBoundary>
  );
}
