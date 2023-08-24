package com.heima.kafka.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class HelloListener {


    @KafkaListener(topics = "itcast-topic")
    public void onMessage(String message){
        System.out.println(message);
    }
}
