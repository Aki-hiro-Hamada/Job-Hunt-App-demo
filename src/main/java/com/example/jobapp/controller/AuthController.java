package com.example.jobapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.jobapp.service.AppUserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AppUserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam("username") String username,
                           @RequestParam("password") String password,
                           Model model) {
        try {
            userService.register(username, password);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", mapRegisterValidationMessage(e.getMessage()));
            model.addAttribute("username", username);
            return "register";
        } catch (DuplicateKeyException e) {
            // existsByUsername と競合・レース時など
            model.addAttribute("error", "このユーザー名は既に登録されています。");
            model.addAttribute("username", username);
            return "register";
        } catch (DataAccessException e) {
            log.warn("User registration failed (database): {}", e.toString());
            model.addAttribute("error",
                    "データベースへの保存に失敗しました。Render の Supabase 接続（SPRING_DATASOURCE_URL / "
                            + "SPRING_DATASOURCE_USERNAME / SUPABASE_DB_PASSWORD）と、Supabase 側のテーブル権限を確認してください。");
            model.addAttribute("username", username != null ? username : "");
            return "register";
        } catch (Exception e) {
            log.error("User registration failed (unexpected)", e);
            model.addAttribute("error", "登録に失敗しました。時間をおいて再度お試しください。");
            model.addAttribute("username", username != null ? username : "");
            return "register";
        }
    }

    private static String mapRegisterValidationMessage(String english) {
        if (english == null) {
            return "入力内容を確認してください。";
        }
        return switch (english) {
            case "username is required" -> "ユーザー名を入力してください。";
            case "password is required" -> "パスワードを入力してください。";
            case "username already exists" -> "このユーザー名は既に使われています。";
            default -> english;
        };
    }
}

