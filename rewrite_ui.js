const fs = require('fs');

const file = 'FE/src/views/modules/banhang/BanHang.vue';
const content = fs.readFileSync(file, 'utf8');

// 1. Extract the Khách Hàng Card
const khachHangStart = content.indexOf('<!-- Khách hàng Card -->');
const khachHangEndIndex = content.indexOf('<!-- Nhận hàng Card -->', khachHangStart);
if(khachHangStart === -1 || khachHangEndIndex === -1) {
    console.error('Cannot find Khach Hang Card');
    process.exit(1);
}
const khachHangHtml = content.substring(khachHangStart, khachHangEndIndex).trim();

// 2. Build the new Left Column bottom and Right Column
const newLayout = `
                            <!-- Notes Card -->
                            <v-card class="pos-card pa-4 mt-4 rounded-lg border flex-grow-1 d-flex flex-column"
                                style="min-height: 140px">
                                <div class="text-slate-800 mb-2 font-weight-bold" style="font-size: 14px !important">Ghi chú</div>
                                <v-textarea v-model="checkoutData.note"
                                    placeholder="Viết ghi chú hoặc /shortcut để ghi chú nhanh" variant="outlined"
                                    rows="2" hide-details class="flex-grow-1 note-textarea text-body-2 bg-slate-50" />
                            </v-card>
                        </v-col>

                        <!-- Right Column (4 cols out of 12) -->
                        <v-col cols="12" lg="4" class="d-flex flex-column gap-4 pl-lg-2 mt-4 mt-lg-0">
                            ${khachHangHtml}

                            <!-- Thông tin đơn hàng Card -->
                            <v-card class="pos-card pa-4 rounded-lg border">
                                <!-- Auto Apply Voucher -->
                                <div v-if="bestVoucher" class="bg-green-50 border border-green-lighten-3 rounded-lg pa-3 mb-4 position-relative">
                                    <div class="d-flex justify-space-between align-center mb-1">
                                        <div class="d-flex align-center gap-2">
                                            <v-icon color="green-darken-2" size="20">mdi-check-circle-outline</v-icon>
                                            <span class="text-green-darken-3 font-weight-bold" style="font-size: 13px !important;">Đang áp dụng voucher tốt nhất</span>
                                        </div>
                                        <v-btn icon="mdi-close" variant="text" size="x-small" color="green-darken-2" @click="removeBestVoucher" style="height: 24px; width: 24px; margin-right: -4px;"></v-btn>
                                    </div>
                                    <div class="font-weight-bold text-slate-800 ml-7" style="font-size: 14px !important;">{{ bestVoucher.ma }}</div>
                                    <div class="text-green-darken-3 ml-7 mt-1" style="font-size: 13px !important;">Giá trị giảm: <span class="float-right font-weight-bold">-{{ formatCurrency(discountAmount) }}</span></div>
                                </div>

                                <div class="d-flex justify-space-between align-center border-b pb-2 mb-3">
                                    <span class="font-weight-bold text-slate-800" style="font-size: 14px !important">Thông tin đơn hàng</span>
                                    <div class="d-flex align-center">
                                        <span class="text-slate-600 mr-2" style="font-size: 13px !important">Giao hàng</span>
                                        <v-switch v-model="isGiaoHang" color="primary" hide-details density="compact" inset style="margin-top: -4px; height: 32px;"></v-switch>
                                    </div>
                                </div>

                                <div class="d-flex flex-column gap-2 mb-3 text-body-2">
                                    <div class="d-flex justify-space-between align-center">
                                        <span class="text-slate-600">Tổng tiền hàng:</span>
                                        <span class="font-weight-bold text-slate-800">{{ formatCurrency(grossTotalAmount) }}</span>
                                    </div>
                                    <div v-if="isGiaoHang" class="d-flex justify-space-between align-center">
                                        <span class="text-slate-600">Phí vận chuyển:</span>
                                        <span class="font-weight-bold text-slate-800">{{ formatCurrency(shippingFee) }}</span>
                                    </div>
                                    <div v-if="discountAmount > 0" class="d-flex justify-space-between align-center text-green-darken-3">
                                        <span>Tiền giảm:</span>
                                        <span class="font-weight-bold">-{{ formatCurrency(discountAmount) }}</span>
                                    </div>
                                </div>

                                <!-- Gợi ý mua thêm -->
                                <div v-if="suggestedVoucher && !bestVoucher" class="border border-orange-lighten-3 bg-orange-50 rounded-lg pa-3 mb-3">
                                    <div class="d-flex justify-space-between align-center mb-2">
                                        <span class="text-orange-darken-3 font-weight-bold" style="font-size: 13px !important;">Gợi ý mua thêm</span>
                                        <v-chip color="orange-darken-3" variant="flat" size="x-small" class="font-weight-bold px-2">1 đề xuất</v-chip>
                                    </div>
                                    <div class="d-flex align-center gap-2 mb-2">
                                        <v-chip color="green-lighten-4" class="text-green-darken-4 font-weight-bold" size="small">{{ suggestedVoucher.giamGia }}%</v-chip>
                                        <span class="font-weight-bold text-slate-800">{{ suggestedVoucher.ma }}</span>
                                    </div>
                                    <div class="d-flex justify-space-between text-body-2">
                                        <span class="text-slate-600">Cần mua thêm:</span>
                                        <span class="font-weight-bold text-slate-800">{{ formatCurrency(suggestedVoucher.missingAmount) }}</span>
                                    </div>
                                    <div class="d-flex justify-space-between text-body-2">
                                        <span class="text-slate-600">Sẽ được giảm:</span>
                                        <span class="font-weight-bold text-green-darken-3">{{ formatCurrency(suggestedVoucher.estimatedDiscount) }}</span>
                                    </div>
                                </div>

                                <!-- Nhận hàng (If Giao hàng) -->
                                <div v-if="isGiaoHang" class="mb-4 pt-3 border-t">
                                    <div class="font-weight-bold text-slate-800 mb-3" style="font-size: 13px !important">Địa chỉ nhận hàng</div>
                                    <div class="d-flex flex-column gap-2">
                                        <v-text-field v-model="recipientName" placeholder="Tên người nhận" variant="outlined" density="compact" hide-details class="dim-input-field" />
                                        <v-text-field v-model="recipientPhone" placeholder="Số điện thoại" variant="outlined" density="compact" hide-details class="dim-input-field" />
                                        <v-text-field v-model="recipientAddressDetail" placeholder="Địa chỉ chi tiết" variant="outlined" density="compact" hide-details class="dim-input-field" />
                                        <v-autocomplete v-model="recipientProvince" :items="provincesShip" item-title="name" item-value="code" placeholder="Tỉnh/Thành phố" density="compact" variant="outlined" hide-details class="dim-select-field" no-data-text="Không có dữ liệu"/>
                                        <v-autocomplete v-model="recipientDistrict" :items="districtsShip" item-title="name" item-value="code" placeholder="Quận/Huyện" density="compact" variant="outlined" hide-details :disabled="!recipientProvince" class="dim-select-field" no-data-text="Không có dữ liệu"/>
                                        <v-autocomplete v-model="recipientWard" :items="wardsShip" item-title="name" item-value="code" placeholder="Phường/Xã" density="compact" variant="outlined" hide-details :disabled="!recipientDistrict" class="dim-select-field" no-data-text="Không có dữ liệu"/>
                                    </div>
                                </div>

                                <v-divider class="my-3"></v-divider>

                                <div class="d-flex justify-space-between align-center mb-4">
                                    <span class="text-slate-800 font-weight-bold">Khách cần trả</span>
                                    <span class="font-weight-bold text-error" style="font-size: 18px !important;">{{ formatCurrency(finalCollectAmount) }}</span>
                                </div>

                                <div class="mb-3">
                                    <div class="text-slate-600 mb-1" style="font-size: 13px !important">Hình thức thanh toán</div>
                                    <v-radio-group v-model="checkoutData.paymentMethod" inline hide-details class="custom-radio-group">
                                        <v-radio label="Tiền mặt" value="CASH" color="error"></v-radio>
                                        <v-radio label="Chuyển khoản" value="VNPAY" color="primary"></v-radio>
                                    </v-radio-group>
                                </div>

                                <div class="mb-4" v-if="checkoutData.paymentMethod === 'CASH'">
                                    <div class="text-slate-600 mb-1" style="font-size: 13px !important">Khách thanh toán</div>
                                    <v-text-field :model-value="formatNumberWithDots(checkoutData.receivedAmount || 0)"
                                        @input="e => { checkoutData.receivedAmount = parseNumberFromDots(e.target.value); e.target.value = formatNumberWithDots(checkoutData.receivedAmount); }"
                                        @keypress="e => { if (!/^[0-9]$/.test(e.key)) e.preventDefault() }"
                                        placeholder="Nhập số tiền khách đưa" variant="outlined" density="compact" hide-details class="dim-input-field font-weight-bold text-right-input" suffix="đ" />
                                    <div v-if="changeAmount > 0" class="d-flex justify-space-between align-center mt-2 text-body-2 text-blue-darken-2">
                                        <span>Tiền thừa trả khách:</span>
                                        <span class="font-weight-bold">{{ formatCurrency(changeAmount) }}</span>
                                    </div>
                                </div>

                                <!-- Action Buttons -->
                                <div class="d-flex gap-3 w-100 mt-2">
                                    <v-btn color="#88c057" height="48" class="font-weight-bold rounded-lg shadow-md px-4 flex-grow-1" style="font-size: 14px !important; color: #ffffff !important;" :disabled="!selectedOrder?.listsHoaDonChiTiet?.length" @click="onPrintInvoice" elevation="0">
                                        <v-icon class="mr-1">mdi-printer</v-icon> IN
                                    </v-btn>
                                    <v-btn color="#4285F4" height="48" class="font-weight-bold rounded-lg btn-checkout shadow-md text-white px-4 flex-grow-1" style="font-size: 14px !important;" :loading="isProcessing" :disabled="!selectedOrder?.listsHoaDonChiTiet?.length" @click="onCheckout" elevation="0">
                                        THANH TOÁN
                                    </v-btn>
                                </div>
                            </v-card>
                        </v-col>
                    </v-row>
`;

const startMarker = '<!-- Row of Price & Payment cards -->';
const endMarker = '<!-- Loading Spinner -->';

const startIndex = content.indexOf(startMarker);
const endIndex = content.indexOf(endMarker);

let endOfRowIndex = content.lastIndexOf('</v-row>', endIndex) + 8;

if (startIndex !== -1 && endOfRowIndex !== -1) {
    const newContent = content.substring(0, startIndex) + newLayout + '\n            ' + content.substring(endIndex);
    fs.writeFileSync(file, newContent, 'utf8');
    console.log('Successfully replaced layout!');
} else {
    console.error('Could not find markers');
}
