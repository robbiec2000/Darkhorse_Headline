package com.heima.schedule.service.impl;

import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.ScheduleApplication;
import com.heima.schedule.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

import static org.junit.Assert.*;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
public class TaskServiceImplTest {

    @Autowired
    TaskService taskService;

    @Test
    public void addTask(){
        Task task = new Task();
        task.setTaskType(100);
        task.setPriority(50);
        task.setParameters("new task".getBytes());
        task.setExecuteTime(new Date().getTime());

        Long id = taskService.addTask(task);
        System.out.println(id);
    }

    @Test
    public void addTask2(){
        for(int i = 0; i < 5; i++){
            Task task = new Task();
            task.setTaskType(100+i);
            task.setPriority(50);
            task.setParameters("task test".getBytes());
            task.setExecuteTime(new Date().getTime() + 500 * i);

            Long id = taskService.addTask(task);
            System.out.println(id);
        }
    }

    @Test
    public void remove(){
        taskService.cancelTask(1674057312828542977L);
    }

    @Test
    public void pull(){
        Task task = taskService.pull(100, 50);
        System.out.println(task);
    }

}