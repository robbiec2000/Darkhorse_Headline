package com.heima.minio.test;

import com.heima.file.service.FileStorageService;
import com.heima.minio.MinIOApplication;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@SpringBootTest(classes = MinIOApplication.class)
@RunWith(SpringRunner.class)
public class MinIOTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    public void test() throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream("C:/Users/Robbie/IdeaProjects/list.html");
        String path = fileStorageService.uploadHtmlFile("", "test2.html", fileInputStream);
        System.out.println(path);
    }
    /*
    public static void main(String[] args){

        try {
        FileInputStream fileInputStream = new FileInputStream("C:/Users/Robbie/IdeaProjects/list.html");
        //获取minio连接信息，获取minio客户端
        MinioClient minioClient = MinioClient.builder().credentials("minio", "minio123").endpoint("http://192.168.200.130:9000").build();
        PutObjectArgs putObjectArgs = null;

            putObjectArgs = PutObjectArgs.builder()
                    .object("test.html")
                    .contentType("text/html")
                    .bucket("leadnews") //桶名称
                    .stream(fileInputStream, fileInputStream.available(), -1).build();

            minioClient.putObject(putObjectArgs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }*/
}
