package com.example.jobapp.entity;

import java.time.LocalDate;

import lombok.Data;

/**
 * {@link JobApplication} に埋め込まれる履歴（MongoDB のサブドキュメント）。
 */
@Data
public class JobHistory {

    private LocalDate eventDate;
    private String action;
    private String note;

}
