import { dichVuXacThuc } from './dichVuXacThuc';

const DEFAULT_GOOGLE_CLIENT_ID = '165274553322-qnrdfe6veno65rhsimgrupor69rm9sg.apps.googleusercontent.com';

/**
 * SocialAuthService - Handles Google and Facebook OAuth client workflows
 */
export const socialAuthService = {
    /**
     * Lấy Google Client ID từ biến môi trường hoặc fallback mặc định
     */
    getGoogleClientId(overrideId = null) {
        return overrideId || import.meta.env.VITE_GOOGLE_CLIENT_ID || DEFAULT_GOOGLE_CLIENT_ID;
    },

    /**
     * Đảm bảo Google Identity Services (GIS) SDK đã được tải vào DOM
     */
    ensureGoogleSdkLoaded() {
        return new Promise((resolve, reject) => {
            if (window.google?.accounts?.oauth2) {
                resolve();
                return;
            }

            const existingScript = document.getElementById('google-jssdk');
            if (existingScript) {
                if (window.google?.accounts?.oauth2) {
                    resolve();
                    return;
                }
                existingScript.addEventListener('load', () => resolve());
                existingScript.addEventListener('error', () => reject(new Error('Không thể tải Google Identity Services SDK.')));
                return;
            }

            const script = document.createElement('script');
            script.id = 'google-jssdk';
            script.src = 'https://accounts.google.com/gsi/client';
            script.async = true;
            script.defer = true;
            script.onload = () => resolve();
            script.onerror = () => reject(new Error('Không thể tải Google Identity Services SDK.'));
            document.head.appendChild(script);
        });
    },

    /**
     * Khởi tạo Google Identity Services nếu cần
     */
    initGoogle(clientId, callback) {
        if (typeof window === 'undefined') return;

        const googleClientId = this.getGoogleClientId(clientId);
        if (!googleClientId) return;

        this.ensureGoogleSdkLoaded().then(() => {
            if (window.google?.accounts?.id && callback) {
                window.google.accounts.id.initialize({
                    client_id: googleClientId,
                    callback: callback
                });
            }
        }).catch((e) => {
            console.warn('[SocialAuthService] Khởi tạo Google SDK:', e.message);
        });
    },

    /**
     * Lấy Access Token từ Google Identity Services Popup
     */
    async getGoogleToken() {
        const googleClientId = this.getGoogleClientId();
        if (!googleClientId) {
            throw new Error('Chưa cấu hình Google Client ID (VITE_GOOGLE_CLIENT_ID).');
        }

        await this.ensureGoogleSdkLoaded();

        if (!window.google?.accounts?.oauth2) {
            throw new Error('Google Identity Services SDK chưa sẵn sàng. Vui lòng thử lại sau vài giây.');
        }

        return new Promise((resolve, reject) => {
            try {
                const client = window.google.accounts.oauth2.initTokenClient({
                    client_id: googleClientId,
                    scope: 'email profile openid',
                    callback: (tokenResponse) => {
                        if (tokenResponse.error) {
                            if (tokenResponse.error === 'access_denied') {
                                reject(new Error('Bạn đã hủy đăng nhập Google.'));
                            } else {
                                reject(new Error(tokenResponse.error_description || tokenResponse.error));
                            }
                            return;
                        }
                        resolve(tokenResponse.access_token);
                    }
                });
                client.requestAccessToken({ prompt: 'select_account' });
            } catch (err) {
                reject(err);
            }
        });
    },

    /**
     * Đăng nhập thật với Google OAuth Popup (fallback)
     */
    async loginWithGoogle(customPayload = null) {
        if (customPayload) {
            return await dichVuXacThuc.dangNhapSocial({
                provider: 'GOOGLE',
                ...customPayload
            });
        }

        const token = await this.getGoogleToken();
        return await dichVuXacThuc.dangNhapSocial({
            provider: 'GOOGLE',
            token
        });
    },

    /**
     * Đảm bảo Facebook SDK đã được tải vào DOM
     */
    ensureFacebookSdkLoaded(appId) {
        return new Promise((resolve, reject) => {
            if (window.FB) {
                resolve();
                return;
            }

            const fbAppId = appId || import.meta.env.VITE_FACEBOOK_APP_ID;
            if (!fbAppId) {
                reject(new Error('Chưa cấu hình Facebook App ID (VITE_FACEBOOK_APP_ID).'));
                return;
            }

            const existingScript = document.getElementById('facebook-jssdk');
            if (existingScript) {
                if (window.FB) {
                    resolve();
                    return;
                }
                existingScript.addEventListener('load', () => resolve());
                existingScript.addEventListener('error', () => reject(new Error('Không thể tải Facebook SDK.')));
                return;
            }

            window.fbAsyncInit = function () {
                window.FB.init({
                    appId: fbAppId,
                    cookie: true,
                    xfbml: true,
                    version: 'v19.0'
                });
                resolve();
            };

            const script = document.createElement('script');
            script.id = 'facebook-jssdk';
            script.src = 'https://connect.facebook.net/vi_VN/sdk.js';
            script.async = true;
            script.defer = true;
            script.onerror = () => reject(new Error('Không thể tải Facebook SDK.'));
            document.head.appendChild(script);
        });
    },

    /**
     * Khởi tạo Facebook SDK nếu có App ID
     */
    initFacebook(appId) {
        if (typeof window === 'undefined') return;
        const fbAppId = appId || import.meta.env.VITE_FACEBOOK_APP_ID;
        if (!fbAppId) return;
        this.ensureFacebookSdkLoaded(fbAppId).catch((e) => {
            console.warn('[SocialAuthService] Khởi tạo Facebook SDK:', e.message);
        });
    },

    /**
     * Đăng nhập thật với Facebook OAuth Popup
     */
    async loginWithFacebook(customPayload = null) {
        if (customPayload) {
            return await dichVuXacThuc.dangNhapSocial({
                provider: 'FACEBOOK',
                ...customPayload
            });
        }

        const fbAppId = import.meta.env.VITE_FACEBOOK_APP_ID;
        if (!fbAppId) {
            throw new Error('Chưa cấu hình Facebook App ID (VITE_FACEBOOK_APP_ID).');
        }

        await this.ensureFacebookSdkLoaded(fbAppId);

        if (!window.FB) {
            throw new Error('Facebook SDK chưa sẵn sàng. Vui lòng thử lại sau.');
        }

        return new Promise((resolve, reject) => {
            window.FB.login(
                async (response) => {
                    if (response.authResponse) {
                        try {
                            const result = await dichVuXacThuc.dangNhapSocial({
                                provider: 'FACEBOOK',
                                token: response.authResponse.accessToken,
                                providerId: response.authResponse.userID
                            });
                            resolve(result);
                        } catch (err) {
                            reject(err);
                        }
                    } else {
                        reject(new Error('Bạn đã hủy đăng nhập Facebook.'));
                    }
                },
                { scope: 'public_profile,email' }
            );
        });
    }
};
