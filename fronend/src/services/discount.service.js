import { db } from "@/services/mock.db";

function toEpoch(value) {
  if (value == null || value === "") return null;
  const num = Number(value);
  if (!Number.isNaN(num) && Number.isFinite(num)) return num;

  const date = new Date(String(value));
  const time = date.getTime();
  return Number.isNaN(time) ? null : time;
}

function normalizeType(value) {
  const key = String(value || "")
    .toUpperCase()
    .trim();
  if (key === "PERCENT") return "PERCENT";
  if (key === "AMOUNT" || key === "MONEY") return "AMOUNT";
  return key || "";
}

function normalizeDiscount(item) {
  const loaiGiamGia = normalizeType(item.loaiGiamGia ?? item.type);
  const phanTram = item.phanTramGiamGia ?? item.percent ?? null;
  const soTien = item.soTienGiam ?? item.value ?? null;

  return {
    id: String(item.id ?? ""),
    ma: item.ma ?? item.code ?? "",
    ten: item.ten ?? item.name ?? "",
    loaiGiamGia,
    soTienGiam: soTien == null || soTien === "" ? null : Number(soTien),
    dieuKienGiamGia:
      item.dieuKienGiamGia ?? item.minOrderAmount ?? item.orderMin ?? null,
    ngayBatDau: toEpoch(item.ngayBatDau ?? item.start),
    ngayKetThuc: toEpoch(item.ngayKetThuc ?? item.end),
    mucUuTien: Number(item.mucUuTien ?? item.priority ?? 0),
    phanTramGiamGia:
      phanTram == null || phanTram === "" ? null : Number(phanTram),
  };
}

export const discountService = {
  async list() {
    return structuredClone(db.discounts).map(normalizeDiscount);
  },
  async get(id) {
    const x = db.discounts.find((d) => String(d.id) === String(id));
    if (!x) throw new Error("Discount not found");
    return normalizeDiscount(structuredClone(x));
  },
  async create(payload) {
    const nextId =
      Math.max(...db.discounts.map((x) => Number(x.id) || 0), 0) + 1;
    const item = { id: String(nextId), ...payload };
    db.discounts.unshift(item);
    return normalizeDiscount(structuredClone(item));
  },
  async update(id, payload) {
    const idx = db.discounts.findIndex((d) => String(d.id) === String(id));
    if (idx < 0) throw new Error("Discount not found");
    db.discounts[idx] = { ...db.discounts[idx], ...payload };
    return normalizeDiscount(structuredClone(db.discounts[idx]));
  },
  async remove(id) {
    const idx = db.discounts.findIndex((d) => String(d.id) === String(id));
    if (idx < 0) return true;
    db.discounts.splice(idx, 1);
    return true;
  },
};
