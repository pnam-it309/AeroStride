export function createMockCrudService({ db, key }) {
  return {
    async list() {
      return structuredClone(db[key]);
    },
    async get(id) {
      const x = db[key].find((it) => String(it.id) === String(id));
      if (!x) throw new Error("Not found");
      return structuredClone(x);
    },
    async create(payload) {
      const nextId =
        (db[key].length ? Math.max(...db[key].map((x) => x.id)) : 0) + 1;
      const item = { id: nextId, ...payload };
      db[key].unshift(item);
      return structuredClone(item);
    },
    async update(id, payload) {
      const idx = db[key].findIndex((it) => String(it.id) === String(id));
      if (idx < 0) throw new Error("Not found");
      db[key][idx] = { ...db[key][idx], ...payload };
      return structuredClone(db[key][idx]);
    },
    async remove(id) {
      const idx = db[key].findIndex((it) => String(it.id) === String(id));
      if (idx >= 0) db[key].splice(idx, 1);
      return true;
    },
  };
}
