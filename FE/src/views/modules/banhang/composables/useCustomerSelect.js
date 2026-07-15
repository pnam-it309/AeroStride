import { ref, watch } from 'vue';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';
import { dichVuKhachHang } from '@/services/admin/dichVuKhachHang';
import api from '@/services/apiService';
import { API_ADMIN } from '@/constants/apiPaths';

export function useCustomerSelect(selectedOrder, updateOrderInList, refreshBestVoucher, addNotification) {
    const customerSearch = ref('');
    const customerResults = ref([]);
    const customerLoading = ref(false);
    const showCustomerSuggestions = ref(false);

    const customerForm = ref({
        ten: '',
        sdt: '',
        email: '',
        gioiTinh: 'Giới tính',
        ngaySinh: '',
        tongDonHang: 0
    });

    const searchCustomers = async () => {
        const kw = customerSearch.value?.trim();
        if (!kw || kw.length < 2) {
            customerResults.value = [];
            return;
        }
        customerLoading.value = true;
        try {
            const data = await dichVuDonHang.searchKhachHang(kw);
            customerResults.value = data || [];
        } catch (e) {
            console.error(e);
        } finally {
            customerLoading.value = false;
        }
    };

    let customerDebounce = null;
    watch(customerSearch, () => {
        if (customerDebounce) clearTimeout(customerDebounce);
        customerDebounce = setTimeout(() => {
            searchCustomers();
        }, 300);
    });

    const selectCustomer = async (customer) => {
        if (!customer?.id || !selectedOrder.value?.id) return;
        try {
            const updated = await dichVuDonHang.setKhachHang(selectedOrder.value.id, customer.id);
            updateOrderInList(updated);
            refreshBestVoucher(updated);
            customerSearch.value = '';
            customerResults.value = [];
        } catch (e) {
            addNotification({ title: 'Lỗi khách hàng', subtitle: 'Không thể gắn khách hàng vào hóa đơn.', color: 'error' });
        }
    };

    const onCustomerInput = () => {
        const searchVal = (customerForm.value.ten || '') + ' ' + (customerForm.value.sdt || '');
        customerSearch.value = searchVal.trim();
        showCustomerSuggestions.value = true;
    };

    const onSelectSuggestedCustomer = async (c) => {
        showCustomerSuggestions.value = false;
        await selectCustomer(c);
    };

    const onCustomerFormUpdate = (form) => {
        customerForm.value = { ...customerForm.value, ...(form || {}) };
    };

    const onRemoveCustomer = async () => {
        if (!selectedOrder.value?.id) return;
        try {
            const updated = await dichVuDonHang.setKhachHang(selectedOrder.value.id, null);
            updateOrderInList(updated);
            refreshBestVoucher(updated);
            addNotification({ title: 'Thành công.', subtitle: 'Đã gỡ khách hàng khỏi hóa đơn.', color: 'success' });
        } catch (e) {
            addNotification({ title: 'Không thể gỡ khách hàng.', subtitle: 'Có lỗi xảy ra khi gỡ khách hàng.', color: 'error' });
        }
    };

    const findExistingCustomerByContact = async (phone, email) => {
        try {
            const kw = phone || email || '';
            if (!kw) return null;
            const res = await dichVuDonHang.searchKhachHang(kw);
            const foundList = res || [];
            
            const normalize = (str) => String(str || '').trim().toLowerCase();
            const pNorm = normalize(phone);
            const eNorm = normalize(email);

            return foundList.find(c => {
                const cPhone = normalize(c.sdt);
                const cEmail = normalize(c.email);
                if (pNorm && cPhone === pNorm) return true;
                if (eNorm && cEmail === eNorm) return true;
                return false;
            }) || null;
        } catch (e) {
            console.error('Error finding existing customer', e);
            return null;
        }
    };

    const ensureCustomerAndGetId = async () => {
        if (selectedOrder.value?.idKhachHang) {
            return selectedOrder.value.idKhachHang;
        }

        const ten = customerForm.value.ten?.trim();
        const sdt = customerForm.value.sdt?.trim();
        const email = customerForm.value.email?.trim() || null;
        const ns = customerForm.value.ngaySinh?.trim() || null;
        const gt = customerForm.value.gioiTinh === 'Nam' ? true : customerForm.value.gioiTinh === 'Nữ' ? false : null;

        if (!ten && !sdt) {
            return null;
        }

        if (!ten || !sdt) {
            throw new Error('Vui lòng điền đầy đủ Tên khách hàng và SĐT để thêm khách hàng mới.');
        }

        const existed = await findExistingCustomerByContact(sdt, email);
        if (existed) {
            await selectCustomer(existed);
            return existed.id;
        }

        const newCustomerPayload = {
            ten,
            sdt,
            email,
            gioiTinh: gt,
            ngaySinh: ns
        };

        const created = await dichVuKhachHang.taoKhachHang(newCustomerPayload);
        if (created?.id) {
            await selectCustomer(created);
            return created.id;
        }
        return null;
    };

    return {
        customerSearch,
        customerResults,
        customerLoading,
        showCustomerSuggestions,
        customerForm,
        searchCustomers,
        selectCustomer,
        onCustomerInput,
        onSelectSuggestedCustomer,
        onCustomerFormUpdate,
        ensureCustomerAndGetId,
        onRemoveCustomer
    };
}
