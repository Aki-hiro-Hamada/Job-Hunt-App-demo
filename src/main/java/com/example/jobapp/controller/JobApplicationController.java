package com.example.jobapp.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

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
    private final SmartValidator smartValidator;

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
        JobApplication loaded = service.findById(principal.getName(), id);
        JobApplication form = new JobApplication();
        form.setId(loaded.getId());
        form.setCompanyName(loaded.getCompanyName());
        form.setOwnerUserId(loaded.getOwnerUserId());
        // ステータス・日付・メモは空欄で表示（会社名のみ既存値）
        model.addAttribute("jobApplication", form);
        model.addAttribute("histories", loaded.getJobHistories());
        return "edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id,
                       @ModelAttribute JobApplication jobApplication,
                       BindingResult result,
                       Model model,
                       Principal principal) {
        JobApplication existing = service.findById(principal.getName(), id);
        mergeEditFormFromExisting(jobApplication, existing);
        jobApplication.setId(id);
        smartValidator.validate(jobApplication, result);
        if (result.hasErrors()) {
            model.addAttribute("histories", existing.getJobHistories());
            return "edit";
        }
        service.save(principal.getName(), jobApplication);
        return "redirect:/applications/edit/" + id;
    }

    /**
     * 編集画面は未入力のままの項目を既存値で埋めてから Bean Validation する（空欄表示と保存の両立）。
     */
    private static void mergeEditFormFromExisting(JobApplication form, JobApplication existing) {
        if (form.getStatus() == null || form.getStatus().isBlank()) {
            form.setStatus(existing.getStatus());
        } else {
            form.setStatus(form.getStatus().trim());
        }
        if (form.getInterviewDate() == null) {
            form.setInterviewDate(existing.getInterviewDate());
        }
        if (form.getMemo() == null || form.getMemo().isBlank()) {
            form.setMemo(existing.getMemo());
        }
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

    @GetMapping("/edit/{jobId}/history/{historyId}")
    public String editHistoryForm(@PathVariable("jobId") Long jobId,
            @PathVariable("historyId") Long historyId,
            Model model,
            Principal principal) {
        try {
            model.addAttribute("jobApplication", service.findById(principal.getName(), jobId));
            model.addAttribute("jobHistory", service.findHistoryById(principal.getName(), jobId, historyId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "history-edit";
    }

    @PostMapping("/edit/{jobId}/history/{historyId}")
    public String updateHistory(@PathVariable("jobId") Long jobId,
            @PathVariable("historyId") Long historyId,
            @RequestParam("action") String action,
            @RequestParam(value = "eventDate", required = false) String eventDate,
            @RequestParam(value = "note", required = false) String note,
            Principal principal) {
        try {
            service.updateHistory(principal.getName(), jobId, historyId, action, eventDate, note);
        } catch (IllegalArgumentException e) {
            throw mapHistoryIllegalArgument(e);
        }
        return "redirect:/applications/edit/" + jobId;
    }

    @PostMapping("/edit/{jobId}/history/{historyId}/delete")
    public String deleteHistory(@PathVariable("jobId") Long jobId,
            @PathVariable("historyId") Long historyId,
            Principal principal) {
        try {
            service.deleteHistory(principal.getName(), jobId, historyId);
        } catch (IllegalArgumentException e) {
            throw mapHistoryIllegalArgument(e);
        }
        return "redirect:/applications/edit/" + jobId;
    }

    private static ResponseStatusException mapHistoryIllegalArgument(IllegalArgumentException e) {
        String m = e.getMessage() != null ? e.getMessage() : "";
        if (m.startsWith("Invalid")) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
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
