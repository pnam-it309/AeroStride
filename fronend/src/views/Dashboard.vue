<template>
  <section class="dashboard">
    <header class="hero">
      <div>
        <p class="eyebrow">Tổng quan hệ thống</p>
        <h1 class="title">Thống kê bán hàng</h1>
      </div>
      <div class="hero-chip">
        <span class="chip-dot"></span>
        Cập nhật: {{ todayLabel }}
      </div>
    </header>

    <div class="kpi-grid">
      <article class="kpi-card kpi-revenue">
        <p class="kpi-label">Tổng doanh thu</p>
        <h2 class="kpi-value">{{ formatCurrency(totalRevenue) }}</h2>
        <p class="kpi-note">Từ {{ orders.length }} hóa đơn</p>
      </article>

      <article class="kpi-card kpi-orders">
        <p class="kpi-label">Đơn thanh toán</p>
        <h2 class="kpi-value">{{ paidOrders }}/{{ orders.length }}</h2>
        <p class="kpi-note">Tỷ lệ {{ paidRate }}%</p>
      </article>

      <article class="kpi-card kpi-customers">
        <p class="kpi-label">Khách hàng</p>
        <h2 class="kpi-value">{{ customers.length }}</h2>
        <p class="kpi-note">Đang hoạt động trong hệ thống</p>
      </article>

      <article class="kpi-card kpi-products">
        <p class="kpi-label">Sản phẩm</p>
        <h2 class="kpi-value">{{ products.length }}</h2>
        <p class="kpi-note">SKU hiện có</p>
      </article>
    </div>

    <div class="panel-grid">
      <section class="panel revenue-panel">
        <div class="panel-head">
          <h3>Doanh thu 7 ngày</h3>
          <span class="legend">Đơn vị: Triệu VND</span>
        </div>
        <div class="bars">
          <div
            v-for="point in revenueSeries"
            :key="point.date"
            class="bar-item"
          >
            <div class="bar-track">
              <div
                class="bar-fill"
                :style="{ height: `${point.height}%` }"
                :title="`${point.date}: ${formatCurrency(point.value)}`"
              ></div>
            </div>
            <p class="bar-label">{{ point.shortDate }}</p>
          </div>
        </div>
      </section>

      <section class="panel status-panel">
        <div class="panel-head">
          <h3>Trạng thái thanh toán</h3>
        </div>
        <ul class="status-list">
          <li>
            <span>PAID</span>
            <strong>{{ paidOrders }}</strong>
          </li>
          <li>
            <span>PENDING</span>
            <strong>{{ pendingOrders }}</strong>
          </li>
          <li>
            <span>UNPAID</span>
            <strong>{{ unpaidOrders }}</strong>
          </li>
        </ul>
      </section>
    </div>

    <section class="panel table-panel">
      <div class="panel-head">
        <h3>Hóa đơn gần đây</h3>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Mã</th>
              <th>Khách hàng</th>
              <th>Tổng tiền</th>
              <th>Ngày tạo</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in recentOrders" :key="order.id">
              <td>{{ order.code }}</td>
              <td>{{ order.customer }}</td>
              <td>{{ formatCurrency(order.total) }}</td>
              <td>{{ order.createdAt }}</td>
              <td>
                <span class="badge" :class="badgeClass(order.status)">
                  {{ order.status }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed } from "vue";
import { db } from "@/services/mock.db";

const orders = computed(() => db.orders ?? []);
const customers = computed(() => db.customers ?? []);
const products = computed(() => db.products ?? []);

const totalRevenue = computed(() =>
  orders.value.reduce((sum, order) => sum + Number(order.total || 0), 0),
);

const paidOrders = computed(
  () =>
    orders.value.filter((x) => String(x.status).toUpperCase() === "PAID")
      .length,
);
const pendingOrders = computed(
  () =>
    orders.value.filter((x) => String(x.status).toUpperCase() === "PENDING")
      .length,
);
const unpaidOrders = computed(
  () =>
    orders.value.filter((x) => String(x.status).toUpperCase() === "UNPAID")
      .length,
);

const paidRate = computed(() => {
  if (!orders.value.length) return 0;
  return Math.round((paidOrders.value / orders.value.length) * 100);
});

const recentOrders = computed(() => {
  return [...orders.value]
    .sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))
    .slice(0, 6);
});

