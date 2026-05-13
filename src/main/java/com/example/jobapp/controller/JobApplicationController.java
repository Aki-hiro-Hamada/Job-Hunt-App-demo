package com.example.jobapp.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;

import com.example.jobapp.entity.JobApplication;
import com.example.jobapp.entity.JobHistory;
import com.example.jobapp.service.JobApplicationService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService service;
    private final Environment environment;

    private boolean isListPreviewWithoutAuth() {
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            return true;
        }
        return Boolean.TRUE.equals(environment.getProperty("jobapp.preview-list-without-auth", Boolean.class, false));
    }

    @GetMapping
    public String list(Model model, Principal principal,
            @RequestParam(defaultValue = "asc") String statusDir,
            @RequestParam(defaultValue = "asc") String dateDir) {
        String statusSort = service.toSortDir(statusDir);
        String dateSort = service.toSortDir(dateDir);
        model.addAttribute("statusSort", statusSort);
        model.addAttribute("dateSort", dateSort);

        if (isListPreviewWithoutAuth() && principal == null) {
            model.addAttribute("applications", List.of());
            model.addAttribute("ingCount", 0L);
            return "list";
        }
        if (principal == null) {
            return "redirect:/login";
        }

        String userId = principal.getName();
        try {
            model.addAttribute("applications", service.findAll(userId, statusSort, dateSort));
            model.addAttribute("ingCount", service.getCountByStatus(userId, "書類選考中"));
        } catch (RuntimeException e) {
            if (isListPreviewWithoutAuth()) {
                model.addAttribute("applications", List.of());
                model.addAttribute("ingCount", 0L);
            } else {
                throw e;
            }
        }
        return "list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model, Principal principal) {
        JobApplication application = service.findById(principal.getName(), id);
        model.addAttribute("jobApplication", application);
        return "detail";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("jobApplication", new JobApplication());
        return "create";
    }

    /**
     * 旧URL互換（デプロイ差分やブックマーク対策）。
     * /applications/new を /applications/create に寄せます。
     */
    @GetMapping("/new")
    public String newFormCompat() {
        return "redirect:/applications/create";
    }

    @PostMapping("/create")
    public String create(@Validated @ModelAttribute JobApplication jobApplication,
                         BindingResult result,
                         Model model,
                         Principal principal) {
        if (result.hasErrors()) {
            return "create";
        }
        service.save(principal.getName(), jobApplication);
        return "redirect:/applications";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable("id") Long id, Model model, Principal principal) {
        JobApplication application = service.findById(principal.getName(), id);
        model.addAttribute("jobApplication", application);
        return "edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id,
                       @Validated @ModelAttribute JobApplication jobApplication,
                       BindingResult result,
                       Model model,
                       Principal principal) {
        if (result.hasErrors()) {
            return "edit";
        }
        jobApplication.setId(id);
        service.save(principal.getName(), jobApplication);
        return "redirect:/applications";
    }

    /**
     * 削除は GET で副作用を起こさない（リンクを踏んだだけで消える事故を防ぐ）。
     */
    @GetMapping("/delete/{id}")
    public String deleteGetCompat(@PathVariable("id") Long id) {
        return "redirect:/applications";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id, Principal principal) {
        service.deleteById(principal.getName(), id);
        return "redirect:/applications";
    }

    @PostMapping("/{id}/history")
    public String addHistory(@PathVariable("id") Long id, @ModelAttribute JobHistory newHistory, Principal principal) {
        service.addHistory(principal.getName(), id, newHistory);
        return "redirect:/applications/edit/" + id;
    }

    /**
     * モバイル等で「自動更新（ポーリング）」するためのAPI。
     * キャッシュを効かせないよう no-store を付与します。
     */
    @GetMapping("/api/applications")
    @ResponseBody
    public ResponseEntity<List<JobApplication>> apiApplications(Principal principal,
            @RequestParam(defaultValue = "asc") String statusDir,
            @RequestParam(defaultValue = "asc") String dateDir) {
        String statusSort = service.toSortDir(statusDir);
        String dateSort = service.toSortDir(dateDir);
        if (isListPreviewWithoutAuth() && principal == null) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(List.of());
        }
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(service.findAll(principal.getName(), statusSort, dateSort));
        } catch (RuntimeException e) {
            if (isListPreviewWithoutAuth()) {
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(List.of());
            }
            throw e;
        }
    }

    /**
     * 応募先詳細の最新データ取得（自動更新用）。
     */
    @GetMapping("/api/applications/{id}")
    @ResponseBody
    public ResponseEntity<JobApplication> apiApplication(@PathVariable("id") Long id, Principal principal) {
        try {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(service.findById(principal.getName(), id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound()
                    .cacheControl(CacheControl.noStore())
                    .build();
        }
    }
}
