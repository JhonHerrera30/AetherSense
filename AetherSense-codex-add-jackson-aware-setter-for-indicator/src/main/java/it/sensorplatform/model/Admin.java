package it.sensorplatform.model;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToMany;

@Entity
public class Admin {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @OneToOne
        private Credentials credentials;

        @OneToMany(mappedBy = "employer")
        private List<Credentials> operators;

        @ManyToMany
        private List<Credentials> authorizedOperators;

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

        public List<Credentials> getOperators() {
                return operators;
        }

        public void setOperators(List<Credentials> operators) {
                this.operators = operators;
        }

        public List<Credentials> getAuthorizedOperators() {
                return authorizedOperators;
        }

        public void setAuthorizedOperators(List<Credentials> authorizedOperators) {
                this.authorizedOperators = authorizedOperators;
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
                Admin other = (Admin) obj;
                return Objects.equals(id, other.id);
        }
}

