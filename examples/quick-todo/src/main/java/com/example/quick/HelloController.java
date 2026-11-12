package com.example.quick;
import org.springframework.web.bind.annotation.*;
@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() { return "服务已启动"; }
}
