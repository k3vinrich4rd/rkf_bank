// @vitest-environment jsdom
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import { ForcaSenha } from "./forca-senha";

afterEach(cleanup);

describe("ForcaSenha", () => {
  it("sem senha não mostra nada", () => {
    const { container } = render(<ForcaSenha senha="" />);

    expect(container.innerHTML).toBe("");
  });

  it.each([
    ["abc", "Muito curta"],
    ["abcdefgh", "Fraca"],
    ["abcdefg1", "Razoável"],
    ["abcdefghijk1", "Boa"],
    ["Abcdefghijk1!", "Forte"],
  ])("%s → %s", (senha, rotulo) => {
    render(<ForcaSenha senha={senha} />);

    expect(screen.getByText(rotulo)).toBeTruthy();
  });
});
