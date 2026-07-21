import { createVuetify } from 'vuetify';
import { vi } from 'vuetify/locale';
import '@mdi/font/css/materialdesignicons.css';
import { BLUE_THEME } from '@/theme/LightTheme';

export default createVuetify({
    locale: {
        locale: 'vi',
        messages: { vi }
    },
    theme: {
        defaultTheme: 'BLUE_THEME',
        themes: {
            BLUE_THEME
        }
    },
    defaults: {
        VCard: {
            rounded: 'xl'
        },
        VTextField: {
            variant: 'outlined',
            density: 'comfortable',
            color: 'primary'
        },
        VTextarea: {
            variant: 'outlined',
            density: 'comfortable',
            color: 'primary'
        },
        VSelect: {
            variant: 'outlined',
            density: 'comfortable',
            color: 'primary'
        },
        VListItem: {
            minHeight: '45px'
        },
        VTooltip: {
            location: 'top'
        }
    }
});
