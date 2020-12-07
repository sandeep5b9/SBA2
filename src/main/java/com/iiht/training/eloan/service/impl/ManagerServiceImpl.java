package com.iiht.training.eloan.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iiht.training.eloan.dto.LoanDto;
import com.iiht.training.eloan.dto.LoanOutputDto;
import com.iiht.training.eloan.dto.ProcessingDto;
import com.iiht.training.eloan.dto.RejectDto;
import com.iiht.training.eloan.dto.SanctionDto;
import com.iiht.training.eloan.dto.SanctionOutputDto;
import com.iiht.training.eloan.dto.UserDto;
import com.iiht.training.eloan.entity.Loan;
import com.iiht.training.eloan.entity.ProcessingInfo;
import com.iiht.training.eloan.entity.SanctionInfo;
import com.iiht.training.eloan.entity.Users;
import com.iiht.training.eloan.exception.AlreadyFinalizedException;
import com.iiht.training.eloan.exception.LoanNotFoundException;
import com.iiht.training.eloan.exception.ManagerNotFoundException;
import com.iiht.training.eloan.repository.LoanRepository;
import com.iiht.training.eloan.repository.ProcessingInfoRepository;
import com.iiht.training.eloan.repository.SanctionInfoRepository;
import com.iiht.training.eloan.repository.UsersRepository;
import com.iiht.training.eloan.service.CustomerService;
import com.iiht.training.eloan.service.ManagerService;

@Service
public class ManagerServiceImpl implements ManagerService {

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

	private SanctionOutputDto convertToSanctionOutputDto(SanctionInfo sanctionInfo) {
		return new SanctionOutputDto(sanctionInfo.getLoanAmountSanctioned(), sanctionInfo.getTermOfLoan(),
				sanctionInfo.getPaymentStartDate(), sanctionInfo.getLoanClosureDate(),
				sanctionInfo.getMonthlyPayment());
	}

	private ProcessingDto convertToProcessingDto(ProcessingInfo processingInfo) {
		return new ProcessingDto(processingInfo.getAcresOfLand(), processingInfo.getLandValue(),
				processingInfo.getAppraisedBy(), processingInfo.getValuationDate(),
				processingInfo.getAddressOfProperty(), processingInfo.getSuggestedAmountOfLoan());
	}

	private SanctionInfo covertToSanctionInfoEntity(SanctionDto sanctionDto, Long managerId,
			Long loanAppId) {
		Double monthlyPayment;
		Double termPayment;
		Double interestRate = 75.0;

		termPayment = (sanctionDto.getLoanAmountSanctioned())
				* (Math.pow((1 + (interestRate / 100)), sanctionDto.getTermOfLoan()));
		monthlyPayment = (termPayment / sanctionDto.getTermOfLoan());

		LocalDate paymentStartDate = LocalDate.parse(sanctionDto.getPaymentStartDate());
		LocalDate loanClosureDate = paymentStartDate.plusMonths(sanctionDto.getTermOfLoan().intValue());

		return new SanctionInfo(loanAppId, managerId, sanctionDto.getLoanAmountSanctioned(),
				sanctionDto.getTermOfLoan(), sanctionDto.getPaymentStartDate(), loanClosureDate.toString(),
				monthlyPayment);
	}

	private LoanDto convertToLoanDto(Loan loan) {
		return new LoanDto(loan.getLoanName(), loan.getLoanAmount(), loan.getLoanApplicationDate(),
				loan.getBusinessStructure(), loan.getBillingIndicator(), loan.getTaxIndicator());
	}

	private SanctionOutputDto fetchSanctionOutputDto(Long loanAppId) {
		SanctionInfo sanctionInfo = this.sanctionInfoRepository.findByLoanAppId(loanAppId);
		return this.convertToSanctionOutputDto(sanctionInfo);
	}

	private LoanOutputDto covertToLoanOutputDto(LoanDto loanDto, Loan newLoan) {
		UserDto userDto = this.customerService.fetchSingleUser(newLoan.getCustomerId());

		LoanOutputDto loanOutputDto = new LoanOutputDto(newLoan.getCustomerId(), newLoan.getId(), userDto, loanDto,
				newLoan.getRemark());

		if (newLoan.getStatus() == 0) {
			loanOutputDto.setStatus("Applied");
		} else {
			loanOutputDto.setProcessingDto(this.fetchProcessingDTO(newLoan.getId()));
			if (newLoan.getStatus() == 1) {
				loanOutputDto.setStatus("Processed");
			} else if (newLoan.getStatus() == 2) {
				loanOutputDto.setStatus("Approved");
				loanOutputDto.setSanctionOutputDto(this.fetchSanctionOutputDto(newLoan.getId()));
			} else if (newLoan.getStatus() == -1) {
				loanOutputDto.setStatus("Rejected");
			}
		}
		return loanOutputDto;
	}

	private ProcessingDto fetchProcessingDTO(Long loanAppId) {
		ProcessingInfo processingInfo = this.processingInfoRepository.findByLoanAppId(loanAppId);
		return this.convertToProcessingDto(processingInfo);
	}

	@Override
	public List<LoanOutputDto> allProcessedLoans() {
		List<Loan> loans = this.loanRepository.findByStatus(1);// find by status
		List<LoanDto> loanDtos = loans.stream().map(this::convertToLoanDto).collect(Collectors.toList());
		List<LoanOutputDto> loanOutputDtos = new ArrayList<LoanOutputDto>();
		for (int i = 0; i < loanDtos.size(); i++) {
			LoanOutputDto loanOutputDto = this.covertToLoanOutputDto(loanDtos.get(i), loans.get(i));
			loanOutputDtos.add(loanOutputDto);
		}
		return loanOutputDtos;
	}

	@Override
	public RejectDto rejectLoan(Long managerId, Long loanAppId, RejectDto rejectDto) {

		if (this.usersRepository.findByRoleId("Manager", managerId) == null)
			throw new ManagerNotFoundException("Manager not found");

		Loan loan = this.loanRepository.findById(loanAppId).orElse(null);
		if (loan == null)
			throw new LoanNotFoundException("Loan not found");

		if (loan.getStatus() != 1)
			throw new AlreadyFinalizedException("Loan already finalized");

		loan.setStatus(-1);
		loan.setRemark(rejectDto.getRemark());
		this.loanRepository.save(loan);
		return rejectDto;
	}

	@Override
	public SanctionOutputDto sanctionLoan(Long managerId, Long loanAppId, SanctionDto sanctionDto) {
		if (this.usersRepository.findByRoleId("Manager", managerId) == null)
			throw new ManagerNotFoundException("Manager not found");

		Loan loan = this.loanRepository.findById(loanAppId).orElse(null);
		if (loan == null)
			throw new LoanNotFoundException("Loan not found");

		if (loan.getStatus() != 1)
			throw new AlreadyFinalizedException("Loan already finalized");
		loan.setStatus(2);
		this.loanRepository.save(loan);

		SanctionInfo sanctionInfo = this.covertToSanctionInfoEntity(sanctionDto, managerId, loanAppId);
		SanctionInfo newSanctionInfo = this.sanctionInfoRepository.save(sanctionInfo);
		return this.convertToSanctionOutputDto(newSanctionInfo);
	}

}
