package com.heima.article.stream;

import com.alibaba.fastjson.JSON;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.mess.UpdateArticleMess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class HotArticleStreamHandler {

    @Bean
    public KStream<String, String> kStream(StreamsBuilder streamsBuilder){
        //接受消息
        KStream<String, String> stream = streamsBuilder.stream(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC);
        //聚会流式消息
        stream.map((k, v)->{
            UpdateArticleMess msg = JSON.parseObject(v, UpdateArticleMess.class);
            //重置key:文章id value:行为次数
            return new KeyValue<>(msg.getArticleId().toString(), msg.getType().name()+":"+msg.getAdd());
        })
                //按照文章id进行聚合
                .groupBy((k, v)->k)
                //时间窗口, 每10秒聚合一次
                .windowedBy(TimeWindows.of(Duration.ofSeconds(10)))
                //自定义聚合计算
                .aggregate(new Initializer<String>() {
                    /**
                     * 初始方法, 返回值是消息value
                     */
                    @Override
                    public String apply() {
                        Map<String, Integer> map = new HashMap<>();
                        map.put("COLLECTION", 0);
                        map.put("COMMENT", 0);
                        map.put("LIKES", 0);
                        map.put("VIEWS", 0);
                        return JSON.toJSONString(map);
                    }
                }, new Aggregator<String, String, String>() {
                    /**
                     * 聚合操作, 返回值是消息value
                     */
                    @Override
                    public String apply(String k, String v, String aggValue) {
                        if(StringUtils.isBlank(v)){
                            return aggValue;
                        }
                        Map<String, Integer> map = JSON.parseObject(aggValue, Map.class);
                        String[] valAry = v.split(":");
                        Integer count = map.get(valAry[0]);
                        map.put(valAry[0], count + Integer.parseInt(valAry[1]));
                        System.out.println("文章的id:"+k);
                        System.out.println("当前时间窗口内的消息处理结果："+JSON.toJSONString(map));

                        return JSON.toJSONString(map);
                    }
                }, Materialized.as("hot-article-stream-1"))
                .toStream()
                .map((k, v)->{
                    return new KeyValue<>(k.key().toString(), formatObj(k.key().toString(), v));
                })
                //发送消息
                .to(HotArticleConstants.HOT_ARTICLE_INCR_HANDLE_TOPIC);

        return stream;

    }

    /**
     * 格式化消息的value数据
     * @param articleId
     * @param value
     * @return
     */
    public String formatObj(String articleId,String value){
        Map<String, Integer> map = JSON.parseObject(value, Map.class);
        ArticleVisitStreamMess msg = new ArticleVisitStreamMess();
        msg.setArticleId(Long.valueOf(articleId));
        msg.setCollect(map.get("COLLECTION"));
        msg.setComment(map.get("COMMENT"));
        msg.setView(map.get("VIEWS"));
        msg.setLike(map.get("LIKES"));
        log.info("聚合消息处理之后的结果为:{}",JSON.toJSONString(msg));
        return JSON.toJSONString(msg);
    }
}
