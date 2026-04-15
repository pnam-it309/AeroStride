import { db } from "@/services/mock.db";

function toEpoch(value) {
  if (value == null || value === "") return null;
  const num = Number(value);
  if (!Number.isNaN(num) && Number.isFinite(num)) return num;

  const date = new Date(String(value));
  const time = date.getTime();
  return Number.isNaN(time) ? null : time;
}

function computeStatus(item) {
  const explicit = String(item.status || "")
    .toUpperCase()
    .trim();
  if (explicit) return explicit;

  const now = Date.now();
  const start = toEpoch(item.ngayBatDau);
  const end = toEpoch(item.ngayKetThuc);
  const qty = Number(item.soLuong ?? 0);

  if (start != null && now < start) return "INACTIVE";
  if (end != null && now > end) return "EXPIRED";
  if (qty <= 0) return "OUT_OF_STOCK";
  return "ACTIVE";
}

function normalizeVoucher(item) {
  const normalized = {
    id: String(item.id ?? ""),
    ma: item.ma ?? item.code ?? "",
    ten: item.ten ?? item.name ?? "",
    loaiPhieu: item.loaiPhieu ?? item.type ?? "",
    phanTramGiamGia:
      item.phanTramGiamGia ??
      (String(item.type || "").toUpperCase() === "PERCENT" ? item.value : null),
    soTienGiam:
      item.soTienGiam ??
      (String(item.type || "").toUpperCase() !== "PERCENT" ? item.value : null),
    soLuong: item.soLuong ?? item.qty ?? 0,
    donHangToiThieu: item.donHangToiThieu ?? 0,
    giamToiDa: item.giamToiDa ?? 0,
    ngayBatDau: toEpoch(item.ngayBatDau ?? item.start),
    ngayKetThuc: toEpoch(item.ngayKetThuc ?? item.end),
    ghiChu: item.ghiChu ?? item.note ?? "",
    status: computeStatus(item),
  };

  return normalized;
}

// Khi join backend: thay bằng api.get/post/put/delete
export const voucherService = {
  async list() {
    return structuredClone(db.vouchers).map(normalizeVoucher);
  },
  async get(id) {
    const x = db.vouchers.find((v) => String(v.id) === String(id));
    if (!x) throw new Error("Voucher not found");
    return normalizeVoucher(structuredClone(x));
  },
  async create(payload) {
    const nextId = Math.max(...db.vouchers.map((x) => x.id)) + 1;
    const item = { id: nextId, ...payload };
    db.vouchers.unshift(item);
    return normalizeVoucher(structuredClone(item));
  },
  async update(id, payload) {
    const idx = db.vouchers.findIndex((v) => String(v.id) === String(id));
    if (idx < 0) throw new Error("Voucher not found");
    db.vouchers[idx] = { ...db.vouchers[idx], ...payload };
    return normalizeVoucher(structuredClone(db.vouchers[idx]));
  },
  async remove(id) {
    const idx = db.vouchers.findIndex((v) => String(v.id) === String(id));
    if (idx < 0) return true;
    db.vouchers.splice(idx, 1);
    return true;
  },
};
