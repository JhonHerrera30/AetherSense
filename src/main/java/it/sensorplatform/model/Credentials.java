package it.sensorplatform.model;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Credentials {
	
	public static final String LTRAD_OPERATOR_ROLE = "LTRAD_OPERATOR";
	public static final String FIRE_OPERATOR_ROLE = "FIRE_OPERATOR";
	public static final String VOLCANO_OPERATOR_ROLE = "VOLCANO_OPERATOR";
	public static final String LTRAD_ADMIN_ROLE = "LTRAD_ADMIN";
	public static final String FIRE_ADMIN_ROLE = "FIRE_ADMIN";
	public static final String VOLCANO_ADMIN_ROLE = "VOLCANO_ADMIN";
	public static final String SUPERADMIN_ROLE = "SUPERADMIN";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@Column(nullable = false)
	@NotBlank
	private String username;
	
	@Column(nullable = false)
	@NotBlank
	private String email;
	
	@Column(nullable = false)
	@NotBlank
	private String password;
	
	@Column(nullable = false)
	private String role;
	
	@OneToOne(cascade = CascadeType.ALL)
	private User user;
	
	private Long projectId;	
	
	@Column(nullable = true)
	private String visibleUsername;
	
        @OneToMany
        private List<Device> device;

        @OneToOne(mappedBy = "credentials")
        private Admin admin;

        @ManyToOne
        private Admin employer;
	
	
	public List<Device> getDevice() {
		return device;
	}

        public void setDevice(List<Device> device) {
                this.device = device;
        }

        public Admin getAdmin() {
                return admin;
        }

        public void setAdmin(Admin admin) {
                this.admin = admin;
        }

        public Admin getEmployer() {
                return employer;
        }

        public void setEmployer(Admin employer) {
                this.employer = employer;
        }

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}


	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}


	public String getVisibleUsername() {
		return visibleUsername;
	}

	public void setVisibleUsername(String visibleUsername) {
		this.visibleUsername = visibleUsername;
	}

	@Override
	public int hashCode() {
		return Objects.hash(email, id, password, username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Credentials other = (Credentials) obj;
		return Objects.equals(email, other.email) && Objects.equals(id, other.id)
				&& Objects.equals(password, other.password) && Objects.equals(username, other.username);
	}
	
	
	
}