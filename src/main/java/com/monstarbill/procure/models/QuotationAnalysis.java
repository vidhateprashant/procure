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

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import com.monstarbill.procure.commons.AppConstants;
import com.monstarbill.procure.commons.CommonUtils;
import com.monstarbill.procure.enums.Operation;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "procure", name = "quotation_analysis")
@ToString
@Audited
@AuditTable("quotation_analysis_aud")
@EqualsAndHashCode
public class QuotationAnalysis implements Cloneable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "qa_number", unique = true)
	private String qaNumber; // Auto-generated from sequencing
	
	@Column(name = "rfq_id", unique = true)
	private Long rfqId; // value from rfq page
	
	@Column(name = "subsidiary_id")
	private Long subsidiaryId; // value from rfq page
	
	@Column(name = "qa_date")
	private Date qaDate;	// input
	
	private String creator;
	
	@Transient
	private String bidType;

	@Transient
	private OffsetDateTime bidOpenDate;
	
	@Transient
	private OffsetDateTime bidCloseDate;

	@Transient
	private String currency;
	
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
	
	@Transient
	private List<QuotationAnalysisItem> quotationAnalysisItems;
	
	@Transient
	private String subsidiaryName;
	
	@Transient
	private String rfqNumber;;
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/**
	 * Compare the fields and values of 2 objects in order to find out the
	 * difference between old and new value
	 * 
	 * @param quotationAnalysis
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public List<QuotationAnalysisHistory> compareFields(QuotationAnalysis quotationAnalysis)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		List<QuotationAnalysisHistory> quotationHistories = new ArrayList<QuotationAnalysisHistory>();
		Field[] fields = this.getClass().getDeclaredFields();

		for (Field field : fields) {
			String fieldName = field.getName();

			if (!CommonUtils.getUnusedFieldsOfHistory().contains(fieldName.toLowerCase())) {
				Object oldValue = field.get(this);
				Object newValue = field.get(quotationAnalysis);

				if (oldValue == null) {
					if (newValue != null) {
						quotationHistories.add(this.prepareQuotationHistory(quotationAnalysis, field));
					}
				} else if (!oldValue.equals(newValue)) {
					quotationHistories.add(this.prepareQuotationHistory(quotationAnalysis, field));
				}
			}
		}
		return quotationHistories;
	}

	private QuotationAnalysisHistory prepareQuotationHistory(QuotationAnalysis quotationAnalysis, Field field) throws IllegalAccessException {
		QuotationAnalysisHistory quotationAnalysisHistory = new QuotationAnalysisHistory();
		quotationAnalysisHistory.setQaNumber(quotationAnalysis.getQaNumber());
		quotationAnalysisHistory.setModuleName(AppConstants.QUOTATION_ANALYSIS);
		quotationAnalysisHistory.setChangeType(AppConstants.UI);
		quotationAnalysisHistory.setOperation(Operation.UPDATE.toString());
		quotationAnalysisHistory.setFieldName(CommonUtils.splitCamelCaseWithCapitalize(field.getName()));
		if (field.get(this) != null) {
			quotationAnalysisHistory.setOldValue(field.get(this).toString());
		}
		if (field.get(quotationAnalysis) != null) {
			quotationAnalysisHistory.setNewValue(field.get(quotationAnalysis).toString());			
		}
		quotationAnalysisHistory.setLastModifiedBy(quotationAnalysis.getLastModifiedBy());
		return quotationAnalysisHistory;
	}

	public QuotationAnalysis(Long id, String qaNumber, Long rfqId, Long subsidiaryId, Date qaDate, String bidType,
			OffsetDateTime bidOpenDate, OffsetDateTime bidCloseDate, String currency, String subsidiaryName) {
		this.id = id;
		this.qaNumber = qaNumber;
		this.rfqId = rfqId;
		this.subsidiaryId = subsidiaryId;
		this.qaDate = qaDate;
		this.bidType = bidType;
		this.bidOpenDate = bidOpenDate;
		this.bidCloseDate = bidCloseDate;
		this.currency = currency;
		this.subsidiaryName = subsidiaryName;
	}
	
	public QuotationAnalysis(Long id, String qaNumber) {
		this.id = id;
		this.qaNumber = qaNumber;
	}
	
}
