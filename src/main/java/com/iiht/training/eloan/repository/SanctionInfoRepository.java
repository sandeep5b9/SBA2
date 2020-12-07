package com.iiht.training.eloan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iiht.training.eloan.entity.Loan;
import com.iiht.training.eloan.entity.SanctionInfo;

@Repository
public interface SanctionInfoRepository extends JpaRepository<SanctionInfo, Long>{
	SanctionInfo findByLoanAppId(Long loanAppId);
}
