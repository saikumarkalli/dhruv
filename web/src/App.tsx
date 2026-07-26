import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "react-router-dom";
import { IntlProvider } from "react-intl";
import { router } from "./router";
import en from "./shared/i18n/en.json";

const queryClient = new QueryClient();

export function App() {
  return (
    <IntlProvider locale="en" messages={en}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </IntlProvider>
  );
}
