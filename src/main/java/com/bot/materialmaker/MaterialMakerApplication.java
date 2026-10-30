package com.bot.materialmaker;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bot.materialmaker.mapper")
public class MaterialMakerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaterialMakerApplication.class, args);
    }
}
