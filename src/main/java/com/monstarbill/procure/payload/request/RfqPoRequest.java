package com.monstarbill.procure.payload.request;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RfqPoRequest implements Cloneable {
	
	private Long subsidiaryId;

	private Long prId;

	private Date prDate;

	private String prCurrency;

	private Long prLocationId;
	
	private String prLocation;

	// private String prType;

	// Required - For creation of RFQ
	private String bidType;

	// Required - For creation of PO
	private Long supplierId;
	
	private String supplierCurrency;

	// PO/RFQ data - Required
	private Date transactionalDate;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}
