package com.iiht.training.eloan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iiht.training.eloan.entity.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long>{
	
	List<Users> findByRole(String role);
	
	@Query("Select U from Users U where U.role =:role and U.id=:id")
	Users findByRoleId(@Param("role")String role, @Param("id")Long id);

}
