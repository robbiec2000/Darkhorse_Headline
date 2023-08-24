package com.heima.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.constants.ScheduleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@Transactional
public class TaskServiceImpl implements TaskService {
    /**
     * 添加延迟任务
     *
     * @param task
     * @return
     */
    @Override
    public long addTask(Task task) {
        //添加任务到数据库中
        boolean success = addTaskToDb(task);

        if(success){
            //添加任务到redis中
            addTaskToCache(task);
        }

        return task.getTaskId();
    }


    @Autowired
    private CacheService cacheService;
    /**
     * //添加任务到redis中
     * @param task
     */
    private void addTaskToCache(Task task) {
        String key = task.getTaskType() + "_" + task.getPriority();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        long nextScheduleTime = calendar.getTimeInMillis();


        if(task.getExecuteTime() <= System.currentTimeMillis()){
            //执行任务时间小于当前时间, 存入list中
            cacheService.lLeftPush(ScheduleConstants.TOPIC + key, JSON.toJSONString(task));
        }else if(task.getExecuteTime() <= nextScheduleTime){
            //执行任务时间大于当前时间, 小于未来5分钟, 存入zset中
            cacheService.zAdd(ScheduleConstants.FUTURE + key, JSON.toJSONString(task), task.getExecuteTime());

        }
    }

    @Autowired
    private TaskinfoMapper taskinfoMapper;
    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;
    /**
     * 添加任务到数据库中
     * @param task
     * @return
     */
    private boolean addTaskToDb(Task task) {
        try {
            //保存任务表
            Taskinfo taskinfo = new Taskinfo();
            BeanUtils.copyProperties(task, taskinfo);
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoMapper.insert(taskinfo);
            //设置task id
            task.setTaskId(taskinfo.getTaskId());
            //保存任务日志数据
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            taskinfoLogs.setVersion(1);
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
            BeanUtils.copyProperties(taskinfo, taskinfoLogs);
            taskinfoLogsMapper.insert(taskinfoLogs);
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 取消任务
     * @param taskId
     * @return
     */
    @Override
    public boolean cancelTask(long taskId) {
        //删除任务, 更新任务日志
        Task task = updateDb(taskId, ScheduleConstants.CANCELLED);
        if(task != null){
            removeTaskFromCache(task);
            return true;
        }
        return false;
    }


    /**
     * 删除redis中数据
     * @param task
     */
    private void removeTaskFromCache(Task task) {
        String key = task.getTaskType() + "_" + task.getPriority();
        if(task.getExecuteTime() <= System.currentTimeMillis()){
            cacheService.lRemove(ScheduleConstants.TOPIC + key, 0, JSON.toJSONString(task));
        }else{
            cacheService.zRemove(ScheduleConstants.FUTURE + key, JSON.toJSONString(task));

        }

    }

    /**
     * 删除任务 更新任务日志
     * @param taskId
     * @param status
     * @return
     */
    private Task updateDb(long taskId, int status) {
        Task task = null;

        try {
            //删除任务
            taskinfoMapper.deleteById(taskId);
            //更新任务日志
            TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(taskId);
            taskinfoLogs.setStatus(status);
            taskinfoLogsMapper.updateById(taskinfoLogs);

            task = new Task();
            BeanUtils.copyProperties(taskinfoLogs, task);
            task.setExecuteTime(taskinfoLogs.getExecuteTime().getTime());

        }catch (Exception e){
            e.printStackTrace();
        }


        return task;

    }

    /**
     * 拉取任务
     * @return
     */
    @Override
    public Task pull(int type, int priority) {

        Task task = null;

        try {
            String key = type + "_" + priority;
            //从redis拉取数据
            String task_json = cacheService.lRightPop(ScheduleConstants.TOPIC + key);
            if(StringUtils.isNotBlank(task_json)){
                task = JSON.parseObject(task_json, Task.class);
                //修改数据库信息
                updateDb(task.getTaskId(), ScheduleConstants.EXECUTED);
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return task;
    }


    /**
     * 未来数据定时刷新
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void schedule(){

        String token = cacheService.tryLock("FUTURE_KEY", 3000);

        if(StringUtils.isNotBlank(token)){
            log.info("未来数据定时刷新");

            //获取所有feature key
            Set<String> featureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");

            for (String featureKey : featureKeys) {

                //根据feature key生成topic key
                String topicKey = ScheduleConstants.TOPIC + featureKey.split(ScheduleConstants.FUTURE)[1];

                //根据key查询符合条件的task
                Set<String> tasks = cacheService.zRangeByScore(featureKey, 0, System.currentTimeMillis());

                //同步数据到list中
                if(!tasks.isEmpty()){
                    cacheService.refreshWithPipeline(featureKey, topicKey, tasks);
                    log.info(featureKey + "刷新到了" + topicKey);
                }

            }
        }
    }

    /**
     * 数据库任务定时同步到redis中
     */
    @PostConstruct
    @Scheduled(cron = "0 */5 * * * ?")
    public void reloadData(){

        //清除缓存数据
        clearCache();

        //查询小于未来5分钟的任务
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        List<Taskinfo> taskInfoList = taskinfoMapper.selectList(Wrappers.<Taskinfo>lambdaQuery().lt(Taskinfo::getExecuteTime, calendar.getTime()));

        //把任务添加到redis中
        if(taskInfoList != null && taskInfoList.size() > 0){
            for (Taskinfo taskinfo : taskInfoList) {
                Task task = new Task();
                BeanUtils.copyProperties(taskinfo, task);
                addTaskToCache(task);
            }
        }

        log.info("数据库同步到redis");
    }

    public void clearCache(){
        Set<String> topicKeys = cacheService.scan(ScheduleConstants.TOPIC + "*");
        Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
        cacheService.delete(topicKeys);
        cacheService.delete(futureKeys);
    }
}
