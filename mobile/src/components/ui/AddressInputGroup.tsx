/**
 * AddressInputGroup - 3-level searchable Vietnamese Administrative Address Selector
 * (Tỉnh/Thành phố -> Quận/Huyện -> Phường/Xã + Địa chỉ chi tiết)
 */

import React, { useState, useEffect, useMemo } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  Pressable,
  Modal,
  FlatList,
  ActivityIndicator,
  SafeAreaView,
  Platform,
} from 'react-native';
import Ionicons from '@expo/vector-icons/Ionicons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import {
  locationService,
  normalizeSearchText,
  type LocationItem,
} from '@/services/locationService';

export interface AddressData {
  tinhThanh: string;
  tinhThanhCode?: number | string;
  quanHuyen: string;
  quanHuyenCode?: number | string;
  phuongXa: string;
  phuongXaCode?: number | string;
  diaChiChiTiet: string;
  fullAddress: string;
}

interface AddressInputGroupProps {
  initialTinhThanh?: string;
  initialQuanHuyen?: string;
  initialPhuongXa?: string;
  initialDiaChiChiTiet?: string;
  initialFullAddress?: string;
  onChange: (data: AddressData) => void;
  required?: boolean;
}

type ModalType = 'PROVINCE' | 'DISTRICT' | 'WARD' | null;

