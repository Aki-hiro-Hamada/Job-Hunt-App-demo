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
     * 一覧のステータス表示用。履歴がある場合は「{@code event_date} がカレンダー上で最も未来（最大）の履歴」の
     * {@code action} を優先し、無い場合は応募先の {@code status} を使う。
     */
    public String getDisplayStatus() {
        JobHistory h = pickMostFutureHistoryForList();
        if (h != null && h.getAction() != null && !h.getAction().isBlank()) {
            return h.getAction().trim();
        }
        return status != null ? status : "";
    }

    /**
     * 一覧で「ステータス・日付・メモ」を揃える基準となる履歴。
     * {@code event_date} の最大値（＝1番未来の日付）の行を選び、同日複数なら {@code id} が大きい方。
     * すべて {@code event_date} が null のときは {@code id} 最大のみで決める。
     */
    private JobHistory pickMostFutureHistoryForList() {
        if (jobHistories == null || jobHistories.isEmpty()) {
            return null;
        }
        return jobHistories.stream()
                .max(Comparator
                        .comparing(JobHistory::getEventDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(JobHistory::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    /**
     * 一覧の「日付」列用。その「最も未来の event_date」の履歴に日付があればそれを、
     * 無い（履歴なし／当該行のみ null）なら面接日 {@link #interviewDate} を返す。
     */
    public LocalDate getDisplayInterviewDate() {
        JobHistory h = pickMostFutureHistoryForList();
        if (h != null && h.getEventDate() != null) {
            return h.getEventDate();
        }
        return interviewDate;
    }

    /**
     * 一覧の「メモ」列用。上記と同一の「最も未来日の履歴」の {@link JobHistory#getNote()} のみ。
     * 履歴が無い場合は応募先の自由メモ {@link #memo} を返す。
     */
    public String getDisplayMemoForList() {
        JobHistory h = pickMostFutureHistoryForList();
        if (h != null) {
            if (h.getNote() != null && !h.getNote().isBlank()) {
                return h.getNote().trim();
            }
            return "";
        }
        return memo != null ? memo.trim() : "";
    }
}
