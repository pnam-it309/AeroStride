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

function normalizeEmployee(item) {
  const gioiTinh =
    item.gioiTinh == null
      ? parseGender(item.gender)
      : parseGender(item.gioiTinh);
  const trangThai = parseStatus(item.trangThai ?? item.status);
  const chucVu =
    item.tenPhanQuyen ?? item.quyenHan ?? item.maPhanQuyen ?? item.role ?? "";

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
    trangThai,
    ngayTao: toEpoch(item.ngayTao ?? item.createdAt),
    ngayCapNhat: toEpoch(item.ngayCapNhat ?? item.updatedAt),
    maPhanQuyen: item.maPhanQuyen ?? item.permissionCode ?? "",
    tenPhanQuyen: item.tenPhanQuyen ?? item.permissionName ?? "",
    quyenHan: item.quyenHan ?? item.permissionRole ?? "",
    chucVu,
  };

  return {
    ...normalized,
    code: normalized.ma,
    name: normalized.ten,
    role: normalized.chucVu,
    status: normalized.trangThai,
    createdAt: normalized.ngayTao,
    updatedAt: normalized.ngayCapNhat,
    gioiTinhText:
      normalized.gioiTinh == null ? "-" : normalized.gioiTinh ? "Nam" : "Nữ",
    chucVuText: normalized.chucVu || "-",
  };
}

function mapPayloadForStore(payload) {
  const mapped = { ...payload };

  if (Object.prototype.hasOwnProperty.call(mapped, "status")) {
    mapped.trangThai = parseStatus(mapped.status);
    delete mapped.status;
  }

  if (Object.prototype.hasOwnProperty.call(mapped, "trangThai")) {
    mapped.trangThai = parseStatus(mapped.trangThai);
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

  if (Object.prototype.hasOwnProperty.call(mapped, "role")) {
    mapped.tenPhanQuyen = mapped.role;
    mapped.quyenHan = mapped.role;
    delete mapped.role;
  }

  return mapped;
}

export const employeeService = {
  async list() {
    return structuredClone(db.employees).map(normalizeEmployee);
  },

  async get(id) {
    const row = db.employees.find((x) => String(x.id) === String(id));
    if (!row) throw new Error("Employee not found");
    return normalizeEmployee(structuredClone(row));
  },

  async create(payload) {
    const nextId =
      Math.max(
        0,
        ...db.employees.map((x) => {
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

    db.employees.unshift(item);
    return normalizeEmployee(structuredClone(item));
  },

  async update(id, payload) {
    const idx = db.employees.findIndex((x) => String(x.id) === String(id));
    if (idx < 0) throw new Error("Employee not found");

    const next = {
      ...db.employees[idx],
      ...mapPayloadForStore(payload),
      ngayCapNhat: Date.now(),
    };

    db.employees[idx] = next;
    return normalizeEmployee(structuredClone(next));
  },

  async remove(id) {
    const idx = db.employees.findIndex((x) => String(x.id) === String(id));
    if (idx < 0) return true;
    db.employees.splice(idx, 1);
    return true;
  },
};
