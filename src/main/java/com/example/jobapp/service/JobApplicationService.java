package com.example.jobapp.service;

import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import java.time.LocalDate;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jobapp.entity.JobApplication;
import com.example.jobapp.entity.JobHistory;
import com.example.jobapp.repository.JobApplicationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository repository;

    /**
     * @param statusDir {@code asc} または {@code desc}（以外は asc 扱い）
     * @param dateDir   {@code asc} または {@code desc}（以外は asc 扱い）
     */
    @Transactional(readOnly = true)
    public List<JobApplication> findAll(String ownerUserId, String statusDir, String dateDir) {
        // 表示順: 1) ステータス順 2) 日付順（null は最後）
        // DB の Sort だけだと「日本語ステータスの任意順」を表現しづらいので、アプリ側で並べ替える。
        List<JobApplication> list = repository.findAllByOwnerUserId(ownerUserId, Sort.unsorted());

        boolean statusAsc = !"desc".equalsIgnoreCase(normalizeSortDir(statusDir));
        boolean dateAsc = !"desc".equalsIgnoreCase(normalizeSortDir(dateDir));

        List<String> statusOrder = List.of(
                "応募前",
                "書類選考中",
                "一次面接",
                "二次面接",
                "最終面接",
                "内定",
                "お見送り",
                "辞退"
        );
        Map<String, Integer> statusRank = statusOrder.stream()
                .collect(Collectors.toMap(s -> s, statusOrder::indexOf));

        Comparator<JobApplication> byStatus = Comparator.comparingInt(app ->
                statusRank.getOrDefault(app.getStatus(), Integer.MAX_VALUE)
        );
        if (!statusAsc) {
            byStatus = byStatus.reversed();
        }

        Comparator<LocalDate> dateKey = dateAsc
                ? Comparator.nullsLast(Comparator.naturalOrder())
                : Comparator.nullsLast(Comparator.reverseOrder());
        Comparator<JobApplication> byDate = Comparator.comparing(JobApplication::getInterviewDate, dateKey);

        return list.stream()
                .sorted(byStatus.thenComparing(byDate))
                .toList();
    }

    /** テンプレート・URL 用に asc / desc のみ返す */
    public String toSortDir(String raw) {
        return normalizeSortDir(raw);
    }

    private static String normalizeSortDir(String dir) {
        if (dir == null) {
            return "asc";
        }
        String t = dir.trim().toLowerCase();
        return "desc".equals(t) ? "desc" : "asc";
    }

    @Transactional(readOnly = true)
    public JobApplication findById(String ownerUserId, Long id) {
        return repository.findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid application Id:" + id));
    }

    @Transactional
    public void save(String ownerUserId, JobApplication application) {
        application.setOwnerUserId(ownerUserId);

        // 編集時はフォームに jobHistories が含まれないため、null のまま save すると
        // カスケード設定により既存履歴が消える事故になる。
        // 既存レコードを読み込み、フォームで入力された値だけを反映する。
        if (application.getId() != null) {
            JobApplication existing = repository.findByIdAndOwnerUserId(application.getId(), ownerUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid application Id:" + application.getId()));
            existing.setCompanyName(application.getCompanyName());
            existing.setStatus(application.getStatus());
            existing.setInterviewDate(application.getInterviewDate());
            existing.setMemo(application.getMemo());
            repository.save(existing);
            return;
        }
        repository.save(application);
    }

    @Transactional
    public void deleteById(String ownerUserId, Long id) {
        repository.deleteByIdAndOwnerUserId(id, ownerUserId);
    }

    @Transactional(readOnly = true)
    public long getCountByStatus(String ownerUserId, String status) {
        return repository.countByOwnerUserIdAndStatus(ownerUserId, status);
    }

    /**
     * 履歴追加は find と save を同一トランザクションで行う（OSIV 無効時の detached マージと
     * orphanRemoval 絡みの不整合で子が消えるのを防ぐ）。リクエスト束縛で id が付かないよう明示する。
     */
    @Transactional
    public void addHistory(String ownerUserId, Long jobId, JobHistory newHistory) {
        newHistory.setId(null);
        newHistory.setJobApplication(null);
        JobApplication job = repository.findByIdAndOwnerUserId(jobId, ownerUserId)
                .orElseThrow(() -> new RuntimeException("応募先が見つかりません"));
        job.addHistory(newHistory);
        repository.save(job);
    }
}
