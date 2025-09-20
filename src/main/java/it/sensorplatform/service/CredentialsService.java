package it.sensorplatform.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.sensorplatform.repository.CredentialsRepository;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.model.Project;

@Service
public class CredentialsService {

	@Autowired
	protected PasswordEncoder passwordEncoder;

	@Autowired
	protected CredentialsRepository credentialsRepository;

	@Autowired
	protected ProjectService projectService;

        @Transactional
        public Credentials getCredentials(Long id) {
                Optional<Credentials> result = this.credentialsRepository.findById(id);
                return result.orElse(null);
        }

        @Transactional
	public Credentials getCredentials(String username) {
		Optional<Credentials> result = this.credentialsRepository.findByUsername(username);
		return result.orElse(null);
	}
	
        @Transactional
        public Credentials getCredentialsByUsernameAndProjectId(String username, Long projectId) {
            return credentialsRepository.findByUsernameAndProjectId(username, projectId).get();
        }


        @Transactional
	public Credentials saveCredentials(Credentials credentials) {
		credentials.setPassword(this.passwordEncoder.encode(credentials.getPassword()));
		return this.credentialsRepository.save(credentials);
	}
	
	public boolean existsByUsernameAndProjectId(String username, Long projectId) {
	    return credentialsRepository.findByUsernameAndProjectId(username, projectId).isPresent();
	}

	public boolean existsByUsername(String username) {
		return this.credentialsRepository.existsByUsername(username);
	}

	public boolean existsByEmailAndProjectId(String email, Long projectId) {
		return this.credentialsRepository.existsByEmailAndProjectId(email, projectId);
	}
	
	/*public List<Credentials> findOperatorsByProject(Project project) {
	    String role = "OPERATOR";

	    if ("LTRAD".equals(project.getName())) {
	        role = Credentials.LTRAD_OPERATOR_ROLE;
	    } else if ("FIRE".equals(project.getName())) {
	        role = Credentials.FIRE_OPERATOR_ROLE;
	    } else if ("VOLCANO".equals(project.getName())) {
	        role = Credentials.VOLCANO_OPERATOR_ROLE;
	    } 

	    return credentialsRepository.findByRoleAndUserIsNull(role);
	}*/
	
        public List<Credentials> findByRoleAndProjectId(String role, Long projectId) {
            return credentialsRepository.findByRoleAndProjectId(role, projectId);
        }

        public Credentials findById(Long id) {
                return credentialsRepository.findById(id).get();
        }

        @Transactional
        public Credentials save(Credentials credentials) {
                return credentialsRepository.save(credentials);
        }

        @Transactional
        public Optional<Credentials> findByUsernameAndIdNot(String username, Long id) {
                return credentialsRepository.findByUsernameAndIdNot(username, id);
        }

        @Transactional
        public List<Credentials> findByProjectIdAndUserIsNotNull(Long projectId) {
                if (projectId == null) {
                        return new ArrayList<>();
                }
                return credentialsRepository.findByProjectIdAndUserIsNotNull(projectId);
        }

        @Transactional
        public Credentials updateCredentials(Credentials credentials, String newVisibleUsername, String rawPassword) {
                credentials.setVisibleUsername(newVisibleUsername);

                String suffix = Credentials.SUPERADMIN_ROLE;
                if (!Credentials.SUPERADMIN_ROLE.equals(credentials.getRole())) {
                        Long projectId = credentials.getProjectId();
                        if (projectId != null) {
                                Project project = projectService.getProjectById(projectId);
                                if (project != null && StringUtils.hasText(project.getName())) {
                                        suffix = project.getName();
                                } else {
                                        suffix = "";
                                }
                        } else {
                                suffix = "";
                        }
                }

                String persistedUsername = newVisibleUsername;
                if (StringUtils.hasText(suffix)) {
                        persistedUsername = newVisibleUsername + "|" + suffix;
                }
                credentials.setUsername(persistedUsername);

                if (StringUtils.hasText(rawPassword)) {
                        credentials.setPassword(this.passwordEncoder.encode(rawPassword));
                }

                return credentialsRepository.save(credentials);
        }


}