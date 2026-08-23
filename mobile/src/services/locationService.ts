/**
 * Location Service for AeroStride Mobile
 * Fetches 3-level Vietnamese administrative divisions (Provinces -> Districts -> Wards)
 * With automatic dynamic online API re-sync & offline local fallback
 */

import axios from 'axios';
import {
  LOCAL_PROVINCES,
  LOCAL_DISTRICTS,
  LOCAL_WARDS,
  getFallbackDistricts,
  getFallbackWards,
  type LocationItem,
} from '@/constants/localLocations';

export type { LocationItem };

export const normalizeSearchText = (value: string | null | undefined): string => {
  if (!value) return '';
  return String(value)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
};

let cachedProvinces: LocationItem[] = [];
let isProvincesLive = false;

const cachedDistricts: Record<string, { list: LocationItem[]; isLive: boolean }> = {};
const cachedWards: Record<string, { list: LocationItem[]; isLive: boolean }> = {};

export const locationService = {
  async getProvinces(): Promise<LocationItem[]> {
    if (cachedProvinces.length > 0 && isProvincesLive) {
      return cachedProvinces;
    }
    try {
      const res = await axios.get<any[]>('https://provinces.open-api.vn/api/p/', { timeout: 3000 });
      if (res.data && res.data.length > 0) {
        const list: LocationItem[] = res.data.map((p) => ({
          code: p.code,
          name: p.name,
        }));
        cachedProvinces = list;
        isProvincesLive = true;
        return list;
      }
    } catch (error) {
      console.warn('Network offline / API unavailable, serving local provinces fallback:', error);
    }
    // Khi tat mang: phuc vu bo du lieu offline local
    isProvincesLive = false;
    return LOCAL_PROVINCES;
  },

  async getDistricts(provinceCode: number | string, provinceName?: string): Promise<LocationItem[]> {
    const key = String(provinceCode);
    if (cachedDistricts[key]?.isLive) {
      return cachedDistricts[key].list;
    }
    try {
      const res = await axios.get<any>(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`, { timeout: 3000 });
      if (res.data?.districts && res.data.districts.length > 0) {
        const list: LocationItem[] = res.data.districts.map((d: any) => ({
          code: d.code,
          name: d.name,
        }));
        cachedDistricts[key] = { list, isLive: true };
        return list;
      }
    } catch (error) {
      console.warn(`Network offline / API error for district ${provinceCode}, serving local fallback:`, error);
    }

    // Offline Local Fallback
    const local = LOCAL_DISTRICTS[provinceCode] || LOCAL_DISTRICTS[key] || getFallbackDistricts(provinceName);
    cachedDistricts[key] = { list: local, isLive: false };
    return local;
  },

  async getWards(districtCode: number | string, districtName?: string): Promise<LocationItem[]> {
    const key = String(districtCode);
    if (cachedWards[key]?.isLive) {
      return cachedWards[key].list;
    }
    try {
      const res = await axios.get<any>(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`, { timeout: 3000 });
      if (res.data?.wards && res.data.wards.length > 0) {
        const list: LocationItem[] = res.data.wards.map((w: any) => ({
          code: w.code,
          name: w.name,
        }));
        cachedWards[key] = { list, isLive: true };
        return list;
      }
    } catch (error) {
      console.warn(`Network offline / API error for ward ${districtCode}, serving local fallback:`, error);
    }

    // Offline Local Fallback
    const local = LOCAL_WARDS[districtCode] || LOCAL_WARDS[key] || getFallbackWards(districtName);
    cachedWards[key] = { list: local, isLive: false };
    return local;
  },
};
