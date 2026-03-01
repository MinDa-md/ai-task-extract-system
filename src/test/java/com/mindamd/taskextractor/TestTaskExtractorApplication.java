package com.mindamd.taskextractor;

import org.springframework.boot.SpringApplication;

public class TestTaskExtractorApplication {

    public static void main(String[] args) {
        SpringApplication.from(TaskExtractorApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
