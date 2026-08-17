import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { App } from "../App";

describe("App", () => {
  it("boots to the Home tab", () => {
    render(<App />);
    expect(screen.getByRole("heading", { name: /Home/i })).toBeInTheDocument();
  });
});
