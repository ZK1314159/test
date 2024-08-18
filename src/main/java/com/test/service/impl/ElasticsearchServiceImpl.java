package com.test.service.impl;

import com.test.entity.ArticleEntity;
import com.test.service.interfaces.ElasticsearchService;
import org.springframework.stereotype.Component;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/7/23 17:13
 */
@Component
public class ElasticsearchServiceImpl implements ElasticsearchService {

//    @Autowired
//    private EsRepository esRepository;

    @Override
    public ArticleEntity getById(String id) {
        return null;
//        Optional<ArticleEntity> articleEntity = esRepository.findById(id);
//        return articleEntity.get();
    }

    @Override
    public ArticleEntity save(ArticleEntity articleEntity) {
        return null;
//        return esRepository.save(articleEntity);
    }

}
