package it.sensorplatform.model;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import it.sensorplatform.util.MacAddressUtils;
@Entity
public class Device {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
        @Column(nullable = true)
        private String name;
	
        @Column(nullable = true)
        private Double latitude;

        @Column(nullable = true)
        private Double longitude;
	
	@Column(nullable = true, unique = true)
	private String devEui;
	
        @Column(unique = true, nullable = true)
        private String macAddress;
	
        @Column(nullable = true)
        private String emailOwner;

        @Column(nullable = true)
        private String gsheet;

        @Column(name = "activated", nullable = false)
        private boolean activated = false;
	
	@ManyToOne
	private Project project;
	
	@OneToMany
	private List <MeasurementRecord> records;
	
	@ManyToOne
	private Group group;
	
	@ManyToOne
	private TypeOfDevice tod;
	
	@ManyToOne
	@JoinColumn(name = "operator_id", nullable = true) 
	private Credentials operator;
	
	public Credentials getOperator() {
		return operator;
	}

	public void setOperator(Credentials operator) {
		this.operator = operator;
	}


	public Long getId() {
		return id;
	}
	
	
	public void setId(Long id) {
		this.id = id;
	}
	
	
	public String getName() {
		return name;
	}
	
	
	public void setName(String name) {
		this.name = name;
	}
	
	
	public Double getLatitude() {
		return latitude;
	}
	
	
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	
	
	public Double getLongitude() {
		return longitude;
	}
	
	
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	
	
	public String getDevEui() {
		return devEui;
	}
	
	
        public void setDevEui(String devEui) {
                this.devEui = MacAddressUtils.normalize(devEui);
        }


	public String getMacAddress() {
		return macAddress;
	}


        public void setMacAddress(String macAddress) {
                this.macAddress = MacAddressUtils.normalize(macAddress);
        }
	
	
	public List<MeasurementRecord> getRecords() {
		return records;
	}


	public void setRecords(List<MeasurementRecord> records) {
		this.records = records;
	}


	public String getEmailOwner() {
		return emailOwner;
	}

	
        public void setEmailOwner(String emailOwner) {
                this.emailOwner = emailOwner;
        }

        public String getGsheet() {
                return normalizeBlank(gsheet);
        }

        public void setGsheet(String gsheet) {
                this.gsheet = normalizeBlank(gsheet);
        }

        public boolean isActivated() {
                return activated;
        }

        public void setActivated(boolean activated) {
                this.activated = activated;
        }


	public Group getGroup() {
		return group;
	}


	public void setGroup(Group group) {
		this.group = group;
	}
	
	
	public Project getProject() {
		return project;
	}


	public void setProject(Project project) {
		this.project = project;
	}
	
	

	public TypeOfDevice getTod() {
		return tod;
	}


	public void setTod(TypeOfDevice tod) {
		this.tod = tod;
	}


	@Override
	public int hashCode() {
		return Objects.hash(id, macAddress);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Device other = (Device) obj;
		return Objects.equals(id, other.id) && Objects.equals(macAddress, other.macAddress);
	}
	
        public String getVisibleUsername() {
                if(operator!=null)
                        return operator.getVisibleUsername();
                return null;
        }

        public String getDeviceKey() {
                if (macAddress != null && !macAddress.isBlank()) {
                        return macAddress;
                }
                if (devEui != null && !devEui.isBlank()) {
                        return devEui;
                }
                return null;
        }

        private static String normalizeBlank(String value) {
                if (value == null || value.isBlank()) {
                        return null;
                }
                return value;
        }
}

