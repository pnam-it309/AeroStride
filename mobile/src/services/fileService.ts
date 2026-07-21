/**
 * File/Storage Service - resolves image URLs from BE storage
 */

import { API_CONFIG } from '@/config/api';

const isAbsoluteUrl = (value: string | null | undefined): boolean => {
  if (!value) return false;
  return /^(https?:)?\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:');
};

export const fileService = {
  /**
   * Get full image URL from a file path stored in BE
   * Matches FE dichVuFile.layUrlFile pattern
   */
  getFileUrl(filePath: string | null | undefined): string {
    if (!filePath) return '';
    if (isAbsoluteUrl(filePath)) return filePath;

    const cleanPath = filePath.replace(/^\/+/, '');
    if (cleanPath.startsWith('api/v1/')) {
      return `${API_CONFIG.ORIGIN}/${cleanPath}`;
    }
    return `${API_CONFIG.ORIGIN}/${cleanPath}`;
  },

  /**
   * Get image source for React Native Image component
   * Returns either a URI object or undefined for fallback
   */
  getImageSource(filePath: string | null | undefined): { uri: string } | undefined {
    const url = this.getFileUrl(filePath);
    if (!url) return undefined;
    return { uri: url };
  },
};
