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
                    .mauCauTraLoi("Dạ em chào anh/chị ạ! Em là trợ lý tư vấn của AeroStride. Em có thể giúp anh/chị tìm mẫu giày ưng ý, tư vấn chọn size hoặc giải đáp các thắc mắc về đơn hàng hôm nay ạ!")
                    .doUuTien(10)
                    .build(),
                KienThucAi.builder()
                    .mucDich("THANK_YOU")
                    .tuKhoa("cảm ơn,thanks,tks,cám ơn,thank you")
                    .mauCauTraLoi("Dạ không có gì ạ! Chúc anh/chị một ngày thật vui vẻ và chọn được đôi giày ưng ý nhất tại AeroStride nhé.")
                    .doUuTien(10)
                    .build(),
                KienThucAi.builder()
                    .mucDich("STOCK_CHECK")
                    .tuKhoa("còn hàng,hết hàng,còn không,số lượng,còn size")
                    .mauCauTraLoi("Dạ, hiện tại shop vẫn còn sẵn nhiều mẫu giày và đủ size tại kho. Anh/chị đang quan tâm đến mẫu giày hay size nào để em kiểm tra tồn kho chi tiết giúp mình nhé!")
                    .doUuTien(8)
                    .build(),
                KienThucAi.builder()
                    .mucDich("PRICE_INQUIRY")
                    .tuKhoa("khoảng giá,bảng giá")
                    .mauCauTraLoi("Giá các mẫu giày tại AeroStride cực kỳ ưu đãi, dao động từ vài trăm nghìn đến hơn 2 triệu tùy dòng sản phẩm. Bạn muốn tìm giày trong tầm giá bao nhiêu để shop gợi ý phù hợp nhất nhé?")
                    .doUuTien(8)
                    .build(),
                KienThucAi.builder()
                    .mucDich("MATERIAL_INFO")
                    .tuKhoa("chất liệu,làm bằng,da thật,vải mesh,đế cao su")
                    .mauCauTraLoi("Giày tại AeroStride được sản xuất từ các chất liệu cao cấp như da bò tự nhiên, da tổng hợp cao cấp, vải Mesh thoáng khí và đế cao su non chống trượt êm ái, đảm bảo độ bền và thoải mái tối đa khi vận động.")
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
