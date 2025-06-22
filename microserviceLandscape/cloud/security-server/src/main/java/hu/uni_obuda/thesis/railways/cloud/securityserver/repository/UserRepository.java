package hu.uni_obuda.thesis.railways.cloud.securityserver.repository;

import hu.uni_obuda.thesis.railways.cloud.securityserver.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
}
