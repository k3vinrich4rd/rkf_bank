// @vitest-environment jsdom
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { ContaResponse } from "@/lib/api";
import { SeletorConta } from "./seletor-conta";

afterEach(cleanup);

function conta(id: string, tipoConta: ContaResponse["tipoConta"], numeroConta: string): ContaResponse {
  return { id, clienteId: "cliente", agencia: "0001", numeroConta, tipoConta, saldo: 0, ativa: true };
}

const CORRENTE = conta("c1", "CORRENTE", "11111111");
const POUPANCA = conta("c2", "POUPANCA", "22222222");

describe("SeletorConta", () => {
  it("com uma conta só não mostra nada", () => {
    const { container } = render(<SeletorConta contas={[CORRENTE]} contaId="c1" onEscolher={vi.fn()} />);

    expect(container.innerHTML).toBe("");
  });

  it("marca a conta escolhida e avisa quando a pessoa troca", async () => {
    const onEscolher = vi.fn();
    render(<SeletorConta contas={[CORRENTE, POUPANCA]} contaId="c1" onEscolher={onEscolher} />);

    expect(screen.getByRole("radio", { name: /Conta corrente/ }).getAttribute("aria-checked")).toBe("true");
    expect(screen.getByRole("radio", { name: /Poupança/ }).getAttribute("aria-checked")).toBe("false");

    await userEvent.click(screen.getByRole("radio", { name: /Poupança/ }));

    expect(onEscolher).toHaveBeenCalledWith("c2");
  });
});
