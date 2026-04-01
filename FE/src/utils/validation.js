import { defineRule, configure } from 'vee-validate'
import { validationUtil } from './validationUtil'

/**
 * Register global validation rules for Vee-Validate.
 */
export const setupValidation = () => {
  // Standard rules
  defineRule('required', (value) => {
    return !validationUtil.isEmpty(value)
  })

  defineRule('email', (value) => {
    return !value || validationUtil.isEmail(value)
  })

  defineRule('min', (value, [min]) => {
    return !value || validationUtil.isMinLength(value, min)
  })

  defineRule('max', (value, [max]) => {
    return !value || validationUtil.isMaxLength(value, max)
  })

  defineRule('confirmed', (value, [target]) => {
    return value === target
  })

  // Custom configuration for error messages
  configure({
    generateMessage: (ctx) => {
      const messages = {
        required: 'Trường này là bắt buộc.',
        email: 'Email không hợp lệ.',
        min: `Phải có ít nhất ${ctx.rule.params[0]} ký tự.`,
        max: `Không được vượt quá ${ctx.rule.params[0]} ký tự.`,
        confirmed: 'Xác nhận mật khẩu chưa khớp.',
      }

      const message = messages[ctx.rule.name]
      return message ? message : `Trường ${ctx.field} không hợp lệ.`
    },
    validateOnInput: true, // Validate as the user types
  })
}

export default setupValidation
