import { createRouter, createWebHistory } from "vue-router";
import AdminLayout from "@/layouts/AdminLayout.vue";

import Login from "@/views/Login.vue";
import Dashboard from "@/views/Dashboard.vue";

import VoucherList from "@/views/vouchers/VoucherList.vue";
import VoucherCreate from "@/views/vouchers/VoucherCreate.vue";
import VoucherEdit from "@/views/vouchers/VoucherEdit.vue";
import VoucherDetail from "@/views/vouchers/VoucherDetail.vue";

import ProductList from "@/views/products/ProductList.vue";
import ProductCreate from "@/views/products/ProductCreate.vue";
import ProductEdit from "@/views/products/ProductEdit.vue";
import ProductDetail from "@/views/products/ProductDetail.vue";

import SalesList from "@/views/sales/SalesList.vue";
import SalesCreate from "@/views/sales/SalesCreate.vue";
import SalesEdit from "@/views/sales/SalesEdit.vue";
import SalesDetail from "@/views/sales/SalesDetail.vue";

import DiscountList from "@/views/discounts/DiscountList.vue";
import DiscountCreate from "@/views/discounts/DiscountCreate.vue";
import DiscountEdit from "@/views/discounts/DiscountEdit.vue";
import DiscountDetail from "@/views/discounts/DiscountDetail.vue";

import OrderList from "@/views/orders/OrderList.vue";
import OrderCreate from "@/views/orders/OrderCreate.vue";
import OrderEdit from "@/views/orders/OrderEdit.vue";
import OrderDetail from "@/views/orders/OrderDetail.vue";

import AttributeList from "@/views/attributes/AttributeList.vue";
import AttributeCreate from "@/views/attributes/AttributeCreate.vue";
import AttributeEdit from "@/views/attributes/AttributeEdit.vue";
import AttributeDetail from "@/views/attributes/AttributeDetail.vue";

import CustomerList from "@/views/customers/CustomerList.vue";
import CustomerCreate from "@/views/customers/CustomerCreate.vue";
import CustomerEdit from "@/views/customers/CustomerEdit.vue";
import CustomerDetail from "@/views/customers/CustomerDetail.vue";

import EmployeeList from "@/views/employees/EmployeeList.vue";
import EmployeeCreate from "@/views/employees/EmployeeCreate.vue";
import EmployeeEdit from "@/views/employees/EmployeeEdit.vue";
import EmployeeDetail from "@/views/employees/EmployeeDetail.vue";

import PaymentList from "@/views/payments/PaymentList.vue";
import PaymentCreate from "@/views/payments/PaymentCreate.vue";
import PaymentEdit from "@/views/payments/PaymentEdit.vue";
import PaymentDetail from "@/views/payments/PaymentDetail.vue";

import FileList from "@/views/files/FileList.vue";
import FileCreate from "@/views/files/FileCreate.vue";
import FileEdit from "@/views/files/FileEdit.vue";
import FileDetail from "@/views/files/FileDetail.vue";

import { useAuthStore } from "@/stores/auth.store";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", name: "login", component: Login },

    {
      path: "/",
      component: AdminLayout,
      children: [
        { path: "", name: "dashboard", component: Dashboard },

        // Sales
        { path: "sales", name: "sales.list", component: SalesList },
        { path: "sales/create", name: "sales.create", component: SalesCreate },
        { path: "sales/:id", name: "sales.detail", component: SalesDetail },
        { path: "sales/:id/edit", name: "sales.edit", component: SalesEdit },

        // Discounts
        {
          path: "discounts",
          name: "discounts.list",
          component: DiscountList,
        },
        {
          path: "discounts/create",
          name: "discounts.create",
          component: DiscountCreate,
        },
        {
          path: "discounts/:id",
          name: "discounts.detail",
          component: DiscountDetail,
        },
        {
          path: "discounts/:id/edit",
          name: "discounts.edit",
          component: DiscountEdit,
        },

        // Orders
        { path: "orders", name: "orders.list", component: OrderList },
        {
          path: "orders/create",
          name: "orders.create",
          component: OrderCreate,
        },
        { path: "orders/:id", name: "orders.detail", component: OrderDetail },
        { path: "orders/:id/edit", name: "orders.edit", component: OrderEdit },

        // Products
        { path: "products", name: "products.list", component: ProductList },
        {
          path: "products/create",
          name: "products.create",
          component: ProductCreate,
        },
        {
          path: "products/:id",
          name: "products.detail",
          component: ProductDetail,
        },
        {
          path: "products/:id/edit",
          name: "products.edit",
          component: ProductEdit,
        },

        // Attributes
        {
          path: "attributes",
          name: "attributes.list",
          component: AttributeList,
        },
        {
          path: "attributes/create",
          name: "attributes.create",
          component: AttributeCreate,
        },
        {
          path: "attributes/:id",
          name: "attributes.detail",
          component: AttributeDetail,
        },
        {
          path: "attributes/:id/edit",
          name: "attributes.edit",
          component: AttributeEdit,
        },

        // Vouchers
        { path: "vouchers", name: "vouchers.list", component: VoucherList },
        {
          path: "vouchers/create",
          name: "vouchers.create",
          component: VoucherCreate,
        },
        {
          path: "vouchers/:id",
          name: "vouchers.detail",
          component: VoucherDetail,
        },
        {
          path: "vouchers/:id/edit",
          name: "vouchers.edit",
          component: VoucherEdit,
        },

        // Customers
        { path: "customers", name: "customers.list", component: CustomerList },
        {
          path: "customers/create",
          name: "customers.create",
          component: CustomerCreate,
        },
        {
          path: "customers/:id",
          name: "customers.detail",
          component: CustomerDetail,
        },
        {
          path: "customers/:id/edit",
          name: "customers.edit",
          component: CustomerEdit,
        },

        // Employees
        { path: "employees", name: "employees.list", component: EmployeeList },
        {
          path: "employees/create",
          name: "employees.create",
          component: EmployeeCreate,
        },
        {
          path: "employees/:id",
          name: "employees.detail",
          component: EmployeeDetail,
        },
        {
          path: "employees/:id/edit",
          name: "employees.edit",
          component: EmployeeEdit,
        },

        // Payments
        { path: "payments", name: "payments.list", component: PaymentList },
        {
          path: "payments/create",
          name: "payments.create",
          component: PaymentCreate,
        },
        {
          path: "payments/:id",
          name: "payments.detail",
          component: PaymentDetail,
        },
        {
          path: "payments/:id/edit",
          name: "payments.edit",
          component: PaymentEdit,
        },

        // Files
        { path: "files", name: "files.list", component: FileList },
        { path: "files/create", name: "files.create", component: FileCreate },
        { path: "files/:id", name: "files.detail", component: FileDetail },
        { path: "files/:id/edit", name: "files.edit", component: FileEdit },
      ],
    },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.name !== "login" && !auth.isAuthenticated) return { name: "login" };
  if (to.name === "login" && auth.isAuthenticated) return { name: "dashboard" };

  const readOnlyRoutes = [
    "orders.create",
    "orders.edit",
    "payments.create",
    "payments.edit",
  ];

  if (readOnlyRoutes.includes(String(to.name))) {
    if (String(to.name).startsWith("orders.")) return { name: "orders.list" };
    if (String(to.name).startsWith("payments.")) {
      return { name: "payments.list" };
    }
  }
});

export default router;
