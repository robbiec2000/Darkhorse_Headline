package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.nntp.Article;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {

    @Autowired
    private WmNewsMapper wmNewsMapper;

    @Override
    //@Async
    public void autoScanWmNews(Integer id) {
        //查询自媒体文章
        log.info(String.valueOf(id));
        WmNews wmNews = wmNewsMapper.selectOne(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getId, id));
        if(wmNews == null){
            throw new RuntimeException("WmNewsAutoScanServiceImpl-文章不存在");
        }


        if(wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())){
            Map<String, Object> textAndImage = extractTextAndImage(wmNews);
            //自管理敏感词过滤
            boolean isSensitive = handleSensitiveScan((String) textAndImage.get("content"), wmNews);
            if(isSensitive)return;
            //审核文本内容
            /*boolean isTextValid = handleTextScan((String) textAndImage.get("content"), wmNews);
            if(!isTextValid)return;
            //审核图片内容
            boolean isImageValid = handleImageScan((List<String>) textAndImage.get("images"), wmNews);
            if(!isImageValid)return;*/
            //审核成功, 保存app端相关数据
            ResponseResult res = saveAppArticle(wmNews);

            if(!res.getCode().equals(200)){
                throw new RuntimeException("WmNewsAutoScanServiceImpl-文章审核, 保存app端文章失败");
            }
            wmNews.setArticleId((Long) res.getData());
            updateWmNews(wmNews, WmNews.Status.PUBLISHED.getCode(), "审核成功");
        }

    }


    @Autowired
    WmSensitiveMapper wmSensitiveMapper;
    /**
     * 自定义敏感词审核
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleSensitiveScan(String content, WmNews wmNews) {
        //获取所有敏感词
        List<WmSensitive> wmSensitives = wmSensitiveMapper.selectList(Wrappers.<WmSensitive>lambdaQuery().select(WmSensitive::getSensitives));
        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());
        //初始敏感词库
        SensitiveWordUtil.initMap(sensitiveList);
        //检查文章是否包含敏感词
        Map<String, Integer> map = SensitiveWordUtil.matchWords(content);
        if(map.size() > 0){
            updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "当前文章中存在违规内容" + map);
            return true;
        }
        return false;
    }


    @Autowired
    private IArticleClient articleClient;
    @Autowired
    private WmChannelMapper wmChannelMapper;
    @Autowired
    private WmUserMapper wmUserMapper;
    /**
     * 保存app端文章数据
     * @param wmNews
     */
    @Override
    public ResponseResult saveAppArticle(WmNews wmNews) {
        ArticleDto articleDto = new ArticleDto();
        BeanUtils.copyProperties(wmNews, articleDto);
        //文章布局
        articleDto.setLayout(wmNews.getType());
        //频道
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if(wmChannel != null){
            articleDto.setChannelName(wmChannel.getName());
        }
        //作者
        articleDto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if(wmUser != null){
            articleDto.setAuthorName(wmUser.getName());
        }

        //保存文章id
        if(wmNews.getArticleId() != null){
            articleDto.setId(wmNews.getArticleId());
        }

        articleDto.setCreatedTime(new Date());
        ResponseResult res = articleClient.saveArticle(articleDto);
        return res;
    }

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private GreenImageScan greenImageScan;

    @Autowired
    private Tess4jClient tess4jClient;
    /**
     * 审核图片内容
     * @param wmNews
     * @return
     */
    private boolean handleImageScan(List<String> images, WmNews wmNews) {

        boolean flag = true;

        if(images == null || images.size() == 0){
            return flag;
        }
        //从Minio下载图片
        //图片去重
        images = images.stream().distinct().collect(Collectors.toList());

        List<byte[]> imageList = new ArrayList<>();

        try {
            for(String image:images){
                byte[] bytes = fileStorageService.downLoadFile(image);
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(in);
                //图片识别
                String result = tess4jClient.doOCR(bufferedImage);
                //过滤文字
                boolean isSensitive = handleSensitiveScan(result, wmNews);
                if(isSensitive){
                    return false;
                }
                imageList.add(bytes);
            }

        }catch (Exception e){
            e.printStackTrace();
        }


        try {
            Map map = greenImageScan.imageScan(imageList);

            if(map != null){
                //审核失败
                if(map.get("suggestion").equals("block")){
                    updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "图片违规");
                    flag = false;
                }

                //不确定信息, 需要人工审核
                if(map.get("suggestion").equals("review")){
                    wmNews.setStatus(WmNews.Status.ADMIN_AUTH.getCode());
                    wmNews.setReason("当前图片存在不确定内容");
                    flag = false;
                }

            }

        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        return flag;
    }


    @Autowired
    private GreenTextScan greenTextScan;
    /**
     * 审核纯文本内容
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleTextScan(String content, WmNews wmNews) {

        boolean flag = true;

        if(wmNews.getTitle().length() == 0 && wmNews.getContent().length() == 0){
            return flag;
        }

        try {
            Map map = greenTextScan.greeTextScan(wmNews.getTitle() + "-" + content);
            if(map != null){
                //审核失败
                if(map.get("suggestion").equals("block")){
                    updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "文章存在违规信息");
                    flag = false;
                }

                //不确定信息, 需要人工审核
                if(map.get("suggestion").equals("review")){
                    wmNews.setStatus(WmNews.Status.ADMIN_AUTH.getCode());
                    wmNews.setReason("当前文章存在不确定内容内容");
                    flag = false;
                }

            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        return flag;

    }

    private void updateWmNews(WmNews wmNews, short status, String reason){
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 从自媒体文章中提取文本和图片
     * 提取文章封面图片
     * @param wmNews
     * @return
     */
    private Map<String, Object> extractTextAndImage(WmNews wmNews) {

        StringBuilder stringBuilder = new StringBuilder();
        List<String> images = new ArrayList<>();

        if(StringUtils.isNotBlank(wmNews.getContent())){
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            for(Map map: maps){
                if(map.get("type").equals("text")){
                    stringBuilder.append(map.get("value"));
                }

                if(map.get("type").equals("image")){
                    images.add((String) map.get("value"));
                }
            }
        }

        if(StringUtils.isNotBlank(wmNews.getImages())){
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }

        Map<String, Object> resMap = new HashMap<>();
        resMap.put("content", stringBuilder.toString());
        resMap.put("images", images);

        return resMap;
    }
}
