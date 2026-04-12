<template>
  <div class="tableWrap">
    <table>
      <thead>
        <tr>
          <th
            v-for="col in columns"
            :key="col.key"
            :class="alignClass(col.key)"
            :style="col.width ? { width: col.width } : null"
          >
            {{ col.label }}
          </th>
        </tr>
      </thead>

      <tbody>
        <tr v-if="!rows.length">
          <td :colspan="columns.length" class="empty">Không có dữ liệu</td>
        </tr>

        <tr v-for="(row, idx) in rows" :key="row._key ?? row.id ?? idx">
          <td
            v-for="col in columns"
            :key="col.key"
            :class="alignClass(col.key)"
          >
            <slot :name="`cell:${col.key}`" :row="row" :value="row[col.key]">
              {{ row[col.key] }}
            </slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  columns: { type: Array, default: () => [] },
  rows: { type: Array, default: () => [] },
});

function alignClass(key) {
  if (
    ["status", "actions", "id", "qty", "value", "gioiTinhText", "stt"].includes(
      key,
    )
  ) {
    return "centerCell";
  }

  return "leftCell";
}
</script>

<style scoped>
.tableWrap {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
}

table {
  width: max-content;
  min-width: 100%;
  table-layout: auto;
  border-collapse: separate;
  border-spacing: 0;
}

th,
td {
  border-bottom: 1px solid #edf2fa;
  padding: 12px 10px;
  vertical-align: middle;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.leftCell {
  text-align: left;
}

.centerCell {
  text-align: center;
}

th {
  color: #2f3b4f;
  font-size: 12px;
  font-weight: 700;
  background: #eef2f7;
  position: sticky;
  top: 0;
}

th:first-child {
  border-top-left-radius: 10px;
  border-bottom-left-radius: 10px;
}

th:last-child {
  border-top-right-radius: 10px;
  border-bottom-right-radius: 10px;
}

tbody tr:hover td {
  background: #fafcff;
}

.empty {
  text-align: center;
  color: var(--muted);
  padding: 16px;
}
</style>
