package com.test.service;

import com.test.entity.ArticleEntity;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/7/23 17:05
 */
public interface ElasticsearchService {

    ArticleEntity getById(String id);

    ArticleEntity save(ArticleEntity articleEntity);

}
