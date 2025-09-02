package it.sensorplatform.repository;


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.sensorplatform.model.Spec;

@Repository
public interface SpecRepository extends CrudRepository<Spec, Long>{

	@Query(value = """
		    SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
		    FROM spec
		    WHERE measurement = :#{#spec.measurement}
		      AND unit_of_measurement = :#{#spec.unitOfMeasurement}
		      AND component = :#{#spec.component}
		    """, nativeQuery = true)
		boolean existsByFields(@Param("spec") Spec spec);
	
}
