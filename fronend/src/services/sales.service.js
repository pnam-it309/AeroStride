import { db } from "@/services/mock.db";

export const salesService = {
  async list() {
    return structuredClone(db.sales);
  },
  async get(id) {
    const x = db.sales.find((s) => String(s.id) === String(id));
    if (!x) throw new Error("Campaign not found");
    return structuredClone(x);
  },
  async create(payload) {
    const nextId = Math.max(...db.sales.map((x) => Number(x.id) || 0), 0) + 1;
    const item = {
      id: nextId,
      createdAt: new Date().toISOString().slice(0, 10),
      ...payload,
    };
    db.sales.unshift(item);
    return structuredClone(item);
  },
  async update(id, payload) {
    const idx = db.sales.findIndex((s) => String(s.id) === String(id));
    if (idx < 0) throw new Error("Campaign not found");
    db.sales[idx] = { ...db.sales[idx], ...payload };
    return structuredClone(db.sales[idx]);
  },
  async remove(id) {
    const idx = db.sales.findIndex((s) => String(s.id) === String(id));
    if (idx < 0) return true;
    db.sales.splice(idx, 1);
    return true;
  },
};
