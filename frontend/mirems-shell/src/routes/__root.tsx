import { createRootRoute, Outlet } from '@tanstack/react-router';
import { TanStackRouterDevtools } from '@tanstack/react-router-devtools';
const routerDevtoolsEnabled = import.meta.env.DEV;

export const Route = createRootRoute({
  component: RootRouteComponent,
});

function RootRouteComponent() {
  return (
    <>
      <Outlet />
      {routerDevtoolsEnabled ? <TanStackRouterDevtools /> : null}
    </>
  );
}
