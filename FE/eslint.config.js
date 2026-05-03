import pluginVue from 'eslint-plugin-vue';
import prettier from '@vue/eslint-config-prettier';

export default [
    ...pluginVue.configs['flat/essential'],
    {
        rules: {
            'comma-dangle': 'off',
            'no-unused-vars': 'warn',
        },
    },
    prettier,
];
