package it.sensorplatform.model;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor

public class Spec {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	//@NotBlank
	private String measurement;
	
	//@NotBlank
	private String unitOfMeasurement;
	
	//@NotBlank
	private String component;

	private Double min;

	private Double max;


	@Override
	public int hashCode() {
		return Objects.hash(component, measurement);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Spec other = (Spec) obj;
		return Objects.equals(component, other.component) && Objects.equals(measurement, other.measurement);
	}
	
	
	
}

