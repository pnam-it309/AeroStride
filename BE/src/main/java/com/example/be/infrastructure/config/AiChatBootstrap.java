package com.example.be.infrastructure.config;

import com.example.be.entity.KienThucAi;
import com.example.be.entity.TuDongNghiaAi;
import com.example.be.repository.KienThucAiRepository;
import com.example.be.repository.TuDongNghiaAiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiChatBootstrap implements CommandLineRunner {

    private final KienThucAiRepository knowledgeRepository;
    private final TuDongNghiaAiRepository synonymRepository;

    @Override
    public void run(String... args) {
        if (knowledgeRepository.count() == 0) {
            log.info("Bootstrapping AI Knowledge Base...");
            
            List<KienThucAi> initialKnowledge = Arrays.asList(
                KienThucAi.builder()
                    .mucDich("GREETING")
                    .tuKhoa("chào,hello,hi,xin chào")
                    .mauCauTraLoi("Chào bạn! Tôi là trợ lý ảo của AeroStride. Tôi có thể giúp gì cho bạn hôm nay?")
                    .doUuTien(10)
                    .build(),
                KienThucAi.builder()
                    .mucDich("PRODUCT_LIST")
                    .tuKhoa("sản phẩm,mẫu mới,giày mới,xem hàng")
                    .mauCauTraLoi("Dưới đây là một số mẫu giày đang có sẵn tại cửa hàng:\n{products}\n\nBạn quan tâm đến mẫu nào ạ?")
                    .doUuTien(5)
                    .build(),
                KienThucAi.builder()
                    .mucDich("PRODUCT_SEARCH")
                    .tuKhoa("tìm,mua,kiếm,giày")
                    .mauCauTraLoi("Đây là các sản phẩm phù hợp với từ khóa '{search_term}' mà tôi tìm thấy:")
                    .doUuTien(1)
                    .build(),
                KienThucAi.builder()
                    .mucDich("THANK_YOU")
                    .tuKhoa("cảm ơn,thanks,tks,cám ơn")
                    .mauCauTraLoi("Dạ không có gì ạ! Chúc bạn chọn được đôi giày ưng ý.")
                    .doUuTien(10)
                    .build(),
                KienThucAi.builder()
                    .mucDich("STOCK_CHECK")
                    .tuKhoa("còn hàng,hết hàng,còn không,số lượng")
                    .mauCauTraLoi("Dạ, hiện tại shop vẫn còn hàng cho nhiều mẫu giày hot. Bạn có thể xem tình trạng tồn kho cụ thể ở danh sách sản phẩm bên dưới nha!")
                    .doUuTien(8)
                    .build(),
                KienThucAi.builder()
                    .mucDich("PRICE_INQURY")
                    .tuKhoa("giá,nhiêu,tiền,giảm giá,khuyến mãi")
                    .mauCauTraLoi("Giá các mẫu giày tại AeroStride cực kỳ cạnh tranh, chỉ từ 500.000 VNĐ. Đặc biệt đang có nhiều mẫu giảm giá sâu tới 50% đó ạ!")
                    .doUuTien(8)
                    .build(),
                KienThucAi.builder()
                    .mucDich("MATERIAL_INFO")
                    .tuKhoa("chất liệu,làm bằng,da,vải,cao su")
                    .mauCauTraLoi("Giày của AeroStride được làm từ các chất liệu cao cấp như da bò thật, vải Mesh thoáng khí và đế cao su non êm ái, giúp bạn thoải mái vận động cả ngày.")
                    .doUuTien(8)
                    .build()
            );
            
            knowledgeRepository.saveAll(initialKnowledge);
        }

        if (synonymRepository.count() == 0) {
            log.info("Bootstrapping AI Word Library (Synonyms)...");
            
            List<TuDongNghiaAi> initialSynonyms = Arrays.asList(
                TuDongNghiaAi.builder().tuGoc("hi").tuChuanHoa("chào").build(),
                TuDongNghiaAi.builder().tuGoc("hello").tuChuanHoa("chào").build(),
                TuDongNghiaAi.builder().tuGoc("kiếm").tuChuanHoa("tìm").build(),
                TuDongNghiaAi.builder().tuGoc("mua").tuChuanHoa("tìm").build(),
                TuDongNghiaAi.builder().tuGoc("sneaker").tuChuanHoa("giày").build(),
                TuDongNghiaAi.builder().tuGoc("hàng").tuChuanHoa("sản phẩm").build(),
                TuDongNghiaAi.builder().tuGoc("nhiêu").tuChuanHoa("giá").build(),
                TuDongNghiaAi.builder().tuGoc("tiền").tuChuanHoa("giá").build(),
                TuDongNghiaAi.builder().tuGoc("sale").tuChuanHoa("giảm giá").build()
            );
            
            synonymRepository.saveAll(initialSynonyms);
        }
    }
}
