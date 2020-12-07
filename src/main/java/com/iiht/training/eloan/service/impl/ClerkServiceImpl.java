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
import com.iiht.training.eloan.exception.AlreadyProcessedException;
import com.iiht.training.eloan.exception.ClerkNotFoundException;
import com.iiht.training.eloan.exception.CustomerNotFoundException;
import com.iiht.training.eloan.exception.LoanNotFoundException;
import com.iiht.training.eloan.repository.LoanRepository;
import com.iiht.training.eloan.repository.ProcessingInfoRepository;
import com.iiht.training.eloan.repository.SanctionInfoRepository;
import com.iiht.training.eloan.repository.UsersRepository;
import com.iiht.training.eloan.service.ClerkService;
import com.iiht.training.eloan.service.CustomerService;

@Service
public class ClerkServiceImpl implements ClerkService {

	@Autowired
	private UsersRepository usersRepository;
	
	@Autowired
	private LoanRepository loanRepository;
	
	@Autowired
	private ProcessingInfoRepository processingInfoRepository;
	
	@Autowired
	private SanctionInfoRepository sanctionInfoRepository;
	
	@Autowired
	private CustomerService customerService;
	
	private ProcessingDto fetchProcessingDTO(Long loanAppId) {
		ProcessingInfo processingInfo = this.processingInfoRepository.findByLoanAppId(loanAppId);
		return this.convertProcessingEntityToProcessingDto(processingInfo);
	}
	
	private SanctionOutputDto fetchSanctionOutputDto(Long loanAppId) {
		SanctionInfo sanctionInfo = this.sanctionInfoRepository.findByLoanAppId(loanAppId);
		return new SanctionOutputDto(sanctionInfo.getLoanAmountSanctioned(),
												sanctionInfo.getTermOfLoan(), sanctionInfo.getPaymentStartDate(),
												sanctionInfo.getLoanClosureDate(), sanctionInfo.getMonthlyPayment());
	}
	
	private ProcessingDto convertProcessingEntityToProcessingDto(ProcessingInfo processingInfo) {
		return new ProcessingDto(processingInfo.getAcresOfLand(), processingInfo.getLandValue()
				, processingInfo.getAppraisedBy(), processingInfo.getValuationDate(),
				processingInfo.getAddressOfProperty(), processingInfo.getSuggestedAmountOfLoan());
	}
	
	
	private LoanDto convertLoanEntityToLoanDto(Loan loan) {
		return new LoanDto(loan.getLoanName(), loan.getLoanAmount(), loan.getLoanApplicationDate(), 
				loan.getBusinessStructure(),loan.getBillingIndicator(), loan.getTaxIndicator());
	}
	
	
	private LoanOutputDto covertToLoanOutputDto(LoanDto loanDto, Loan newLoan) {
		UserDto userDto = this.customerService.fetchSingleUser(newLoan.getCustomerId());
		
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

	private ProcessingInfo covertProcessingDtoToProcessingEntity(Long clerkId, Long loanAppId, ProcessingDto processingDto) {
		return new ProcessingInfo (loanAppId, clerkId,
				processingDto.getAcresOfLand(), processingDto.getLandValue(),
				processingDto.getAppraisedBy(), processingDto.getValuationDate(), 
				processingDto.getAddressOfProperty(), processingDto.getSuggestedAmountOfLoan());
	}
		
	@Override
	public List<LoanOutputDto> allAppliedLoans() {
		List<Loan> loans = this.loanRepository.findByStatus(0);
		List<LoanDto> loanDtos = 
					loans.stream()
							 .map(this :: convertLoanEntityToLoanDto)
							 .collect(Collectors.toList());
		List<LoanOutputDto> loanOutputDtos = new ArrayList<LoanOutputDto>();
		for(int i=0;i<loanDtos.size();i++) {
			LoanOutputDto loanOutputDto = this.covertToLoanOutputDto(loanDtos.get(i), loans.get(i));
			loanOutputDtos.add(loanOutputDto);
		}
		return loanOutputDtos;
	}

	@Override
	public ProcessingDto processLoan(Long clerkId, Long loanAppId, ProcessingDto processingDto) {
		if(this.usersRepository.findByRoleId("Clerk", clerkId) == null)
			throw new ClerkNotFoundException("Clerk not found");
		
		Loan loan = this.loanRepository.findById(loanAppId).orElse(null);
		if(loan == null)
			throw new LoanNotFoundException("Loan not found");
		
		if(loan.getStatus() != 0)
			throw new AlreadyProcessedException("Loan is already processed");
		
		loan.setStatus(1);
		this.loanRepository.save(loan);
		
		ProcessingInfo processingInfo = this.covertProcessingDtoToProcessingEntity(clerkId, loanAppId, processingDto);
		
		ProcessingInfo newProcessingInfo = this.processingInfoRepository.save(processingInfo);
		return this.convertProcessingEntityToProcessingDto(newProcessingInfo);
	}

}
