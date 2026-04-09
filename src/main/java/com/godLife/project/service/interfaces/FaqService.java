package com.godLife.project.service.interfaces;

import com.godLife.project.dto.model.content.FaQDTO;
import com.godLife.project.dto.query.search.SearchQueryDTO;
import com.godLife.project.dto.query.content.FaqListDTO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FaqService {
  List<FaqListDTO> selectAllFaq();
  FaQDTO getFaqById(Integer faqIdx);
  List<FaqListDTO> selectCateFaq(Integer faqCategory);
  int createFaq(FaQDTO faq);
  int updateFaq(FaQDTO faq);
  int deleteFaq(@Param("faqIdx") int faqIdx);
  List<FaQDTO> searchFaq(SearchQueryDTO searchQuery);

}
