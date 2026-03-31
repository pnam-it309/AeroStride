import { createRouter, createWebHistory } from 'vue-router'
import setupRouteGuards from './routeGuards'
import { ROUTES } from '../constants'

import AuthLayout from '../layouts/AuthLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: ROUTES.AUTH.BASE,
      component: AuthLayout,
      meta: { guestOnly: true },
      children: [
        { path: 'login', name: 'Login', component: () => import('../pages/Login.vue') },
        { path: 'register', name: 'Register', component: () => import('../pages/Register.vue') },
      ],
      redirect: ROUTES.AUTH.LOGIN,
    },
    {
      path: ROUTES.ADMIN.BASE,
      component: AdminLayout,
      meta: { requiresAuth: true },
      children: [
        // Error Pages
        {
          path: 'error/403',
          name: 'Error403',
          component: () => import('../pages/admin/errors/403/Error403.vue'),
          meta: { title: '403 Forbidden' },
        },
        {
          path: 'error/404',
          name: 'Error404',
          component: () => import('../pages/admin/errors/404/Error404.vue'),
          meta: { title: '404 Not Found' },
        },
        {
          path: 'error/500',
          name: 'Error500',
          component: () => import('../pages/admin/errors/500/Error500.vue'),
          meta: { title: '500 Server Error' },
        },
      ],
      redirect: ROUTES.ADMIN.DASHBOARD,
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: ROUTES.ADMIN.ERRORS.E404,
    },
    { path: ROUTES.HOME, redirect: ROUTES.AUTH.LOGIN },
  ],
})

import { useLoadingStore } from '@/stores/loadingStore'

router.beforeEach((to, from, next) => {
  const loadingStore = useLoadingStore()
  loadingStore.startProgress()
  next()
})

router.afterEach(() => {
  const loadingStore = useLoadingStore()
  loadingStore.finishProgress()
})

setupRouteGuards(router)

export default router
