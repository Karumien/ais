/*
 * Copyright (c) 2019-2029 Karumien s.r.o.
 *
 * Karumien s.r.o. is not responsible for defects arising from 
 * unauthorized changes to the source code.
 */
package com.karumien.cloud.ais.service;

import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.data.domain.Page;

import com.karumien.cloud.ais.api.entity.ViewPass;
import com.karumien.cloud.ais.api.model.PassDTO;
import com.karumien.cloud.ais.api.model.UserInfoDTO;
import com.karumien.cloud.ais.api.model.WorkMonthDTO;

/**
 * Front AIS Service.
 *
 * @author <a href="miroslav.svoboda@karumien.com">Miroslav Svoboda</a>
 * @since 1.0, 15. 4. 2019 17:09:02
 */
public interface AISService {
	
	/** Hours in work day 7.5 vs 8.0 */
	double HOURS_IN_DAY = 8d;

	/**
	 * Returns passes filtered by user (optional).
	 * 
	 * @param usercode filtered by usercode (optional)
	 * @return {@link Page} of {@link PassDTO} filtered by optional user
	 */
	Page<ViewPass> getPass(Integer usercode);

	/**
	 * Returns passes filtered by user (optional).
	 * 
	 * @param username filtered by username (optional)
	 * @return {@link Page} of {@link PassDTO} filtered by optional user
	 */
	Page<ViewPass> getPass(String username);

	/**
	 * Returns all users onsite.
	 * 
	 * @return {@link List} of {@link PassDTO} which is onsite
	 */
	List<ViewPass> getPassOnsite();

	/**
	 * Return work month for specified user
	 * 
	 * @param year     year of work month
	 * @param month    year of work month
	 * @param username username records
	 * @return {@link WorkMonthDTO} work month of specified user
	 */
	WorkMonthDTO getWorkDays(Integer year, Integer month, @NotNull @Valid String username);

	/**
	 * Return known users list,
	 * 
	 * @param username
	 * @return {@link List} of {@link UserInfoDTO} known users
	 */
	List<UserInfoDTO> getWorkUsers(@Valid String username);

	
	@Deprecated
	Long setWork(@NotNull @Valid LocalDate date, @NotNull @Valid String username,
			@Valid String hours, @Valid Long id, @Valid String workType);

}
