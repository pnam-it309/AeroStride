import { dichVuXacThuc } from './dichVuXacThuc';

/**
 * SocialAuthService - Handles Google and Facebook OAuth client workflows
 */
export const socialAuthService = {
    /**
     * Khởi tạo Google Identity Services (GIS) nếu có Client ID
     */
    initGoogle(clientId, callback) {
        if (typeof window === 'undefined') return;
        
        const googleClientId = clientId || import.meta.env.VITE_GOOGLE_CLIENT_ID;
        if (!googleClientId) return;

        if (!document.getElementById('google-jssdk')) {
            const script = document.createElement('script');
            script.id = 'google-jssdk';
            script.src = 'https://accounts.google.com/gsi/client';
            script.async = true;
            script.defer = true;
            script.onload = () => {
                if (window.google?.accounts?.id) {
                    window.google.accounts.id.initialize({
                        client_id: googleClientId,
                        callback: callback
                    });
                }
            };
            document.head.appendChild(script);
        }
    },

    /**
     * Đăng nhập với Google
     */
    async loginWithGoogle(customPayload = null) {
        if (customPayload) {
            return await dichVuXacThuc.dangNhapSocial({
                provider: 'GOOGLE',
                ...customPayload
            });
        }

        const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

        // Nếu có Client ID và GIS SDK đã tải
        if (googleClientId && window.google?.accounts?.oauth2) {
            return new Promise((resolve, reject) => {
                try {
                    const client = window.google.accounts.oauth2.initTokenClient({
                        client_id: googleClientId,
                        scope: 'email profile openid',
                        callback: async (tokenResponse) => {
                            if (tokenResponse.error) {
                                reject(new Error(tokenResponse.error_description || tokenResponse.error));
                                return;
                            }
                            try {
                                const result = await dichVuXacThuc.dangNhapSocial({
                                    provider: 'GOOGLE',
                                    token: tokenResponse.access_token
                                });
                                resolve(result);
                            } catch (err) {
                                reject(err);
                            }
                        }
                    });
                    client.requestAccessToken();
                } catch (err) {
                    reject(err);
                }
            });
        }

        // Fallback / Dev helper cho Google login
        return await dichVuXacThuc.dangNhapSocial({
            provider: 'GOOGLE',
            email: 'google.customer@aerostride.vn',
            name: 'Khách Hàng Google',
            avatarUrl: 'https://lh3.googleusercontent.com/a/default-user=s96-c',
            providerId: 'google_' + Date.now()
        });
    },

    /**
     * Khởi tạo Facebook SDK nếu có App ID
     */
    initFacebook(appId) {
        if (typeof window === 'undefined') return;
        
        const fbAppId = appId || import.meta.env.VITE_FACEBOOK_APP_ID;
        if (!fbAppId) return;

        if (!document.getElementById('facebook-jssdk')) {
            window.fbAsyncInit = function () {
                window.FB.init({
                    appId: fbAppId,
                    cookie: true,
                    xfbml: true,
                    version: 'v19.0'
                });
            };

            const script = document.createElement('script');
            script.id = 'facebook-jssdk';
            script.src = 'https://connect.facebook.net/vi_VN/sdk.js';
            script.async = true;
            script.defer = true;
            document.head.appendChild(script);
        }
    },

    /**
     * Đăng nhập với Facebook
     */
    async loginWithFacebook(customPayload = null) {
        if (customPayload) {
            return await dichVuXacThuc.dangNhapSocial({
                provider: 'FACEBOOK',
                ...customPayload
            });
        }

        const fbAppId = import.meta.env.VITE_FACEBOOK_APP_ID;

        // Nếu có Facebook SDK và App ID
        if (fbAppId && window.FB) {
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
                            reject(new Error('Người dùng đã hủy đăng nhập Facebook'));
                        }
                    },
                    { scope: 'public_profile,email' }
                );
            });
        }

        // Fallback / Dev helper cho Facebook login
        return await dichVuXacThuc.dangNhapSocial({
            provider: 'FACEBOOK',
            email: 'facebook.customer@aerostride.vn',
            name: 'Khách Hàng Facebook',
            avatarUrl: 'https://graph.facebook.com/4/picture?type=large',
            providerId: 'fb_' + Date.now()
        });
    }
};
