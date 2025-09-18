package it.sensorplatform.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import it.sensorplatform.model.Credentials;

@Repository
public interface CredentialsRepository extends CrudRepository<Credentials, Long>{
	public Optional<Credentials> findByUsername(String username);

	public Optional<Credentials> findByUsernameAndProjectId(String username, Long projectId);

	public boolean existsByUsername(String username);

	public boolean existsByEmailAndProjectId(String email, Long projectId);
	
    public List<Credentials> findByRoleAndUserIsNull(String role);
    
    public Optional<Credentials>  findById(Long id);
    
    List<Credentials> findByRoleAndProjectId(String role, Long projectId);
}
