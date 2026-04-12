import { db } from "@/services/mock.db";

function toProductListItem(item) {
  return {
    ...item,
    // Keep current mock structure while preparing fields for BE join response.
    brandName: item.brandName ?? item.brand ?? "",
    categoryName: item.categoryName ?? item.category ?? "",
    qty: item.qty ?? item.stock ?? 0,
  };
}

export const productService = {
  async list() {
    return structuredClone(db.products).map(toProductListItem);
  },
  async get(id) {
    const x = db.products.find((p) => String(p.id) === String(id));
    if (!x) throw new Error("Product not found");
    return toProductListItem(structuredClone(x));
  },
  async create(payload) {
    const nextId = Math.max(...db.products.map((x) => x.id)) + 1;
    const item = {
      id: nextId,
      createdAt: new Date().toISOString().slice(0, 10),
      ...payload,
    };
    db.products.unshift(item);
    return structuredClone(item);
  },
  async update(id, payload) {
    const idx = db.products.findIndex((p) => String(p.id) === String(id));
    if (idx < 0) throw new Error("Product not found");
    db.products[idx] = { ...db.products[idx], ...payload };
    return structuredClone(db.products[idx]);
  },
  async remove(id) {
    const idx = db.products.findIndex((p) => String(p.id) === String(id));
    if (idx < 0) return true;
    db.products.splice(idx, 1);
    return true;
  },
};
