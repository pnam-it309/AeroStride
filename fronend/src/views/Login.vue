<template>
  <section class="loginPage">
    <div class="glow glow-a"></div>
    <div class="glow glow-b"></div>

    <form class="card" @submit.prevent="submit">
      <p class="eyebrow">Truy cập quản trị</p>
      <h1>Đăng nhập hệ thống bán giày</h1>
      <p class="muted">Sử dụng tài khoản quản trị để tiếp tục.</p>

      <div class="formGrid">
        <AppInput
          label="Tài khoản"
          placeholder="admin"
          v-model="form.username"
        />
        <AppInput
          label="Mật khẩu"
          type="password"
          placeholder="******"
          v-model="form.password"
        />
      </div>

      <div v-if="error" class="error">{{ error }}</div>

      <div class="actions">
        <AppButton type="submit">Đăng nhập</AppButton>
      </div>
    </form>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth.store";
import AppInput from "@/components/common/AppInput.vue";
import AppButton from "@/components/common/AppButton.vue";

const router = useRouter();
const auth = useAuthStore();

const form = reactive({
  username: "admin",
  password: "",
});

const error = ref("");

function submit() {
  if (!form.username.trim() || !form.password.trim()) {
    error.value = "Vui lòng nhập đầy đủ tài khoản và mật khẩu";
    return;
  }

  error.value = "";
  auth.login({ username: form.username.trim() });
  router.push({ name: "dashboard" });
}
</script>

<style scoped>
.loginPage {
  min-height: 100vh;
  background: linear-gradient(135deg, #f4f8ff 0%, #ecf5ff 45%, #f7fbff 100%);
  display: grid;
  place-items: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.glow {
  position: absolute;
  width: 480px;
  height: 480px;
  border-radius: 50%;
  filter: blur(72px);
  opacity: 0.45;
  pointer-events: none;
}

.glow-a {
  background: #93c5fd;
  top: -140px;
  left: -120px;
}

.glow-b {
  background: #86efac;
  bottom: -180px;
  right: -120px;
}

.card {
  position: relative;
  width: min(460px, 100%);
  border: 1px solid #d7e4f6;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 50px rgba(11, 51, 92, 0.12);
  padding: 24px;
  display: grid;
  gap: 12px;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #0369a1;
  font-weight: 800;
}

h1 {
  font-size: 28px;
  line-height: 1.2;
}

.formGrid {
  display: grid;
  gap: 12px;
}

.error {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #991b1b;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 13px;
}

.actions {
  display: flex;
  justify-content: flex-end;
}
</style>
