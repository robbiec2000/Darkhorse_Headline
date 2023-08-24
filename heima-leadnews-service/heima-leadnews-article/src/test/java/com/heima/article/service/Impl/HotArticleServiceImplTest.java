package com.heima.article.service.Impl;

import com.heima.article.ArticleApplication;
import com.heima.article.service.HotArticleService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@SpringBootTest(classes = ArticleApplication.class)
@RunWith(SpringRunner.class)
public class HotArticleServiceImplTest {

    @Autowired
    HotArticleService hotArticleService;

    @Test
    public void computeHotArticle(){
        hotArticleService.computeHotArticle();
    }

}