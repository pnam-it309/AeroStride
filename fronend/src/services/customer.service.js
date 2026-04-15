import { db } from "@/services/mock.db";

function toEpoch(value) {
  if (value == null || value === "") return null;
  const num = Number(value);
  if (!Number.isNaN(num) && Number.isFinite(num)) return num;

  const date = new Date(String(value));
  const time = date.getTime();
  return Number.isNaN(time) ? null : time;
}

function parseGender(value) {
  if (typeof value === "boolean") return value;
  const key = String(value ?? "")
    .trim()
    .toLowerCase();
  if (["true", "1", "nam", "male", "m"].includes(key)) return true;
  if (["false", "0", "nữ", "nu", "female", "f"].includes(key)) return false;
  return null;
}

function parseStatus(value) {
  const key = String(value ?? "")
    .trim()
    .toUpperCase();
  if (["ACTIVE", "HOAT_DONG", "HOẠT_ĐỘNG", "HOAT DONG"].includes(key)) {
    return "ACTIVE";
  }
  if (
    [
      "INACTIVE",
      "NGUNG_HOAT_DONG",
      "NGỪNG_HOẠT_ĐỘNG",
      "NGUNG HOAT DONG",
    ].includes(key)
  ) {
    return "INACTIVE";
  }
  return key || "ACTIVE";
}

function normalizeCustomer(item) {
  const gioiTinh =
    item.gioiTinh == null
      ? parseGender(item.gender)
      : parseGender(item.gioiTinh);
  const trangThai = parseStatus(item.trangThai ?? item.status);

  const normalized = {
    id: String(item.id ?? ""),
    ma: item.ma ?? item.code ?? "",
    ten: item.ten ?? item.name ?? "",
    email: item.email ?? "",
    tenTaiKhoan: item.tenTaiKhoan ?? item.username ?? "",
    gioiTinh,
    sdt: item.sdt ?? item.phone ?? "",
    ngaySinh: item.ngaySinh ?? "",
    hinhAnh: item.hinhAnh ?? item.avatar ?? "",
    ghiChu: item.ghiChu ?? item.note ?? "",
    trangThai,
    ngayTao: toEpoch(item.ngayTao ?? item.createdAt),
    ngayCapNhat: toEpoch(item.ngayCapNhat ?? item.updatedAt),
    tinh: item.tinh ?? "",
    thanhPho: item.thanhPho ?? "",
    phuongXa: item.phuongXa ?? "",
    diaChiChiTiet: item.diaChiChiTiet ?? item.address ?? "",
    tenNguoiNhan: item.tenNguoiNhan ?? "",
    sdtNguoiNhan: item.sdtNguoiNhan ?? "",
  };

  return {
    ...normalized,
    code: normalized.ma,
    name: normalized.ten,
    phone: normalized.sdt,
    status: normalized.trangThai,
    createdAt: normalized.ngayTao,
    updatedAt: normalized.ngayCapNhat,
    gioiTinhText:
      normalized.gioiTinh == null ? "-" : normalized.gioiTinh ? "Nam" : "Nữ",
    diaChi: [
      normalized.diaChiChiTiet,
      normalized.phuongXa,
      normalized.thanhPho,
      normalized.tinh,
    ]
      .filter(Boolean)
      .join(", "),
    customerType:
      String(item.loaiKhachHang ?? "").toUpperCase() === "RETAIL"
        ? "RETAIL"
        : normalized.tenTaiKhoan
          ? "ONLINE"
          : "RETAIL",
    customerTypeText:
      String(item.loaiKhachHang ?? "").toUpperCase() === "RETAIL"
        ? "Khách lẻ"
        : normalized.tenTaiKhoan
          ? "Khách online"
          : "Khách lẻ",
  };
}

function mapPayloadForStore(payload) {
  const mapped = { ...payload };

  if (Object.prototype.hasOwnProperty.call(mapped, "status")) {
    mapped.trangThai = parseStatus(mapped.status);
    delete mapped.status;
  }

  if (Object.prototype.hasOwnProperty.call(mapped, "createdAt")) {
    mapped.ngayTao = toEpoch(mapped.createdAt);
    delete mapped.createdAt;
  }

  if (Object.prototype.hasOwnProperty.call(mapped, "updatedAt")) {
    mapped.ngayCapNhat = toEpoch(mapped.updatedAt);
    delete mapped.updatedAt;
  }

  if (Object.prototype.hasOwnProperty.call(mapped, "gioiTinh")) {
    mapped.gioiTinh = parseGender(mapped.gioiTinh);
  }

  if (Object.prototype.hasOwnProperty.call(mapped, "ngayTao")) {
    mapped.ngayTao = toEpoch(mapped.ngayTao);
  }

  if (Object.prototype.hasOwnProperty.call(mapped, "ngayCapNhat")) {
    mapped.ngayCapNhat = toEpoch(mapped.ngayCapNhat);
  }

  return mapped;
}

export const customerService = {
  async list() {
    return structuredClone(db.customers).map(normalizeCustomer);
  },

  async get(id) {
    const row = db.customers.find((x) => String(x.id) === String(id));
    if (!row) throw new Error("Customer not found");
    return normalizeCustomer(structuredClone(row));
  },

  async create(payload) {
    const nextId =
      Math.max(
        0,
        ...db.customers.map((x) => {
          const n = Number(x.id);
          return Number.isNaN(n) ? 0 : n;
        }),
      ) + 1;

    const now = Date.now();
    const item = {
      id: String(nextId),
      ...mapPayloadForStore(payload),
    };

    if (!item.ngayTao) item.ngayTao = now;
    item.ngayCapNhat = now;

    db.customers.unshift(item);
    return normalizeCustomer(structuredClone(item));
  },

  async update(id, payload) {
    const idx = db.customers.findIndex((x) => String(x.id) === String(id));
    if (idx < 0) throw new Error("Customer not found");

    const next = {
      ...db.customers[idx],
      ...mapPayloadForStore(payload),
      ngayCapNhat: Date.now(),
    };

    db.customers[idx] = next;
    return normalizeCustomer(structuredClone(next));
  },

  async remove(id) {
    const idx = db.customers.findIndex((x) => String(x.id) === String(id));
    if (idx < 0) return true;
    db.customers.splice(idx, 1);
    return true;
  },
};
