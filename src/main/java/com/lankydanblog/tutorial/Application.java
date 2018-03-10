package com.lankydanblog.tutorial;

import com.lankydanblog.tutorial.client.Client;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Application {

  public static void main(String args[]) {
    SpringApplication.run(Application.class);
    Client client = new Client();
    client.doStuff();
  }
}
