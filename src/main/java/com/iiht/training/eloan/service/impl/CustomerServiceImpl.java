package com.iiht.training.eloan.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iiht.training.eloan.dto.LoanDto;
import com.iiht.training.eloan.dto.LoanOutputDto;
import com.iiht.training.eloan.dto.ProcessingDto;
import com.iiht.training.eloan.dto.SanctionOutputDto;
import com.iiht.training.eloan.dto.UserDto;
import com.iiht.training.eloan.entity.Loan;
import com.iiht.training.eloan.entity.ProcessingInfo;
import com.iiht.training.eloan.entity.SanctionInfo;
import com.iiht.training.eloan.entity.Users;
import com.iiht.training.eloan.repository.LoanRepository;
import com.iiht.training.eloan.repository.ProcessingInfoRepository;
import com.iiht.training.eloan.repository.SanctionInfoRepository;
import com.iiht.training.eloan.repository.UsersRepository;
import com.iiht.training.eloan.service.CustomerService;


import com.iiht.training.eloan.exception.LoanNotFoundException;
import com.iiht.training.eloan.exception.CustomerNotFoundException;

@Service
public class CustomerServiceImpl implements CustomerService {

	@Autowired
	private UsersRepository usersRepository;
	
	@Autowired
	private LoanRepository loanRepository;
	
	@Autowired
	private ProcessingInfoRepository processingInfoRepository;
	
	@Autowired
	private SanctionInfoRepository sanctionInfoRepository;
	
	private UserDto convertToUserOutputDto(Users users) {
		return new UserDto(users.getId(), users.getFirstName(), 
				users.getLastName(), users.getEmail(), users.getMobile());
	}
	
	private ProcessingDto fetchProcessingDTO(Long loanAppId) {
		ProcessingInfo processingInfo = this.processingInfoRepository.findByLoanAppId(loanAppId);
		return new ProcessingDto(processingInfo.getAcresOfLand(), processingInfo.getLandValue()
											, processingInfo.getAppraisedBy(), processingInfo.getValuationDate(),
											processingInfo.getAddressOfProperty(), processingInfo.getSuggestedAmountOfLoan());
	}
	
	private Users covertToUserEntity(UserDto userDto, String role) {
		return new Users(userDto.getFirstName(), userDto.getLastName(),
				userDto.getEmail(),userDto.getMobile(), role);
	}
	
	private Loan covertLoanDtoToLoanEntity(LoanDto loanDto, Long customerId,Integer status ) {
		return new Loan(customerId, loanDto.getLoanName(), loanDto.getLoanAmount(),
				loanDto.getLoanApplicationDate(),
				loanDto.getBusinessStructure(), loanDto.getBillingIndicator(), loanDto.getTaxIndicator(),status, "None");
	}
	
	
	private LoanOutputDto covertToLoanOutputDto(LoanDto loanDto, Loan newLoan) {
		UserDto userDto = this.fetchSingleUser(newLoan.getCustomerId());
		
		LoanOutputDto loanOutputDto = new LoanOutputDto(newLoan.getCustomerId(), newLoan.getId(), 
				userDto, loanDto, newLoan.getRemark());
		
		if(newLoan.getStatus()==0) {
			loanOutputDto.setStatus("Applied");
		}
		else {
			loanOutputDto.setProcessingDto(this.fetchProcessingDTO(newLoan.getId()));
			if(newLoan.getStatus()==1) {
				loanOutputDto.setStatus("Processed");
			}
			else if(newLoan.getStatus()==2) {
				loanOutputDto.setStatus("Approved");
				loanOutputDto.setSanctionOutputDto(this.fetchSanctionOutputDto(newLoan.getId()));
			}
			else if(newLoan.getStatus()==-1) {
				loanOutputDto.setStatus("Rejected");
			}
		}
		return loanOutputDto;
	}
	
	private SanctionOutputDto fetchSanctionOutputDto(Long loanAppId) {
		SanctionInfo sanctionInfo = this.sanctionInfoRepository.findByLoanAppId(loanAppId);
		return new SanctionOutputDto(sanctionInfo.getLoanAmountSanctioned(),
												sanctionInfo.getTermOfLoan(), sanctionInfo.getPaymentStartDate(),
												sanctionInfo.getLoanClosureDate(), sanctionInfo.getMonthlyPayment());
	}
	
	private LoanDto convertToLoanDto(Loan loan) {
		return new LoanDto(loan.getLoanName(), loan.getLoanAmount(), loan.getLoanApplicationDate(), 
				loan.getBusinessStructure(),loan.getBillingIndicator(), loan.getTaxIndicator());
	}
	
	@Override
	public UserDto register(UserDto userDto) {
		Users users = this.covertToUserEntity(userDto, "Client");
		Users newUsers = this.usersRepository.save(users);
		return this.convertToUserOutputDto(newUsers);
	}

	@Override
	public UserDto fetchSingleUser(Long customerId) {
		Users user = this.usersRepository.findById(customerId).orElse(null);
		return this.convertToUserOutputDto(user);
	}

	@Override
	public LoanOutputDto applyLoan(Long customerId, LoanDto loanDto) {
		
		if(this.usersRepository.findByRoleId("Client", customerId) == null)
			throw new CustomerNotFoundException("Customer not found");
		Loan loan = this.covertLoanDtoToLoanEntity(loanDto, customerId, 0);
		Loan newLoan = this.loanRepository.save(loan);
		return this.covertToLoanOutputDto(loanDto, newLoan);
	}

	@Override
	public LoanOutputDto getStatus(Long loanAppId) {
		Loan loan = this.loanRepository.findById(loanAppId).orElse(null);
		
		if(loan == null)
			throw new LoanNotFoundException("Loan not found");
		
		LoanDto loanDto = this.convertToLoanDto(loan);
		return this.covertToLoanOutputDto(loanDto, loan);
	}

	@Override
	public List<LoanOutputDto> getStatusAll(Long customerId) {
		if(this.usersRepository.findByRoleId("Client", customerId) == null)
			throw new CustomerNotFoundException("Customer not found");
		
		List<Loan> loans = this.loanRepository.findByCustomerId(customerId);
		List<LoanDto> loanDtos = 
					loans.stream()
							 .map(this :: convertToLoanDto)
							 .collect(Collectors.toList());
		List<LoanOutputDto> loanOutputDtos = new ArrayList<LoanOutputDto>();
		for(int i=0;i<loanDtos.size();i++) {
			LoanOutputDto loanOutputDto = this.covertToLoanOutputDto(loanDtos.get(i), loans.get(i));
			loanOutputDtos.add(loanOutputDto);
		}
		return loanOutputDtos;
	}

}
