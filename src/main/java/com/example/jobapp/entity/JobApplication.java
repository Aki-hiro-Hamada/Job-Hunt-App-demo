package com.example.jobapp.entity;

import java.util.Comparator;

import org.hibernate.annotations.OrderBy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_applications")
@Data
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所有者（ログインユーザー）のユーザー名。
     * このフィールドで全クエリをスコープし、他ユーザーのデータに到達できないようにする。
     */
    @Column(name = "owner_user_id", nullable = false)
    private String ownerUserId;

    @NotBlank(message = "会社名は必須です")
    @Column(name = "company_name", nullable = false)
    private String companyName;

    @NotBlank(message = "ステータスは必須です")
    @Column(nullable = false)
    private String status;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "interview_date")
    private LocalDate interviewDate;

    @OneToMany(mappedBy = "jobApplication", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy(clause = "event_date desc nulls last, id desc")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<JobHistory> jobHistories = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String memo;

    /** 双方向関連を保ったまま履歴を追加する（テンプレート / Service から使用）。 */
    public void addHistory(JobHistory history) {
        if (jobHistories == null) {
            jobHistories = new ArrayList<>();
        }
        history.setJobApplication(this);
        jobHistories.add(history);
    }

    /**
     * 一覧のステータス表示用。履歴がある場合は「イベント日が最も新しい履歴」の action を優先し、
     * 無い・日付がすべて空の場合は応募先の status を使う。
     */
    public String getDisplayStatus() {
        JobHistory latest = pickLatestHistoryForDisplay();
        if (latest != null && latest.getAction() != null && !latest.getAction().isBlank()) {
            return latest.getAction().trim();
        }
        return status != null ? status : "";
    }

    private JobHistory pickLatestHistoryForDisplay() {
        if (jobHistories == null || jobHistories.isEmpty()) {
            return null;
        }
        return jobHistories.stream()
                .max(Comparator
                        .comparing(JobHistory::getEventDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(JobHistory::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }
}
