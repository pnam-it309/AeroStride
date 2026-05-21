package com.example.be.infrastructure.config;

import com.example.be.entity.AiKnowledge;
import com.example.be.entity.AiSynonym;
import com.example.be.repository.AiKnowledgeRepository;
import com.example.be.repository.AiSynonymRepository;
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

    private final AiKnowledgeRepository knowledgeRepository;
    private final AiSynonymRepository synonymRepository;

    @Override
    public void run(String... args) {
        if (knowledgeRepository.count() == 0) {
            log.info("Bootstrapping AI Knowledge Base...");
            
            List<AiKnowledge> initialKnowledge = Arrays.asList(
                AiKnowledge.builder()
                    .intent("GREETING")
                    .keywords("chào,hello,hi,xin chào")
                    .responseTemplate("Chào bạn! Tôi là trợ lý ảo của AeroStride. Tôi có thể giúp gì cho bạn hôm nay?")
                    .priority(10)
                    .build(),
                AiKnowledge.builder()
                    .intent("PRODUCT_LIST")
                    .keywords("sản phẩm,mẫu mới,giày mới,xem hàng")
                    .responseTemplate("Dưới đây là một số mẫu giày đang có sẵn tại cửa hàng:\n{products}\n\nBạn quan tâm đến mẫu nào ạ?")
                    .priority(5)
                    .build(),
                AiKnowledge.builder()
                    .intent("PRODUCT_SEARCH")
                    .keywords("tìm,mua,kiếm,giày")
                    .responseTemplate("Đây là các sản phẩm phù hợp với từ khóa '{search_term}' mà tôi tìm thấy:")
                    .priority(1)
                    .build(),
                AiKnowledge.builder()
                    .intent("THANK_YOU")
                    .keywords("cảm ơn,thanks,tks,cám ơn")
                    .responseTemplate("Dạ không có gì ạ! Chúc bạn chọn được đôi giày ưng ý.")
                    .priority(10)
                    .build(),
                AiKnowledge.builder()
                    .intent("STOCK_CHECK")
                    .keywords("còn hàng,hết hàng,còn không,số lượng")
                    .responseTemplate("Dạ, hiện tại shop vẫn còn hàng cho nhiều mẫu giày hot. Bạn có thể xem tình trạng tồn kho cụ thể ở danh sách sản phẩm bên dưới nha!")
                    .priority(8)
                    .build(),
                AiKnowledge.builder()
                    .intent("PRICE_INQURY")
                    .keywords("giá,nhiêu,tiền,giảm giá,khuyến mãi")
                    .responseTemplate("Giá các mẫu giày tại AeroStride cực kỳ cạnh tranh, chỉ từ 500.000 VNĐ. Đặc biệt đang có nhiều mẫu giảm giá sâu tới 50% đó ạ!")
                    .priority(8)
                    .build(),
                AiKnowledge.builder()
                    .intent("MATERIAL_INFO")
                    .keywords("chất liệu,làm bằng,da,vải,cao su")
                    .responseTemplate("Giày của AeroStride được làm từ các chất liệu cao cấp như da bò thật, vải Mesh thoáng khí và đế cao su non êm ái, giúp bạn thoải mái vận động cả ngày.")
                    .priority(8)
                    .build()
            );
            
            knowledgeRepository.saveAll(initialKnowledge);
        }

        if (synonymRepository.count() == 0) {
            log.info("Bootstrapping AI Word Library (Synonyms)...");
            
            List<AiSynonym> initialSynonyms = Arrays.asList(
                AiSynonym.builder().word("hi").canonicalWord("chào").build(),
                AiSynonym.builder().word("hello").canonicalWord("chào").build(),
                AiSynonym.builder().word("kiếm").canonicalWord("tìm").build(),
                AiSynonym.builder().word("mua").canonicalWord("tìm").build(),
                AiSynonym.builder().word("sneaker").canonicalWord("giày").build(),
                AiSynonym.builder().word("hàng").canonicalWord("sản phẩm").build(),
                AiSynonym.builder().word("nhiêu").canonicalWord("giá").build(),
                AiSynonym.builder().word("tiền").canonicalWord("giá").build(),
                AiSynonym.builder().word("sale").canonicalWord("giảm giá").build()
            );
            
            synonymRepository.saveAll(initialSynonyms);
        }
    }
}
