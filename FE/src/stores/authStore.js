import {
  AUTHENCATION_STORAGE_REFRESH_TOKEN,
  AUTHENCATION_STORAGE_TOKEN,
  AUTHENCATION_STORAGE_USER_DATA,
} from '@/constants/authenticationConstant'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { jwtDecode } from 'jwt-decode'
import { localStorageUtils } from '@/utils/storageUtil'

/**
 * Authentication Store
 * Handles user state, tokens, and login/logout logic.
 */
const useAuthStore = defineStore('authUser', () => {
  const user = ref(localStorageUtils.get(AUTHENCATION_STORAGE_USER_DATA) || null)
  const token = ref(localStorageUtils.get(AUTHENCATION_STORAGE_TOKEN) || null)
  const refreshToken = ref(localStorageUtils.get(AUTHENCATION_STORAGE_REFRESH_TOKEN) || null)

  const isLogin = computed(() => user.value !== null)

  const decodeToken = (tokenData) => {
    if (!tokenData) return null
    try {
      return jwtDecode(tokenData)
    } catch (error) {
      console.error('Failed to decode token:', error)
      return null
    }
  }

  const setUser = (data) => {
    user.value = data
    localStorageUtils.set(AUTHENCATION_STORAGE_USER_DATA, user.value)
  }

  const setToken = (access_token, refresh_token) => {
    token.value = access_token
    refreshToken.value = refresh_token
    localStorageUtils.set(AUTHENCATION_STORAGE_TOKEN, token.value)
    localStorageUtils.set(AUTHENCATION_STORAGE_REFRESH_TOKEN, refreshToken.value)
  }

  const login = (tokenData) => {
    if (!tokenData) {
      return false
    }

    let parsedData
    try {
      parsedData = JSON.parse(atob(tokenData))
    } catch (e) {
      console.error('Failed to parse login token data:', e)
      return false
    }

    const access_token = parsedData?.accessToken
    const refresh_token = parsedData?.refreshToken

    if (access_token) {
      const decoded = decodeToken(access_token)
      setUser(decoded)
      setToken(access_token, refresh_token)
    }

    if (user.value == null) {
      logout()
      return false
    }

    return true
  }

  const logout = () => {
    user.value = null
    token.value = null
    refreshToken.value = null

    localStorageUtils.remove(AUTHENCATION_STORAGE_TOKEN)
    localStorageUtils.remove(AUTHENCATION_STORAGE_REFRESH_TOKEN)
    localStorageUtils.remove(AUTHENCATION_STORAGE_USER_DATA)
  }

  const updateUser = (data = {}) => {
    setUser({
      ...user.value,
      ...data,
      picture: data?.picture || user.value?.picture,
      role: user.value?.role,
    })
  }

  return { user, token, refreshToken, isLogin, login, logout, updateUser, setToken }
})

export default useAuthStore
