import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { App } from "../App";

describe("App", () => {
  it("boots to the Finance placeholder route", () => {
    render(<App />);
    expect(screen.getByText(/Finance/i)).toBeInTheDocument();
  });
});
