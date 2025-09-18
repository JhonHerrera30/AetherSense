package it.sensorplatform.model;


import java.util.List;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;

@Entity
public class TypeOfDevice {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@NotBlank
	private String name;
	
        @ManyToMany
        private List<Spec> specs;

        @OneToMany
        @JoinTable(
                        name = "type_of_device_indicator",
                        joinColumns = @JoinColumn(name = "type_of_device_id"),
                        inverseJoinColumns = @JoinColumn(name = "indicator_id"))
        private List<Indicator> indicators;

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

	public List<Spec> getSpecs() {
		return specs;
	}

        public void setSpecs(List<Spec> specs) {
                this.specs = specs;
        }

        public List<Indicator> getIndicators() {
                return indicators;
        }

        public void setIndicators(List<Indicator> indicators) {
                this.indicators = indicators;
        }

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}
	

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypeOfDevice other = (TypeOfDevice) obj;
		return Objects.equals(id, other.id) && Objects.equals(name, other.name);
	}
	
	
}
