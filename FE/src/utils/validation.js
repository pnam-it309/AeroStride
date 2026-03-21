import { validationUtil } from './validationUtil';

/**
 * Register global validation rules for Vee-Validate.
 */
export const setupValidation = () => {
    // Standard rules
    defineRule('required', (value) => {
        if (validationUtil.isEmpty(value)) return 'Trường này là bắt buộc.';
        return true;
    });

    defineRule('email', (value) => {
        if (!validationUtil.isEmail(value)) return 'Email không hợp lệ.';
        return true;
    });

    defineRule('min', (value, [min]) => {
        if (!validationUtil.isMinLength(value, min)) return `Phải có ít nhất ${min} ký tự.`;
        return true;
    });

    defineRule('max', (value, [max]) => {
        if (!validationUtil.isMaxLength(value, max)) return `Không được vượt quá ${max} ký tự.`;
        return true;
    });

    defineRule('confirmed', (value, [target]) => {
        if (value !== target) return 'Xác nhận mật khẩu chưa khớp.';
        return true;
    });


    // Custom configuration
    configure({
        generateMessage: localize({
            vi: {
                messages: {
                    required: 'Trường này là bắt buộc.',
                    email: 'Email không hợp lệ.',
                    min: 'Phải có ít nhất 0:{min} ký tự.',
                    max: 'Không được vượt quá 0:{max} ký tự.',
                    confirmed: 'Xác nhận mật khẩu chưa khớp.'
                }
            }
        }),
        validateOnInput: true, // Validate as the user types
    });
};

export default setupValidation;
