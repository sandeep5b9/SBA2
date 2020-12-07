package com.iiht.training.eloan.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iiht.training.eloan.dto.UserDto;
import com.iiht.training.eloan.entity.Users;
import com.iiht.training.eloan.repository.UsersRepository;
import com.iiht.training.eloan.service.AdminService;

@Service
public class AdminServiceImpl implements AdminService {

	@Autowired
	private UsersRepository usersRepository;

	private UserDto convertToUserOutputDto(Users users) {
		return new UserDto(users.getId(), users.getFirstName(), users.getLastName(), users.getEmail(),
				users.getMobile());
	}

	private Users covertToUserEntity(UserDto userDto, String role) {
		return new Users(userDto.getFirstName(), userDto.getLastName(), userDto.getEmail(), userDto.getMobile(),
				role);
	}

	@Override
	public UserDto registerClerk(UserDto userDto) {
		Users users = this.covertToUserEntity(userDto, "Clerk");
		Users newUsers = this.usersRepository.save(users);
		return this.convertToUserOutputDto(newUsers);
	}

	@Override
	public UserDto registerManager(UserDto userDto) {
		Users users = this.covertToUserEntity(userDto, "Manager");
		Users newUsers = this.usersRepository.save(users);
		return this.convertToUserOutputDto(newUsers);
	}

	@Override
	public List<UserDto> getAllClerks() {
		List<Users> users = this.usersRepository.findByRole("Clerk");// findByRole("Clerk")
		return users.stream().map(this::convertToUserOutputDto).collect(Collectors.toList());
	}

	@Override
	public List<UserDto> getAllManagers() {
		List<Users> users = this.usersRepository.findByRole("Manager");// findByRole("Manager")
		return users.stream().map(this::convertToUserOutputDto).collect(Collectors.toList());
	}

}
