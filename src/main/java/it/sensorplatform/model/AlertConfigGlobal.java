package it.sensorplatform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "alert_config_global")
public class AlertConfigGlobal {

    @Id
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "interval_min", nullable = false)
    private int intervalMin = 30;

    @Column(name = "telegram_chat_id")
    private String telegramChatId;

    @Column(name = "telegram_invite_link")
    private String telegramInviteLink;

    @OneToOne
    @MapsId
    @JoinColumn(name = "project_id")
    private Project project;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public int getIntervalMin() { return intervalMin; }
    public void setIntervalMin(int intervalMin) { this.intervalMin = intervalMin; }

    public String getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; }

    public String getTelegramInviteLink() { return telegramInviteLink; }
    public void setTelegramInviteLink(String telegramInviteLink) { this.telegramInviteLink = telegramInviteLink; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
}