export const AddressInputGroup: React.FC<AddressInputGroupProps> = ({
  initialTinhThanh = '',
  initialQuanHuyen = '',
  initialPhuongXa = '',
  initialDiaChiChiTiet = '',
  initialFullAddress = '',
  onChange,
  required = true,
}) => {
  const theme = useTheme();

  const [tinhThanh, setTinhThanh] = useState<LocationItem | null>(null);
  const [quanHuyen, setQuanHuyen] = useState<LocationItem | null>(null);
  const [phuongXa, setPhuongXa] = useState<LocationItem | null>(null);
  const [diaChiChiTiet, setDiaChiChiTiet] = useState('');

  // Search Modal state
  const [activeModal, setActiveModal] = useState<ModalType>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [loadingList, setLoadingList] = useState(false);

  const [provinces, setProvinces] = useState<LocationItem[]>([]);
  const [districts, setDistricts] = useState<LocationItem[]>([]);
  const [wards, setWards] = useState<LocationItem[]>([]);

  // Load initial provinces on mount
  useEffect(() => {
    locationService.getProvinces().then((list) => {
      setProvinces(list);

      // Pre-select if initial values passed
      if (initialTinhThanh) {
        const found = list.find(
          (p) =>
            normalizeSearchText(p.name) === normalizeSearchText(initialTinhThanh) ||
            normalizeSearchText(p.name).includes(normalizeSearchText(initialTinhThanh)) ||
            normalizeSearchText(initialTinhThanh).includes(normalizeSearchText(p.name))
        );
        if (found) {
          setTinhThanh(found);
          // Load districts
          locationService.getDistricts(found.code).then((dList) => {
            setDistricts(dList);
            if (initialQuanHuyen) {
              const foundD = dList.find(
                (d) =>
                  normalizeSearchText(d.name) === normalizeSearchText(initialQuanHuyen) ||
                  normalizeSearchText(d.name).includes(normalizeSearchText(initialQuanHuyen)) ||
                  normalizeSearchText(initialQuanHuyen).includes(normalizeSearchText(d.name))
              );
              if (foundD) {
                setQuanHuyen(foundD);
                // Load wards
                locationService.getWards(foundD.code).then((wList) => {
                  setWards(wList);
                  if (initialPhuongXa) {
                    const foundW = wList.find(
                      (w) =>
                        normalizeSearchText(w.name) === normalizeSearchText(initialPhuongXa) ||
                        normalizeSearchText(w.name).includes(normalizeSearchText(initialPhuongXa)) ||
                        normalizeSearchText(initialPhuongXa).includes(normalizeSearchText(w.name))
                    );
                    if (foundW) {
                      setPhuongXa(foundW);
                    }
                  }
                });
              }
            }
          });
        }
      }
    });

    if (initialDiaChiChiTiet) {
      setDiaChiChiTiet(initialDiaChiChiTiet);
    } else if (initialFullAddress && !initialTinhThanh) {
      // Fallback: If only full address is given initially, set as detailed address
      setDiaChiChiTiet(initialFullAddress);
    }
  }, [initialTinhThanh, initialQuanHuyen, initialPhuongXa, initialDiaChiChiTiet, initialFullAddress]);

  // Emit changes whenever any component updates
  useEffect(() => {
    const pName = tinhThanh?.name || '';
    const dName = quanHuyen?.name || '';
    const wName = phuongXa?.name || '';
    const detail = diaChiChiTiet.trim();

    const parts = [detail, wName, dName, pName].filter(Boolean);
    const fullAddress = parts.join(', ');

    onChange({
      tinhThanh: pName,
      tinhThanhCode: tinhThanh?.code,
      quanHuyen: dName,
      quanHuyenCode: quanHuyen?.code,
      phuongXa: wName,
      phuongXaCode: phuongXa?.code,
      diaChiChiTiet: detail,
      fullAddress,
    });
  }, [tinhThanh, quanHuyen, phuongXa, diaChiChiTiet]);

  // Open modal handlers
  const handleOpenProvinces = async () => {
    setSearchQuery('');
    setActiveModal('PROVINCE');
    if (provinces.length === 0) {
      setLoadingList(true);
      const list = await locationService.getProvinces();
      setProvinces(list);
      setLoadingList(false);
    }
  };

  const handleOpenDistricts = async () => {
    if (!tinhThanh) return;
    setSearchQuery('');
    setActiveModal('DISTRICT');
    setLoadingList(true);
    const list = await locationService.getDistricts(tinhThanh.code);
    setDistricts(list);
    setLoadingList(false);
  };

  const handleOpenWards = async () => {
    if (!quanHuyen) return;
    setSearchQuery('');
    setActiveModal('WARD');
    setLoadingList(true);
    const list = await locationService.getWards(quanHuyen.code);
    setWards(list);
    setLoadingList(false);
  };

  const currentItems = useMemo(() => {
    let raw: LocationItem[] = [];
    if (activeModal === 'PROVINCE') raw = provinces;
    else if (activeModal === 'DISTRICT') raw = districts;
    else if (activeModal === 'WARD') raw = wards;

    if (!searchQuery.trim()) return raw;
    const q = normalizeSearchText(searchQuery);
    return raw.filter((item) => normalizeSearchText(item.name).includes(q));
  }, [activeModal, provinces, districts, wards, searchQuery]);

  const modalTitle = useMemo(() => {
    if (activeModal === 'PROVINCE') return 'Chọn Tỉnh / Thành phố';
    if (activeModal === 'DISTRICT') return 'Chọn Quận / Huyện';
    if (activeModal === 'WARD') return 'Chọn Phường / Xã';
    return '';
  }, [activeModal]);

  const selectedItemCode = useMemo(() => {
    if (activeModal === 'PROVINCE') return tinhThanh?.code;
    if (activeModal === 'DISTRICT') return quanHuyen?.code;
    if (activeModal === 'WARD') return phuongXa?.code;
    return null;
  }, [activeModal, tinhThanh, quanHuyen, phuongXa]);

  const handleSelectItem = (item: LocationItem) => {
    if (activeModal === 'PROVINCE') {
      if (tinhThanh?.code !== item.code) {
        setTinhThanh(item);
        setQuanHuyen(null);
        setPhuongXa(null);
        setDistricts([]);
        setWards([]);
      }
    } else if (activeModal === 'DISTRICT') {
      if (quanHuyen?.code !== item.code) {
        setQuanHuyen(item);
        setPhuongXa(null);
        setWards([]);
      }
    } else if (activeModal === 'WARD') {
      setPhuongXa(item);
    }
    setActiveModal(null);
  };

  return (
    <View style={styles.container}>
      {/* Tỉnh / Thành phố */}
      <View style={styles.fieldGroup}>
        <Text style={[styles.label, { color: theme.textSecondary }]}>
          Tỉnh / Thành phố {required && <Text style={{ color: Brand.error }}>*</Text>}
        </Text>
        <Pressable
          style={[
            styles.selectTrigger,
            {
              backgroundColor: theme.background,
              borderColor: theme.border,
            },
          ]}
          onPress={handleOpenProvinces}
        >
          <Ionicons name="business-outline" size={18} color={theme.textSecondary} style={styles.triggerIcon} />
          <Text
            style={[
              styles.selectText,
              { color: tinhThanh ? theme.text : theme.textTertiary },
            ]}
            numberOfLines={1}
          >
            {tinhThanh ? tinhThanh.name : 'Chọn Tỉnh / Thành phố'}
          </Text>
          <Ionicons name="chevron-down" size={16} color={theme.textTertiary} />
        </Pressable>
      </View>

      {/* Quận / Huyện */}
      <View style={styles.fieldGroup}>
        <Text style={[styles.label, { color: theme.textSecondary }]}>
          Quận / Huyện {required && <Text style={{ color: Brand.error }}>*</Text>}
        </Text>
        <Pressable
          style={[
            styles.selectTrigger,
            {
              backgroundColor: !tinhThanh ? theme.borderLight : theme.background,
              borderColor: theme.border,
              opacity: !tinhThanh ? 0.6 : 1,
            },
          ]}
          onPress={handleOpenDistricts}
          disabled={!tinhThanh}
        >
          <Ionicons name="map-outline" size={18} color={theme.textSecondary} style={styles.triggerIcon} />
          <Text
            style={[
              styles.selectText,
              { color: quanHuyen ? theme.text : theme.textTertiary },
            ]}
            numberOfLines={1}
          >
            {quanHuyen ? quanHuyen.name : tinhThanh ? 'Chọn Quận / Huyện' : 'Vui lòng chọn Tỉnh / Thành phố trước'}
          </Text>
          <Ionicons name="chevron-down" size={16} color={theme.textTertiary} />
        </Pressable>
      </View>

      {/* Phường / Xã */}
      <View style={styles.fieldGroup}>
        <Text style={[styles.label, { color: theme.textSecondary }]}>
          Phường / Xã {required && <Text style={{ color: Brand.error }}>*</Text>}
        </Text>
        <Pressable
          style={[
            styles.selectTrigger,
            {
              backgroundColor: !quanHuyen ? theme.borderLight : theme.background,
              borderColor: theme.border,
              opacity: !quanHuyen ? 0.6 : 1,
            },
          ]}
          onPress={handleOpenWards}
          disabled={!quanHuyen}
        >
          <Ionicons name="navigate-outline" size={18} color={theme.textSecondary} style={styles.triggerIcon} />
          <Text
            style={[
              styles.selectText,
              { color: phuongXa ? theme.text : theme.textTertiary },
            ]}
            numberOfLines={1}
          >
            {phuongXa ? phuongXa.name : quanHuyen ? 'Chọn Phường / Xã' : 'Vui lòng chọn Quận / Huyện trước'}
          </Text>
          <Ionicons name="chevron-down" size={16} color={theme.textTertiary} />
        </Pressable>
      </View>

      {/* Địa chỉ chi tiết */}
      <View style={styles.fieldGroup}>
        <Text style={[styles.label, { color: theme.textSecondary }]}>
          Địa chỉ chi tiết (Số nhà, tên đường...) {required && <Text style={{ color: Brand.error }}>*</Text>}
        </Text>
        <View
          style={[
            styles.inputContainer,
            {
              backgroundColor: theme.background,
              borderColor: theme.border,
            },
          ]}
        >
          <Ionicons name="home-outline" size={18} color={theme.textSecondary} style={styles.triggerIcon} />
          <TextInput
            style={[styles.input, { color: theme.text }]}
            value={diaChiChiTiet}
            onChangeText={setDiaChiChiTiet}
            placeholder="Ví dụ: Số 123 đường Cầu Giấy"
            placeholderTextColor={theme.textTertiary}
            multiline
            numberOfLines={2}
          />
        </View>
      </View>

      {/* Searchable Picker Modal */}
      <Modal
        visible={activeModal !== null}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={() => setActiveModal(null)}
      >
        <SafeAreaView style={[styles.modalRoot, { backgroundColor: theme.surface }]}>
          {/* Modal Header */}
          <View style={[styles.modalHeader, { borderBottomColor: theme.border }]}>
            <Text style={[styles.modalTitle, { color: theme.text }]}>{modalTitle}</Text>
            <Pressable
              style={styles.closeBtn}
              onPress={() => setActiveModal(null)}
              hitSlop={12}
            >
              <Ionicons name="close-circle" size={26} color={theme.textSecondary} />
            </Pressable>
          </View>

          {/* Search Bar */}
          <View style={[styles.searchBoxWrap, { backgroundColor: theme.surface }]}>
            <View style={[styles.searchBox, { backgroundColor: theme.background, borderColor: theme.border }]}>
              <Ionicons name="search-outline" size={18} color={theme.textSecondary} />
              <TextInput
                style={[styles.searchInput, { color: theme.text }]}
                placeholder={`Tìm kiếm ${modalTitle.toLowerCase()}...`}
                placeholderTextColor={theme.textTertiary}
                value={searchQuery}
                onChangeText={setSearchQuery}
                autoFocus={false}
                clearButtonMode="while-editing"
              />
              {searchQuery.length > 0 && Platform.OS !== 'ios' && (
                <Pressable onPress={() => setSearchQuery('')} hitSlop={8}>
                  <Ionicons name="close" size={18} color={theme.textTertiary} />
                </Pressable>
              )}
            </View>
          </View>

          {/* Items List */}
          {loadingList ? (
            <View style={styles.loadingBox}>
              <ActivityIndicator size="large" color={Brand.primary} />
              <Text style={[styles.loadingText, { color: theme.textSecondary }]}>Đang tải danh sách...</Text>
            </View>
          ) : currentItems.length === 0 ? (
            <View style={styles.emptyBox}>
              <Ionicons name="search" size={44} color={theme.textTertiary} />
              <Text style={[styles.emptyText, { color: theme.textSecondary }]}>
                Không tìm thấy kết quả phù hợp
              </Text>
              {searchQuery.trim().length > 0 && (
                <Pressable
                  style={[styles.customSelectBtn, { backgroundColor: Brand.primary }]}
                  onPress={() =>
                    handleSelectItem({
                      code: 'CUSTOM_' + Date.now(),
                      name: searchQuery.trim(),
                    })
                  }
                >
                  <Ionicons name="add-circle-outline" size={18} color="#FFFFFF" />
                  <Text style={styles.customSelectText}>
                    Sử dụng: &quot;{searchQuery.trim()}&quot;
                  </Text>
                </Pressable>
              )}
            </View>
          ) : (
            <FlatList
              data={currentItems}
              keyExtractor={(item) => String(item.code)}
              keyboardShouldPersistTaps="handled"
              ItemSeparatorComponent={() => (
                <View style={[styles.separator, { backgroundColor: theme.borderLight }]} />
              )}
              renderItem={({ item }) => {
                const isSelected = item.code === selectedItemCode;
                return (
                  <Pressable
                    style={({ pressed }) => [
                      styles.itemRow,
                      {
                        backgroundColor: isSelected
                          ? Brand.primary + '12'
                          : pressed
                          ? theme.borderLight
                          : 'transparent',
                      },
                    ]}
                    onPress={() => handleSelectItem(item)}
                  >
                    <Text
                      style={[
                        styles.itemText,
                        {
                          color: isSelected ? Brand.primary : theme.text,
                          fontWeight: isSelected ? '700' : '500',
                        },
                      ]}
                    >
                      {item.name}
                    </Text>
                    {isSelected && (
                      <Ionicons name="checkmark-circle" size={20} color={Brand.primary} />
                    )}
                  </Pressable>
                );
              }}
              contentContainerStyle={{ paddingBottom: 32 }}
            />
          )}
        </SafeAreaView>
      </Modal>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    gap: Spacing.two,
  },
  fieldGroup: {
    gap: 6,
  },
  label: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.semibold,
  },
  selectTrigger: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.two,
    paddingVertical: 12,
    minHeight: 46,
  },
  triggerIcon: {
    marginRight: 8,
  },
  selectText: {
    flex: 1,
    fontSize: FontSizes.base,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.two,
    paddingVertical: 10,
    minHeight: 60,
  },
  input: {
    flex: 1,
    fontSize: FontSizes.base,
    paddingTop: 0,
    paddingBottom: 0,
  },
  modalRoot: {
    flex: 1,
  },
  modalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 4,
    borderBottomWidth: 1,
  },
  modalTitle: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.bold,
  },
  closeBtn: {
    padding: 4,
  },
  searchBoxWrap: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
  },
  searchBox: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.two + 2,
    paddingVertical: Platform.OS === 'ios' ? 8 : 4,
    gap: 8,
  },
  searchInput: {
    flex: 1,
    fontSize: FontSizes.base,
    paddingVertical: 4,
  },
  itemRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 4,
  },
  itemText: {
    fontSize: FontSizes.base,
    flex: 1,
  },
  separator: {
    height: 1,
    marginLeft: Spacing.three,
  },
  loadingBox: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.two,
  },
  loadingText: {
    fontSize: FontSizes.base,
  },
  emptyBox: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.two,
    paddingHorizontal: Spacing.four,
  },
  emptyText: {
    fontSize: FontSizes.base,
    textAlign: 'center',
  },
  customSelectBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.three,
    paddingVertical: 10,
    borderRadius: BorderRadius.md,
    gap: 8,
    marginTop: Spacing.two,
  },
  customSelectText: {
    color: '#FFFFFF',
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
  },
});
