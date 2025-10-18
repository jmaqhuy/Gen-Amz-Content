package com.example.ai_gen_amz_content;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("currentPage", "home");
        return "index";
    }

    @GetMapping("/images")
    public String images(Model model) {
        model.addAttribute("currentPage", "images");
        return "upload-images";
    }

    @GetMapping("orders")
    public String orders(Model model) {
        model.addAttribute("currentPage", "orders");
        return "extract-order";
    }
}
