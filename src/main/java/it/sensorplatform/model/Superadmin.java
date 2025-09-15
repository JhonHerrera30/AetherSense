package it.sensorplatform.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class Superadmin {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @OneToOne
        @JoinColumn(name = "credentials_id", nullable = false, unique = true)
        private Credentials credentials;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "superadmin_admin_emails", joinColumns = @JoinColumn(name = "superadmin_id"))
        @Column(name = "admin_email", nullable = false)
        private List<String> adminEmails = new ArrayList<>();

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public Credentials getCredentials() {
                return credentials;
        }

        public void setCredentials(Credentials credentials) {
                this.credentials = credentials;
        }

        public List<String> getAdminEmails() {
                return adminEmails;
        }

        public void setAdminEmails(List<String> adminEmails) {
                this.adminEmails = adminEmails;
        }

        public void addAdminEmail(String email) {
                if (email == null) {
                        return;
                }
                String normalized = email.trim();
                if (normalized.isEmpty()) {
                        return;
                }
                boolean alreadyPresent = this.adminEmails.stream()
                                .anyMatch(existing -> existing.equalsIgnoreCase(normalized));
                if (!alreadyPresent) {
                        this.adminEmails.add(normalized);
                }
        }

        @Override
        public int hashCode() {
                return Objects.hash(id);
        }

        @Override
        public boolean equals(Object obj) {
                if (this == obj)
                        return true;
                if (obj == null)
                        return false;
                if (getClass() != obj.getClass())
                        return false;
                Superadmin other = (Superadmin) obj;
                return Objects.equals(id, other.id);
        }
}
