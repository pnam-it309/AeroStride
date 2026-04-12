import { computed, ref } from "vue";
import { defineStore } from "pinia";

const AUTH_KEY = "shoe-admin-auth";
const AUTH_PROFILE_KEY = "shoe-admin-auth-profile";

function inferEmployeeCode(username) {
  const raw = String(username || "").trim();
  if (!raw) return "";

  const match = raw.match(/^(nv)[-_ ]?(\d+)$/i);
  if (!match) return raw.toUpperCase();

  return `NV-${match[2]}`;
}

function buildProfile(payload = {}) {
  const username = String(payload.username || "admin").trim();
  const role = String(payload.role || "")
    .trim()
    .toLowerCase();
  const isAdmin = role === "admin" || username.toLowerCase() === "admin";

  if (isAdmin) {
    return {
      username,
      role: "admin",
      employeeCode: "",
    };
  }

  return {
    username,
    role: "employee",
    employeeCode: String(payload.employeeCode || inferEmployeeCode(username)),
  };
}

function profileFromToken(token) {
  const raw = String(token || "");
  const username = raw.endsWith("-token") ? raw.slice(0, -6) : "";
  if (!username) return buildProfile({ username: "admin" });
  return buildProfile({ username });
}

function readStoredProfile() {
  try {
    const raw = localStorage.getItem(AUTH_PROFILE_KEY);
    if (!raw) return null;
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export const useAuthStore = defineStore("auth", () => {
  const token = ref(localStorage.getItem(AUTH_KEY) || "");
  const profile = ref(readStoredProfile() || profileFromToken(token.value));

  const isAuthenticated = computed(() => Boolean(token.value));
  const avatarText = computed(() => {
    if (profile.value?.role === "admin") return "Admin";
    const code = String(profile.value?.employeeCode || "").trim();
    return code || "Nhân viên";
  });
  const avatarTitle = computed(() =>
    profile.value?.role === "admin" ? "Quản trị" : "Nhân viên",
  );

  function login(payload = {}) {
    const username = String(payload.username || "admin");
    token.value = `${username}-token`;
    profile.value = buildProfile(payload);
    localStorage.setItem(AUTH_KEY, token.value);
    localStorage.setItem(AUTH_PROFILE_KEY, JSON.stringify(profile.value));
  }

  function logout() {
    token.value = "";
    profile.value = buildProfile({ username: "admin", role: "admin" });
    localStorage.removeItem(AUTH_KEY);
    localStorage.removeItem(AUTH_PROFILE_KEY);
  }

  return {
    token,
    profile,
    isAuthenticated,
    avatarText,
    avatarTitle,
    login,
    logout,
  };
});
