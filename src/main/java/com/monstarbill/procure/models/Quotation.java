package com.monstarbill.procure.models;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import com.monstarbill.procure.commons.AppConstants;
import com.monstarbill.procure.commons.CommonUtils;
import com.monstarbill.procure.enums.Operation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "procure", name = "quotation")
@ToString
@Audited
@AuditTable("quotation_aud")
public class Quotation implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "rfq_number" , unique =true)
	private String rfqNumber; // Auto-generated from sequencing
	
	@NotNull(message = "Subsidiary is mandatory")
	@Column(name = "subsidiary_id")
	private Long subsidiaryId; // input input from dropdown
	
	@Column(name = "rfq_date")
	private Date rfqDate;	// input
	
	private String creator;
	
//	@NotBlank(message = "PR Number is mandatory")
//	@Column(name = "pr_number")
//	private String prNumber;	// input input from dropdown
	
	@Column(name = "memo_notes")
	private String memoNotes;	// input
	
	@NotBlank(message = "Bid type is mandatory")
	@Column(name = "bid_type")
	private String bidType;	// input from Dropdown
	
	@Column(name = "bid_open_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime bidOpenDate;
	
	@Column(name = "bid_close_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime bidCloseDate;	// input
	
	// private String currency;		// Auto-generated from PR
	private String exchangeRate;	// Auto-generated from PR
	// private String department;		// Auto-generated from PR
	// private String subsidiaryAddress;	// Auto-generated from Subsidiary
	// private String bidRecieveEmail;	// Auto-generated from Subsidiary
	
	@NotNull(message = "Location is mandatory")
	@Column(name = "location_id")
	private Long locationId;		// input input from dropdown
	
	private String status;				// workflow status
	
	@Column(name = "net_suite_id")
	private String netSuiteId;			// input	
	
	@Column(name="is_notification_sent", columnDefinition = "boolean default false")
	private boolean isNoticationSent;
	
	@Column(name="is_deleted", columnDefinition = "boolean default false")
	private boolean isDeleted;
	
	@CreationTimestamp
	@Column(name="created_date", updatable = false)
	private Date createdDate;

	@Column(name="created_by", updatable = false)
	private String createdBy;

	@UpdateTimestamp
	@Column(name="last_modified_date")
	private Timestamp lastModifiedDate;

	@Column(name="last_modified_by")
	private String lastModifiedBy;
	
	private String currency;
	
	@Transient
	private List<QuotationPr> quotationPrs;
	
//	@Transient
//	private String exchangeRate;

	@Transient
	private boolean isSubmitted;

	@Transient
	private boolean createQa;
	
	@Transient
	private String department;
	
	@Transient
	private String subsidiaryAddress;
	
	@Transient
	private String bidRecieveEmail;
	
	@Transient
	private String subsidiaryName;
	
	@Transient
	private String errorMessage;
	
	@Transient
	private List<QuotationVendors> quotationVendors;
	
	@Transient
	private List<QuotationItem> quotationItems;
	
	@Transient
	private List<QuotationGenaralInfo> quotationInfos;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param quotation
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<QuotationHistory> compareFields(Quotation quotation)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<QuotationHistory> quotationHistories = new ArrayList<QuotationHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(quotation);

				if (oldValue == null) {
					if (newValue != null) {
						quotationHistories.add(this.prepareQuotationHistory(quotation, field));
					}
				} else if (!oldValue.equals(newValue)) {
					quotationHistories.add(this.prepareQuotationHistory(quotation, field));
				}
			}
		}
		return quotationHistories;
	}

	private QuotationHistory prepareQuotationHistory(Quotation quotation, Field field) throws IllegalAccessException {
		QuotationHistory quotationHistory = new QuotationHistory();
		quotationHistory.setRfqNumber(quotation.getRfqNumber());
		quotationHistory.setModuleName(AppConstants.QUOTATION);
		quotationHistory.setChangeType(AppConstants.UI);
		quotationHistory.setOperation(Operation.UPDATE.toString());
		quotationHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) {
			quotationHistory.setOldValue(field.get(this).toString());
		}
		if (field.get(quotation) != null) {
			quotationHistory.setNewValue(field.get(quotation).toString());
		}
		quotationHistory.setLastModifiedBy(quotation.getLastModifiedBy());
		return quotationHistory;
	}
	
	public Quotation(Long id, String rfqNumber, Date rfqDate, String bidType, OffsetDateTime bidOpenDate, OffsetDateTime bidCloseDate, String name) {
		this.id = id;
		this.rfqNumber =  rfqNumber;
		this.rfqDate = rfqDate;
		this.bidType = bidType;
		this.bidOpenDate = bidOpenDate;
		this.bidCloseDate = bidCloseDate;
		this.subsidiaryName = name;
	}
	
	public Quotation(Long id, String name, Date rfqDate, String rfqNumber, OffsetDateTime bidOpenDate, 
			OffsetDateTime bidCloseDate, String bidType, String currency, String status) {
		this.id = id;
		this.subsidiaryName = name;
		this.rfqDate = rfqDate;
		this.rfqNumber = rfqNumber;
		this.bidOpenDate = bidOpenDate;
		this.bidCloseDate = bidCloseDate;
		this.bidType = bidType;
		this.currency = currency;
		this.status = status;
	}

}
