package com.test.repository.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.test.entity.ArticleEntity;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/7/23 17:06
 */
public interface EsRepository extends ElasticsearchRepository<ArticleEntity,String> {
}
