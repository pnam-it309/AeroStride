import { db } from "@/services/mock.db";
import { createMockCrudService } from "@/services/entity.factory";

const base = createMockCrudService({ db, key: "payments" });

function decoratePayment(item) {
  const key = String(item.status ?? "")
    .toUpperCase()
    .trim();

  if (key === "SUCCESS") {
    return {
      ...item,
      statusText: "Thành công",
      statusTone: "green",
    };
  }

  if (key === "UNPAID") {
    return {
      ...item,
      statusText: "Chưa thanh toán",
      statusTone: "orange",
    };
  }

  return item;
}

export const paymentService = {
  ...base,
  async list() {
    const rows = await base.list();
    return rows.map(decoratePayment);
  },
  async get(id) {
    const row = await base.get(id);
    return decoratePayment(row);
  },
};
