package hu.uni_obuda.thesis.railways.cloud.securityserver.repository;

import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.RoleEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RoleRepository extends CrudRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(String name);
}