const revenueSeries = computed(() => {
  const map = new Map();
  for (const order of orders.value) {
    const date = String(order.createdAt || "");
    const value = Number(order.total || 0);
    map.set(date, (map.get(date) || 0) + value);
  }

  const points = Array.from(map.entries())
    .sort((a, b) => b[0].localeCompare(a[0]))
    .slice(0, 7)
    .reverse()
    .map(([date, value]) => ({
      date,
      shortDate: date.slice(5),
      value,
    }));

  const max = Math.max(...points.map((x) => x.value), 1);
  return points.map((p) => ({
    ...p,
    height: Math.max(12, Math.round((p.value / max) * 100)),
  }));
});

const todayLabel = new Date().toLocaleDateString("vi-VN", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
});

function formatCurrency(value) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number(value || 0));
}

function badgeClass(status) {
  const x = String(status).toUpperCase();
  if (x === "PAID") return "badge-paid";
  if (x === "PENDING") return "badge-pending";
  return "badge-unpaid";
}
</script>

<style scoped>
.dashboard {
  --bg: #f5f7fb;
  --panel: #ffffff;
  --text: #0f172a;
  --muted: #667085;
  --line: #e5eaf2;
  --accent: #0e9f6e;
  --accent-soft: #d1fae5;
  --warn: #d97706;
  --warn-soft: #ffedd5;
  --danger: #c81e1e;
  --danger-soft: #fee2e2;

  padding: 24px;
  min-height: 100%;
  color: var(--text);
  background:
    radial-gradient(circle at 15% -10%, #dff3ff 0%, transparent 38%),
    radial-gradient(circle at 85% -20%, #e7f8e8 0%, transparent 44%), var(--bg);
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #0f766e;
  font-weight: 700;
}

.title {
  margin: 6px 0;
  font-size: clamp(20px, 2.2vw, 28px);
  line-height: 1.2;
  font-weight: 700;
}

.subtitle {
  margin: 0;
  color: var(--muted);
  max-width: 720px;
}

.hero-chip {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.chip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.14);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.kpi-card {
  border-radius: 18px;
  padding: 18px;
  color: #fff;
  box-shadow: 0 12px 24px rgba(18, 38, 63, 0.12);
}

.kpi-revenue {
  background: linear-gradient(135deg, #0891b2, #0ea5e9);
}

.kpi-orders {
  background: linear-gradient(135deg, #2563eb, #4f46e5);
}

.kpi-customers {
  background: linear-gradient(135deg, #16a34a, #0d9488);
}

.kpi-products {
  background: linear-gradient(135deg, #dc2626, #f97316);
}

.kpi-label {
  margin: 0;
  opacity: 0.92;
  font-size: 13px;
}

.kpi-value {
  margin: 8px 0 2px;
  font-size: clamp(22px, 2vw, 30px);
  line-height: 1.2;
}

.kpi-note {
  margin: 0;
  font-size: 13px;
  opacity: 0.9;
}

.panel-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 14px;
  margin-bottom: 14px;
}

.panel {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 16px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.04);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.panel-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.legend {
  font-size: 12px;
  color: var(--muted);
}

.bars {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  align-items: end;
  min-height: 220px;
  padding: 4px 0;
}

.bar-item {
  display: grid;
  gap: 8px;
}

.bar-track {
  height: 180px;
  border-radius: 12px;
  background: linear-gradient(180deg, #f3f6fb, #eef2f9);
  display: flex;
  align-items: flex-end;
  padding: 6px;
}

.bar-fill {
  width: 100%;
  border-radius: 8px;
  background: linear-gradient(180deg, #22c55e, #0ea5e9);
  transition: height 0.35s ease;
}

.bar-label {
  margin: 0;
  text-align: center;
  font-size: 12px;
  color: var(--muted);
}

.status-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.status-list li {
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-list span {
  color: var(--muted);
  font-size: 13px;
}

.status-list strong {
  font-size: 22px;
}

.table-wrap {
  overflow: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 760px;
}

th,
td {
  padding: 12px 10px;
  border-bottom: 1px solid var(--line);
  text-align: left;
  font-size: 14px;
}

th {
  color: #334155;
  font-weight: 700;
  background: #fafcff;
}

.badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.badge-paid {
  color: var(--accent);
  background: var(--accent-soft);
}

.badge-pending {
  color: var(--warn);
  background: var(--warn-soft);
}

.badge-unpaid {
  color: var(--danger);
  background: var(--danger-soft);
}

@media (max-width: 1100px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-grid {
    grid-template-columns: 1fr;
  }

  .bars {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    row-gap: 14px;
  }
}

@media (max-width: 700px) {
  .dashboard {
    padding: 14px;
  }

  .hero {
    flex-direction: column;
    align-items: stretch;
  }

  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .bars {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
