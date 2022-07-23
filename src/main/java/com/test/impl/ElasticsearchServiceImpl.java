package com.test.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.test.entity.ArticleEntity;
import com.test.repository.EsRepository;
import com.test.service.ElasticsearchService;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/7/23 17:13
 */
@Component
public class ElasticsearchServiceImpl implements ElasticsearchService {

    @Autowired
    private EsRepository esRepository;

    @Override
    public ArticleEntity getById(String id) {
        Optional<ArticleEntity> articleEntity = esRepository.findById(id);
        return articleEntity.get();
    }

    @Override
    public ArticleEntity save(ArticleEntity articleEntity) {
        return esRepository.save(articleEntity);
    }

}
